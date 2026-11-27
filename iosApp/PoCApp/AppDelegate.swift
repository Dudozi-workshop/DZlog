import UIKit
import AVFoundation
import Photos
import PhotosUI
import UniformTypeIdentifiers
import DZlogUi

// Swift owns Apple's camera permission API. Kotlin receives only the verified
// authorization result, so its PhotoKit/camera code cannot bypass a denial.
@main
final class AppDelegate: UIResponder, UIApplicationDelegate, UIDocumentPickerDelegate, UIAdaptivePresentationControllerDelegate {
    var window: UIWindow?
    private var cameraPresentedOrPending = false
    private var permissionActionPending = false
    private var awaitingWorkspaceImport = false
    private var phraseCsvRequest: (picker: UIDocumentPickerViewController, reply: Notification.Name, directory: URL?)?
    private var exportedTemporaryURL: URL?
    private var testWorkspaceArchive: String?
    private var activePhotoShareDirectory: URL?

    private static let openCamera = Notification.Name("dzlog.openCamera")
    private static let closeCamera = Notification.Name("dzlog.dismissCamera")
    private static let sharePhoto = Notification.Name("dzlog.sharePhoto")
    private static let sharePhotoFinished = Notification.Name("dzlog.sharePhotoFinished")
    private static let openSystemSettings = Notification.Name("dzlog.openSystemSettings")
    private static let permissionAction = Notification.Name("dzlog.permissionAction")
    private static let permissionStatusRequest = Notification.Name("dzlog.permissionStatusRequest")
    private static let permissionStatusUpdated = Notification.Name("dzlog.permissionStatusUpdated")
    private static let exportWorkspace = Notification.Name("dzlog.exportWorkspace")
    private static let importWorkspace = Notification.Name("dzlog.importWorkspace")
    private static let exportCompleted = Notification.Name("dzlog.workspaceExportCompleted")
    private static let exportFailed = Notification.Name("dzlog.workspaceExportFailed")
    private static let importSelected = Notification.Name("dzlog.workspaceImportSelected")
    private static let importFailed = Notification.Name("dzlog.workspaceImportFailed")

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        let window = UIWindow(frame: UIScreen.main.bounds)
        self.window = window
        NotificationCenter.default.addObserver(
            self, selector: #selector(openCameraFromHome(_:)),
            name: Self.openCamera, object: nil
        )
        NotificationCenter.default.addObserver(
            self, selector: #selector(dismissCameraFromHome(_:)),
            name: Self.closeCamera, object: nil
        )
        NotificationCenter.default.addObserver(
            self, selector: #selector(sharePhotoFromGallery(_:)),
            name: Self.sharePhoto, object: nil
        )
        NotificationCenter.default.addObserver(
            self, selector: #selector(openSystemSettingsFromSettings(_:)),
            name: Self.openSystemSettings, object: nil
        )
        NotificationCenter.default.addObserver(
            self, selector: #selector(handlePermissionAction(_:)), name: Self.permissionAction, object: nil
        )
        NotificationCenter.default.addObserver(
            self, selector: #selector(reportPermissionStatus(_:)),
            name: Self.permissionStatusRequest, object: nil
        )
        NotificationCenter.default.addObserver(
            self, selector: #selector(exportWorkspaceFromSettings(_:)),
            name: Self.exportWorkspace, object: nil
        )
        NotificationCenter.default.addObserver(
            self, selector: #selector(importWorkspaceFromSettings(_:)),
            name: Self.importWorkspace, object: nil
        )
        NotificationCenter.default.addObserver(
            self, selector: #selector(selectPhraseCsv(_:)), name: Notification.Name("dzlog.phraseCsv"), object: nil
        )
        if CommandLine.arguments.contains("--camera-flow") {
            // Keep the camera disconnected while the asynchronous prompt is unresolved.
            window.rootViewController = UIViewController()
            window.makeKeyAndVisible()
            resolveCameraPermission { [weak self] granted in
                guard let self = self, let activeWindow = self.window else { return }
                activeWindow.rootViewController =
                    MainViewControllerKt.MainViewControllerWithCameraPermission(cameraPermissionGranted: granted)
            }
        } else {
            window.rootViewController = MainViewControllerKt.MainViewController()
            window.makeKeyAndVisible()
        }
        return true
    }

    // Home keeps its existing Compose state while the native camera is presented.
    // The same permission gate is used by --camera-flow and by normal home entry.
    @objc private func openCameraFromHome(_ notification: Notification) {
        guard !cameraPresentedOrPending, let root = window?.rootViewController,
              root.presentedViewController == nil else { return }
        cameraPresentedOrPending = true
        resolveCameraPermission { [weak self, weak root] granted in
            guard let self = self, let root = root,
                  self.window?.rootViewController === root,
                  root.presentedViewController == nil else {
                self?.cameraPresentedOrPending = false
                return
            }
            let screen = MainViewControllerKt.CameraViewController(cameraPermissionGranted: granted)
            screen.modalPresentationStyle = .fullScreen
            root.present(screen, animated: true)
        }
    }

    @objc private func dismissCameraFromHome(_ notification: Notification) {
        guard let root = window?.rootViewController else { return }
        if root.presentedViewController != nil {
            root.dismiss(animated: true) { [weak self] in
                self?.cameraPresentedOrPending = false
            }
        } else {
            cameraPresentedOrPending = false
        }
    }

    @objc private func sharePhotoFromGallery(_ notification: Notification) {
        // Kotlin posts on the UI thread, but keep UIKit and the in-flight guard
        // serialized even if a later caller posts from a background queue.
        guard Thread.isMainThread else {
            DispatchQueue.main.async { [weak self] in self?.sharePhotoFromGallery(notification) }
            return
        }
        guard activePhotoShareDirectory == nil else {
            reportPhotoShare("이미 사진 공유가 진행 중입니다.")
            return
        }
        let payloads: [[String: Any]]
        if let batch = notification.object as? [[String: Any]] {
            payloads = batch
        } else if let single = notification.object as? [String: Any] {
            payloads = [single] // Backwards-compatible single-photo callers.
        } else {
            reportPhotoShare("공유할 사진 데이터를 읽을 수 없습니다.")
            return
        }
        guard !payloads.isEmpty, payloads.count <= 50 else {
            reportPhotoShare("한 번에 1~50장의 사진을 공유할 수 있습니다.")
            return
        }
        // Validate the entire request before creating files or presenting a sheet.
        let byteLimit = 64 * 1024 * 1024
        var totalBytes = 0
        var filesToWrite: [(data: Data, name: String)] = []
        for payload in payloads {
            guard let data = payload["data"] as? Data, !data.isEmpty else {
                reportPhotoShare("선택한 사진 중 읽을 수 없는 데이터가 있습니다.")
                return
            }
            guard data.count <= byteLimit - totalBytes else {
                reportPhotoShare("공유할 사진의 합계가 64MiB를 넘습니다. 사진을 나눠 선택해 주세요.")
                return
            }
            totalBytes += data.count
            let requestedName = payload["filename"] as? String ?? ""
            let leaf = (requestedName as NSString).lastPathComponent
            let safeName = !leaf.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
                leaf == requestedName && leaf != "." && leaf != ".." &&
                !leaf.contains("/") && !leaf.contains("\\") &&
                leaf.rangeOfCharacter(from: .controlCharacters) == nil && leaf.utf8.count <= 255
                ? leaf : "photo.jpg"
            filesToWrite.append((data, safeName))
        }
        guard var presenter = window?.rootViewController else {
            reportPhotoShare("공유 화면을 열 수 없습니다.")
            return
        }
        while let presented = presenter.presentedViewController { presenter = presented }
        guard !(presenter is UIActivityViewController),
              !presenter.isBeingPresented, !presenter.isBeingDismissed,
              presenter.viewIfLoaded?.window != nil else {
            reportPhotoShare("현재 화면에서는 사진 공유를 열 수 없습니다. 잠시 후 다시 시도해 주세요.")
            return
        }

        // Share the original resource bytes and filename, as Android shares its
        // selected URI. A UIImage activity item would re-encode the resource.
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("dzlog-photo-share-\(UUID().uuidString)", isDirectory: true)
        activePhotoShareDirectory = directory
        do {
            try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: false)
            var files: [URL] = []
            for (index, item) in filesToWrite.enumerated() {
                // Original/result companions can share a filename. Separate child
                // directories preserve both names and bytes without overwriting.
                let itemDirectory = directory.appendingPathComponent(String(index), isDirectory: true)
                try FileManager.default.createDirectory(at: itemDirectory, withIntermediateDirectories: false)
                let file = itemDirectory.appendingPathComponent(item.name, isDirectory: false)
                try item.data.write(to: file, options: .atomic)
                files.append(file)
            }
            let activity = UIActivityViewController(activityItems: files, applicationActivities: nil)
            activity.completionWithItemsHandler = { [weak self] _, completed, _, error in
                DispatchQueue.main.async {
                    let result = error.map { "사진 공유 실패: \($0.localizedDescription)" }
                        ?? (completed ? "completed" : "cancelled")
                    self?.finishPhotoShare(directory: directory, result: result)
                }
            }
            if let popover = activity.popoverPresentationController {
                popover.sourceView = presenter.view
                popover.sourceRect = CGRect(x: presenter.view.bounds.midX,
                                            y: presenter.view.bounds.midY, width: 1, height: 1)
            }
            presenter.present(activity, animated: true)
            // UIKit can decline presentation without throwing. Do not leave the
            // Kotlin gallery busy forever or leave this attempt's file behind.
            DispatchQueue.main.async { [weak self, weak activity] in
                guard activity?.presentingViewController == nil else { return }
                self?.finishPhotoShare(directory: directory, result: "사진 공유 화면을 열지 못했습니다.")
            }
        } catch {
            finishPhotoShare(directory: directory, result: "공유 파일 준비 실패: \(error.localizedDescription)")
        }
    }

    private func reportPhotoShare(_ result: String) {
        NotificationCenter.default.post(name: Self.sharePhotoFinished, object: result)
    }

    private func finishPhotoShare(directory: URL, result: String) {
        // A late callback cannot finish or remove a newer operation's files.
        guard activePhotoShareDirectory == directory else { return }
        activePhotoShareDirectory = nil
        try? FileManager.default.removeItem(at: directory)
        reportPhotoShare(result)
    }

    @objc private func openSystemSettingsFromSettings(_ notification: Notification) {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url, options: [:], completionHandler: nil)
    }

    @objc private func reportPermissionStatus(_ notification: Notification) {
        publishPermissions()
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        publishPermissions()
    }

    private var cameraAuthorization: AVAuthorizationStatus {
        if CommandLine.arguments.contains("--deny-camera-permission-for-ui-test") { return .denied }
        return AVCaptureDevice.authorizationStatus(for: .video)
    }

    private func publishPermissions() {
        let status: String
        switch cameraAuthorization {
        case .authorized: status = "allowed"
        case .denied: status = "denied"
        case .restricted: status = "restricted"
        case .notDetermined: status = "undetermined"
        @unknown default: status = "unknown"
        }
        NotificationCenter.default.post(name: Self.permissionStatusUpdated, object: status)
    }

    @objc private func handlePermissionAction(_ notification: Notification) {
        guard !permissionActionPending, let action = notification.object as? String else { return }
        let finish = { [weak self] in
            DispatchQueue.main.async {
                self?.permissionActionPending = false
                self?.publishPermissions()
            }
        }
        switch action {
        case "camera" where cameraAuthorization == .notDetermined:
            permissionActionPending = true
            resolveCameraPermission { _ in finish() }
        case "photos" where PHPhotoLibrary.authorizationStatus(for: .readWrite) == .notDetermined:
            permissionActionPending = true
            PHPhotoLibrary.requestAuthorization(for: .readWrite) { _ in finish() }
        case "selection" where PHPhotoLibrary.authorizationStatus(for: .readWrite) == .limited:
            guard let presenter = availablePresenter(), presenter.viewIfLoaded?.window != nil else { return }
            permissionActionPending = true
            PHPhotoLibrary.shared().presentLimitedLibraryPicker(from: presenter) { _ in finish() }
        default:
            publishPermissions()
        }
    }

    @objc private func exportWorkspaceFromSettings(_ notification: Notification) {
        guard let archive = notification.object as? String else {
            postWorkspaceFailure(Self.exportFailed, "백업 데이터를 만들지 못했습니다.")
            return
        }
        if CommandLine.arguments.contains("--workspace-backup-ui-test") {
            testWorkspaceArchive = archive
            NotificationCenter.default.post(name: Self.exportCompleted, object: nil)
            return
        }
        guard let presenter = availablePresenter() else {
            postWorkspaceFailure(Self.exportFailed, "파일 선택기를 열 수 없습니다.")
            return
        }
        do {
            let formatter = DateFormatter()
            formatter.locale = Locale(identifier: "ko_KR")
            formatter.dateFormat = "yyyyMMdd"
            let url = FileManager.default.temporaryDirectory
                .appendingPathComponent("DZlog_backup_\(formatter.string(from: Date())).dzlog")
            try Data(archive.utf8).write(to: url, options: .atomic)
            exportedTemporaryURL = url
            awaitingWorkspaceImport = false
            let picker = UIDocumentPickerViewController(forExporting: [url], asCopy: true)
            picker.delegate = self
            presenter.present(picker, animated: true)
        } catch {
            cleanupExportedWorkspaceFile()
            postWorkspaceFailure(Self.exportFailed, "백업 파일을 준비하지 못했습니다.")
        }
    }

    @objc private func importWorkspaceFromSettings(_ notification: Notification) {
        if CommandLine.arguments.contains("--workspace-backup-ui-test") {
            guard let archive = testWorkspaceArchive else {
                postWorkspaceFailure(Self.importFailed, "테스트 백업이 준비되지 않았습니다.")
                return
            }
            NotificationCenter.default.post(name: Self.importSelected, object: archive)
            return
        }
        guard let presenter = availablePresenter() else {
            postWorkspaceFailure(Self.importFailed, "파일 선택기를 열 수 없습니다.")
            return
        }
        let dzlogType = UTType(filenameExtension: "dzlog") ?? .data
        awaitingWorkspaceImport = true
        let picker = UIDocumentPickerViewController(
            forOpeningContentTypes: [dzlogType, .json, .data], asCopy: true
        )
        picker.delegate = self
        presenter.present(picker, animated: true)
    }

    @objc private func selectPhraseCsv(_ notification: Notification) {
        guard let input = notification.object as? [String: String], let name = input["reply"],
              name.hasPrefix("dzlog.phraseCsvReply.") else { return }
        let reply = Notification.Name(name)
        // Compose can present the editor in a separate UIKit window.
        let root = window?.windowScene?.windows.first(where: { $0.isKeyWindow })?.rootViewController
            ?? window?.rootViewController
        guard var presenter = root, phraseCsvRequest == nil, !awaitingWorkspaceImport,
              exportedTemporaryURL == nil else {
            NotificationCenter.default.post(name: reply, object: ["message": "파일 선택기를 열 수 없습니다."])
            return
        }
        while let next = presenter.presentedViewController { presenter = next }
        guard presenter.viewIfLoaded?.window != nil, !presenter.isBeingDismissed,
              !(presenter is UIDocumentPickerViewController) else {
            NotificationCenter.default.post(name: reply, object: ["message": "파일 선택기를 열 수 없습니다."])
            return
        }
        var directory: URL?
        do {
            let picker: UIDocumentPickerViewController
            if let csv = input["csv"], let filename = input["name"] {
                let folder = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)
                try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: false)
                directory = folder
                let file = folder.appendingPathComponent((filename as NSString).lastPathComponent)
                try Data(csv.utf8).write(to: file, options: .atomic)
                picker = UIDocumentPickerViewController(forExporting: [file], asCopy: true)
            } else {
                picker = UIDocumentPickerViewController(forOpeningContentTypes: [.commaSeparatedText, .plainText, .data], asCopy: true)
                picker.allowsMultipleSelection = false
            }
            picker.delegate = self
            picker.modalPresentationStyle = .pageSheet
            picker.sheetPresentationController?.prefersGrabberVisible = true
            picker.presentationController?.delegate = self
            phraseCsvRequest = (picker, reply, directory)
            presenter.present(picker, animated: true)
        } catch {
            if let folder = directory { try? FileManager.default.removeItem(at: folder) }
            NotificationCenter.default.post(name: reply, object: ["message": "CSV 파일을 준비하지 못했습니다."])
        }
    }

    func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
        guard let request = phraseCsvRequest,
              presentationController.presentedViewController === request.picker else { return }
        // An interactive sheet dismissal can arrive separately from the picker delegate.
        request.picker.delegate = nil
        _ = finishPhraseCsv(request.picker, urls: nil)
    }

    private func finishPhraseCsv(_ picker: UIDocumentPickerViewController, urls: [URL]?) -> Bool {
        guard let request = phraseCsvRequest, request.picker === picker else { return false }
        phraseCsvRequest = nil
        if let folder = request.directory { try? FileManager.default.removeItem(at: folder) }
        guard let urls = urls else {
            NotificationCenter.default.post(name: request.reply, object: [String: String]())
            return true
        }
        guard let file = urls.first else {
            NotificationCenter.default.post(name: request.reply, object: ["message": "선택한 파일을 찾을 수 없습니다."])
            return true
        }
        if request.directory != nil {
            NotificationCenter.default.post(name: request.reply, object: ["message": "CSV 내보내기가 완료되었습니다."])
            return true
        }
        let reply = request.reply
        DispatchQueue.global(qos: .userInitiated).async {
            let result = Self.readPhraseCsv(file)
            DispatchQueue.main.async { NotificationCenter.default.post(name: reply, object: result) }
        }
        return true
    }

    private static func readPhraseCsv(_ file: URL) -> [String: String] {
        let scoped = file.startAccessingSecurityScopedResource()
        defer { if scoped { file.stopAccessingSecurityScopedResource() } }
        do {
            let handle = try FileHandle(forReadingFrom: file)
            defer { try? handle.close() }
            var bytes = Data()
            let limit = 1_000_000
            while bytes.count <= limit {
                guard let chunk = try handle.read(upToCount: min(65536, limit + 1 - bytes.count)),
                      !chunk.isEmpty else { break }
                bytes.append(chunk)
            }
            guard bytes.count <= limit else { return ["message": "파일 크기가 1 MB를 초과합니다."] }
            guard let csv = String(data: bytes, encoding: .utf8) else {
                return ["message": "UTF-8 CSV 파일을 선택해 주세요."]
            }
            return ["csv": csv]
        } catch {
            return ["message": "CSV 파일을 읽을 수 없습니다."]
        }
    }

    func documentPicker(_ controller: UIDocumentPickerViewController,
                        didPickDocumentsAt urls: [URL]) {
        if finishPhraseCsv(controller, urls: urls) { return }
        if !awaitingWorkspaceImport {
            cleanupExportedWorkspaceFile()
            NotificationCenter.default.post(name: Self.exportCompleted, object: nil)
            return
        }
        awaitingWorkspaceImport = false
        guard let url = urls.first else {
            postWorkspaceFailure(Self.importFailed, "선택한 파일을 찾을 수 없습니다.")
            return
        }
        let scoped = url.startAccessingSecurityScopedResource()
        defer { if scoped { url.stopAccessingSecurityScopedResource() } }
        do {
            let size = try url.resourceValues(forKeys: [.fileSizeKey]).fileSize ?? 0
            guard size <= 2 * 1024 * 1024 else {
                postWorkspaceFailure(Self.importFailed, "백업 파일이 허용 크기를 초과했습니다.")
                return
            }
            let data = try Data(contentsOf: url, options: [.mappedIfSafe])
            guard data.count <= 2 * 1024 * 1024, let archive = String(data: data, encoding: .utf8) else {
                postWorkspaceFailure(Self.importFailed, "백업 파일을 읽을 수 없습니다.")
                return
            }
            NotificationCenter.default.post(name: Self.importSelected, object: archive)
        } catch {
            postWorkspaceFailure(Self.importFailed, "백업 파일을 열지 못했습니다.")
        }
    }

    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        if finishPhraseCsv(controller, urls: nil) { return }
        let wasImport = awaitingWorkspaceImport
        awaitingWorkspaceImport = false
        cleanupExportedWorkspaceFile()
        NotificationCenter.default.post(
            name: wasImport ? Self.importFailed : Self.exportFailed,
            object: "파일 선택을 취소했습니다."
        )
    }

    private func availablePresenter() -> UIViewController? {
        guard var presenter = window?.rootViewController else { return nil }
        while let presented = presenter.presentedViewController { presenter = presented }
        guard !(presenter is UIDocumentPickerViewController) else { return nil }
        return presenter
    }

    private func postWorkspaceFailure(_ name: Notification.Name, _ message: String) {
        NotificationCenter.default.post(name: name, object: message)
    }

    private func cleanupExportedWorkspaceFile() {
        if let url = exportedTemporaryURL { try? FileManager.default.removeItem(at: url) }
        exportedTemporaryURL = nil
    }

    private func resolveCameraPermission(completion: @escaping (Bool) -> Void) {
        switch cameraAuthorization {
        case .authorized:
            completion(true)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async { completion(granted) }
            }
        case .restricted, .denied:
            completion(false)
        @unknown default:
            completion(false)
        }
    }
}
