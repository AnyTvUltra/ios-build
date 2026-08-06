import UIKit
import UserNotifications
import composeApp

@main
class AppDelegate: UIResponder, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        BootTrailSwift.rotate()
        BootTrailSwift.mark("swift:didFinishLaunching")
        BootTrailSwift.installExceptionHandler()
        BootTrailSwift.installSignalHandlers()

        registerForNotifications(application)
        BootTrailSwift.mark("swift:notificationsRequested")

        window = UIWindow(frame: UIScreen.main.bounds)
        BootTrailSwift.mark("swift:windowCreated")

        window?.rootViewController = MainViewControllerKt.MainViewController()
        BootTrailSwift.mark("swift:rootViewControllerSet")

        window?.makeKeyAndVisible()
        BootTrailSwift.mark("swift:windowVisible")
        return true
    }

    private func registerForNotifications(_ application: UIApplication) {
        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in }
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        DeepLinkHandler.handle(url: url)
        return true
    }

    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        completionHandler(.noData)
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        if #available(iOS 14.0, *) {
            completionHandler([.banner, .list, .sound, .badge])
        } else {
            completionHandler([.alert, .sound, .badge])
        }
    }
}

@objc class DeepLinkHandler: NSObject {
    @objc class func handle(url: URL) {
        // Broadcast the deep link URL to the KMP layer if needed.
        NotificationCenter.default.post(name: .init("AnyTVDeepLink"), object: url)
    }
}
