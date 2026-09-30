import UIKit
import SafariServices

public enum CustomTabsHelper {
    @MainActor
    public static func launchUrl(_ urlString: String, from viewController: UIViewController? = nil) {
        guard let url = URL(string: urlString), !urlString.trimmingCharacters(in: .whitespaces).isEmpty else {
            print("Invalid external application URL")
            return
        }

        let config = SFSafariViewController.Configuration()
        config.entersReaderIfAvailable = false

        let safariVC = SFSafariViewController(url: url, configuration: config)

        let presentingVC = viewController ?? topViewController()
        if let presentingVC = presentingVC {
            presentingVC.present(safariVC, animated: true)
        } else {
            UIApplication.shared.open(url, options: [:], completionHandler: nil)
        }
    }

    @MainActor
    private static func topViewController(controller: UIViewController? = nil) -> UIViewController? {
        let base = controller ?? UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }?.rootViewController

        if let nav = base as? UINavigationController {
            return topViewController(controller: nav.visibleViewController)
        }
        if let tab = base as? UITabBarController, let selected = tab.selectedViewController {
            return topViewController(controller: selected)
        }
        if let presented = base?.presentedViewController {
            return topViewController(controller: presented)
        }
        return base
    }
}
