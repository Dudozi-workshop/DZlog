import UIKit
import DZlogUi

@main
final class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions options: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        let source = Bundle.main.url(forResource: "source", withExtension: "jpg")!
        let cells = Bundle.main.url(forResource: "cells", withExtension: "txt")!
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        KoreanPhotoKt.writeKoreanPhoto(source: source.path, labels: try! String(contentsOf: cells, encoding: .utf8),
                                      output: docs.appendingPathComponent("iphone.jpg").path)
        return true
    }
}
