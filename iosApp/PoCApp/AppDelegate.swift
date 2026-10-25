import UIKit
import AVFoundation
import DZlogUi

// Swift owns Apple's camera permission API. Kotlin receives only the verified
// authorization result, so its PhotoKit/camera code cannot bypass a denial.
@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?
    private var cameraPresentedOrPending = false

    private static let openCamera = Notification.Name("dzlog.openCamera")
    private static let closeCamera = Notification.Name("dzlog.dismissCamera")

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
            let screen = CameraViewControllerKt.CameraViewController(cameraPermissionGranted: granted)
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
