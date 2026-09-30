import Foundation
import UIKit

public protocol ShareablePostProtocol {
    var id: String { get }
    var content: String { get }
}

public final class ShareUtils {

    public static func sharePost(
        post: ShareablePostProtocol,
        from viewController: UIViewController
    ) {
        let postUrl = "https://firstapp.com/post/\(post.id)"
        let text = "Check out this post on FirstApp: \(post.content)\n\n\(postUrl)"

        let activityViewController = UIActivityViewController(
            activityItems: [text],
            applicationActivities: nil
        )

        if let popoverController = activityViewController.popoverPresentationController {
            popoverController.sourceView = viewController.view
            popoverController.sourceRect = CGRect(x: viewController.view.bounds.midX, y: viewController.view.bounds.midY, width: 0, height: 0)
            popoverController.permittedArrowDirections = []
        }

        viewController.present(activityViewController, animated: true, completion: nil)
    }

    public static func copyPostLink(post: ShareablePostProtocol) {
        let postUrl = "https://firstapp.com/post/\(post.id)"
        UIPasteboard.general.string = postUrl
    }
}
