import UIKit
import AVFoundation
import UniformTypeIdentifiers
import DZlogUi

// Swift owns Apple's camera permission API. Kotlin receives only the verified
// authorization result, so its PhotoKit/camera code cannot bypass a denial.
@main
final class AppDelegate: UIResponder, UIApplicationDelegate, UIDocumentPickerDelegate {
    var window: UIWindow?
    private var cameraPresentedOrPending = false
    private var awaitingWorkspaceImport = false
    private var exportedTemporaryURL: URL?
    private var testWorkspaceArchive: String?

    private static let openCamera = Notification.Name("dzlog.openCamera")
    private static let closeCamera = Notification.Name("dzlog.dismissCamera")
    private static let sharePhoto = Notification.Name("dzlog.sharePhoto")
    private static let openSystemSettings = Notification.Name("dzlog.openSystemSettings")
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
            self, selector: #selector(exportWorkspaceFromSettings(_:)),
            name: Self.exportWorkspace, object: nil
        )
        NotificationCenter.default.addObserver(
            self, selector: #selector(importWorkspaceFromSettings(_:)),
            name: Self.importWorkspace, object: nil
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
        guard let data = notification.object as? NSData,
              let image = UIImage(data: data as Data),
              var presenter = window?.rootViewController else { return }
        while let presented = presenter.presentedViewController { presenter = presented }
        guard !(presenter is UIActivityViewController) else { return }
        let activity = UIActivityViewController(activityItems: [image], applicationActivities: nil)
        if let popover = activity.popoverPresentationController {
            popover.sourceView = presenter.view
            popover.sourceRect = CGRect(x: presenter.view.bounds.midX,
                                        y: presenter.view.bounds.midY, width: 1, height: 1)
        }
        presenter.present(activity, animated: true)
    }

    @objc private func openSystemSettingsFromSettings(_ notification: Notification) {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url, options: [:], completionHandler: nil)
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

    func documentPicker(_ controller: UIDocumentPickerViewController,
                        didPickDocumentsAt urls: [URL]) {
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
        // Deterministic UI fixture only: may force denial, never grant permission.
        if CommandLine.arguments.contains("--deny-camera-permission-for-ui-test") {
            completion(false)
            return
        }
        switch AVCaptureDevice.authorizationStatus(for: .video) {
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
