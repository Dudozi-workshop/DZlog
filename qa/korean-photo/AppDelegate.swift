import UIKit
import CoreText
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
        let fonts = Bundle.main.bundleURL.appendingPathComponent("font-probe")
        let names = try! JSONDecoder().decode([String].self, from: Data(contentsOf: fonts.appendingPathComponent("names.json")))
        for file in try! FileManager.default.contentsOfDirectory(at: fonts, includingPropertiesForKeys: nil)
            where ["otf", "ttf"].contains(file.pathExtension) {
            precondition(CTFontManagerRegisterFontsForURL(file as CFURL, .process, nil))
        }
        let faces = names.map { UIFont(name: $0, size: UIFont.systemFontSize)!.fontDescriptor }
        let cascade = faces[0].addingAttributes([.cascadeList: Array(faces.dropFirst())])
        let matched = UIFont(descriptor: cascade, size: UIFont.systemFontSize)
        var metrics: [String: Any] = [:]
        let labels = Set(fixtures.flatMap { file in
            (try! String(contentsOf: file, encoding: .utf8))
                .split(whereSeparator: { $0 == "|" || $0.isNewline }).map(String.init)
        })
        for (key, font) in [("native", UIFont.systemFont(ofSize: 28)), ("matched", matched.withSize(28))] {
            var widths: [String: Double] = [:]
            var usedFaces: [String: [String]] = [:]
            for label in labels {
                let text = NSAttributedString(string: label, attributes: [.font: font])
                widths[label] = Double(text.size().width)
                let runs = CTLineGetGlyphRuns(CTLineCreateWithAttributedString(text)) as! [CTRun]
                usedFaces[label] = runs.map { run in
                    let attributes = CTRunGetAttributes(run) as NSDictionary
                    let face = attributes[kCTFontAttributeName] as! CTFont
                    return CTFontCopyPostScriptName(face) as String
                }
            }
            metrics[key] = ["lineHeight": font.ascender - font.descender, "widths": widths, "fonts": usedFaces]
        }
        try! JSONSerialization.data(withJSONObject: metrics).write(to: docs.appendingPathComponent("iphone-metrics.json"))
        for fixture in fixtures {
            let name = fixture.deletingPathExtension().lastPathComponent
            let text = try! String(contentsOf: fixture, encoding: .utf8)
            KoreanPhotoKt.writeKoreanPhoto(source: source.path, labels: text,
                                          output: docs.appendingPathComponent("iphone-\(name).jpg").path, probeFont: nil)
            KoreanPhotoKt.writeKoreanPhoto(source: source.path, labels: text,
                                          output: docs.appendingPathComponent("probe-\(name).jpg").path, probeFont: matched)
        }
        return true
    }
}
