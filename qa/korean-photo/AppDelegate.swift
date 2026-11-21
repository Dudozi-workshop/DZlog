import UIKit
import DZlogUi

@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions options: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        let source = Bundle.main.url(forResource: "source", withExtension: "jpg")!
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let fixtures = (Bundle.main.urls(forResourcesWithExtension: "txt", subdirectory: nil) ?? [])
            .filter { $0.lastPathComponent.hasPrefix("cells") }
        precondition(!fixtures.isEmpty)
        for fixture in fixtures {
            let name = fixture.deletingPathExtension().lastPathComponent
            let text = try! String(contentsOf: fixture, encoding: .utf8)
            KoreanPhotoKt.writeKoreanPhoto(source: source.path, labels: text,
                                          output: docs.appendingPathComponent("iphone-\(name).jpg").path)
        }
        return true
    }
}
