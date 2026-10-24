import UIKit
import AVFoundation
import DZlogUi

// Swift owns Apple's camera permission API. Kotlin receives only the verified
// authorization result, so its PhotoKit/camera code cannot bypass a denial.
@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        let window = UIWindow(frame: UIScreen.main.bounds)
        self.window = window
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
