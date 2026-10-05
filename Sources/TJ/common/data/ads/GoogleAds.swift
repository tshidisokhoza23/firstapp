import SwiftUI
import UIKit

#if canImport(GoogleMobileAds)
import GoogleMobileAds
#endif

enum AdConfig {
    static var appId: String { value("GADApplicationIdentifier") }
    static var bannerUnit: String? { unit("TJAdBannerUnit") }
    static var nativeUnit: String? { unit("TJAdNativeUnit") }
    static var rewardedUnit: String? { unit("TJAdRewardedUnit") }
    static var isConfigured: Bool { appId.contains("ca-app-pub-") }

    private static func unit(_ key: String) -> String? {
        let raw = value(key)
        return raw.contains("ca-app-pub-") ? raw : nil
    }

    private static func value(_ key: String) -> String {
        (Bundle.main.object(forInfoDictionaryKey: key) as? String ?? "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

enum GoogleAds {
    static func start() {
        guard AdConfig.isConfigured else { return }
        #if canImport(GoogleMobileAds)
        GADMobileAds.sharedInstance().start { _ in
            DispatchQueue.main.async { NativeAdPool.shared.warm() }
        }
        #endif
    }
}

enum AppChrome {
    static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        var top = scene?.windows.first(where: \.isKeyWindow)?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}

@MainActor
final class RewardedAds: ObservableObject {
    static let shared = RewardedAds()
    @Published var message: String?
    @Published var batchCount = 0
    @Published var cooldownSeconds = 0

    private let shortCooldown: TimeInterval = 90
    private let longCooldown: TimeInterval = 0
    private let batchLimit = 6
    private var timer: Timer?

    private init() {
        batchCount = UserDefaults.standard.integer(forKey: "ad_batch_count")
        publishCooldown()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            Task { @MainActor in RewardedAds.shared.publishCooldown() }
        }
    }

    var canWatch: Bool { cooldownSeconds == 0 && AdConfig.rewardedUnit != nil }

    var buttonTitle: String {
        if AdConfig.rewardedUnit == nil { return "Rewarded ad unit is not set" }
        if cooldownSeconds == 0 { return "Watch Rewarded Ad (+10 Pts)" }
        if cooldownSeconds < 120 { return "Wait \(cooldownSeconds)s" }
        return "Ad limit reached"
    }

    private var messageHandler: ((String) -> Void)?

    func show(taskId: String, onMessage: ((String) -> Void)? = nil, onReward: @escaping () -> Void) {
        messageHandler = onMessage
        publishCooldown()
        func note(_ text: String) {
            message = text
            onMessage?(text)
        }
        guard cooldownSeconds == 0 else {
            note(buttonTitle)
            return
        }
        guard let unit = AdConfig.rewardedUnit else {
            note("Rewarded ad unit is not set")
            return
        }
        guard let root = AppChrome.topViewController() else {
            note("Ad could not be presented")
            return
        }
        #if canImport(GoogleMobileAds)
        GADRewardedAd.load(withAdUnitID: unit, request: GADRequest()) { [weak self] ad, error in
            Task { @MainActor in
                guard let self else { return }
                guard let ad else {
                    let text = error?.localizedDescription ?? "Ad did not load"
                    self.message = text
                    self.messageHandler?(text)
                    return
                }
                ad.present(fromRootViewController: root) {
                    Task { @MainActor in
                        self.recordWatch()
                        await self.award(taskId: taskId, onReward: onReward)
                    }
                }
            }
        }
        #else
        note("Ads SDK is not linked")
        #endif
    }

    private func award(taskId: String, onReward: @escaping () -> Void) async {
        var body: [String: Any] = [
            "amount": 10.0,
            "source": "REWARDED_AD",
            "description": "Watched rewarded ad"
        ]
        if !taskId.isEmpty { body["task_id"] = taskId }
        do {
            _ = try await API.data("rewards/award", method: "POST", json: body)
            message = "Earned 10 points"
            messageHandler?("Earned 10 points")
            onReward()
        } catch {
            message = error.localizedDescription
            messageHandler?(error.localizedDescription)
        }
    }

    private func recordWatch() {
        let now = Date().timeIntervalSince1970
        var count = UserDefaults.standard.integer(forKey: "ad_batch_count")
        count += 1
        if count >= batchLimit {
            count = 0
            UserDefaults.standard.set(0, forKey: "short_cooldown_until")
            UserDefaults.standard.set(now + longCooldown, forKey: "long_cooldown_until")
        } else {
            UserDefaults.standard.set(now + shortCooldown, forKey: "short_cooldown_until")
        }
        UserDefaults.standard.set(count, forKey: "ad_batch_count")
        batchCount = count
        publishCooldown()
    }

    private func publishCooldown() {
        let now = Date().timeIntervalSince1970
        let shortUntil = UserDefaults.standard.double(forKey: "short_cooldown_until")
        let longUntil = UserDefaults.standard.double(forKey: "long_cooldown_until")
        let until = max(shortUntil, longUntil)
        cooldownSeconds = until > now ? Int(ceil(until - now)) : 0
        batchCount = UserDefaults.standard.integer(forKey: "ad_batch_count")
    }
}

struct LiveBannerAd: View {
    @State private var visible = false
    var body: some View {
        #if canImport(GoogleMobileAds)
        if AdConfig.bannerUnit != nil {
            BannerAdRepresentable(visible: $visible)
                .frame(height: visible ? 50 : 0)
                .clipped()
        }
        #endif
    }
}

struct NativeAdRow: View {
    var slot: String = "feed"
    #if canImport(GoogleMobileAds)
    private let ad: GADNativeAd?
    init(slot: String = "feed") {
        self.slot = slot
        ad = NativeAdPool.shared.claim(slot: slot)
    }
    var body: some View {
        if let ad {
            NativeAdBox(fullScreen: false, preloaded: ad)
                .frame(maxWidth: .infinity)
                .frame(height: 320)
                .clipped()
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
        }
    }
    #else
    init(slot: String = "feed") { self.slot = slot }
    var body: some View { EmptyView() }
    #endif
}

struct NativeAdCover: View {
    var continueTitle: String
    var onContinue: () -> Void
    var slot: String = "cover"
    #if canImport(GoogleMobileAds)
    var preloaded: GADNativeAd? = nil
    #endif
    @State private var failed = false
    @State private var loaded = false

    var body: some View {
        #if canImport(GoogleMobileAds)
        ZStack {
            Color.black.ignoresSafeArea()
            if !loaded {
                ProgressView().tint(.white)
            }
            NativeAdBox(fullScreen: true, preloaded: preloaded ?? NativeAdPool.shared.claim(slot: slot), failed: $failed, loaded: $loaded)
                .opacity(loaded ? 1 : 0)
            VStack {
                Text("Sponsored")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.top, 24)
                Spacer()
                Button(continueTitle, action: onContinue)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 14)
                    .background(Color(hex: "E91E63"))
                    .clipShape(Capsule())
                    .padding(.bottom, 32)
            }
        }
        #else
        Color.clear.onAppear(perform: onContinue)
        #endif
    }
}

struct StoryAdSlot: View {
    @State private var open = false
    #if canImport(GoogleMobileAds)
    private let ad: GADNativeAd?
    init() {
        ad = NativeAdPool.shared.claim(slot: "story-tray")
    }
    var body: some View {
        if let ad {
            Button { open = true } label: {
                VStack(spacing: 6) {
                    Circle()
                        .fill(Color.black.opacity(0.8))
                        .frame(width: 64, height: 64)
                        .overlay(Image(systemName: "megaphone.fill").foregroundColor(.white))
                    Text("Sponsored")
                        .font(.system(size: 11, weight: .bold))
                        .lineLimit(1)
                }
            }
            .buttonStyle(.plain)
            .frame(width: 76)
            .fullScreenCover(isPresented: $open) {
                NativeAdCover(continueTitle: "Close", onContinue: { open = false }, preloaded: ad)
            }
        }
    }
    #else
    var body: some View { EmptyView() }
    #endif
}

#if canImport(GoogleMobileAds)
private struct BannerAdRepresentable: UIViewRepresentable {
    @Binding var visible: Bool
    func makeCoordinator() -> Coordinator { Coordinator(visible: $visible) }

    func makeUIView(context: Context) -> GADBannerView {
        let banner = GADBannerView(adSize: GADAdSizeBanner)
        banner.adUnitID = AdConfig.bannerUnit
        banner.delegate = context.coordinator
        banner.isHidden = true
        return banner
    }

    func updateUIView(_ banner: GADBannerView, context: Context) {
        banner.rootViewController = AppChrome.topViewController()
        guard !context.coordinator.started, banner.adUnitID?.isEmpty == false else { return }
        context.coordinator.started = true
        banner.load(GADRequest())
    }

    final class Coordinator: NSObject, GADBannerViewDelegate {
        var started = false
        var visible: Binding<Bool>
        init(visible: Binding<Bool>) { self.visible = visible }
        func bannerViewDidReceiveAd(_ bannerView: GADBannerView) {
            bannerView.isHidden = false
            visible.wrappedValue = true
        }
        func bannerView(_ bannerView: GADBannerView, didFailToReceiveAdWithError error: Error) {
            bannerView.isHidden = true
            visible.wrappedValue = false
        }
    }
}

final class NativeAdPool: NSObject, GADNativeAdLoaderDelegate {
    static let shared = NativeAdPool()
    private var ready: [GADNativeAd] = []
    private var claimed: [String: GADNativeAd] = [:]
    private var skipped: Set<String> = []
    private var inflight = 0
    private let target = 3
    private var loader: GADAdLoader?

    func warm() {
        guard inflight + ready.count < target else { return }
        guard let unit = AdConfig.nativeUnit, let root = AppChrome.topViewController() else { return }
        let need = target - ready.count - inflight
        guard need > 0 else { return }
        inflight += need
        let loader = GADAdLoader(adUnitID: unit, rootViewController: root, adTypes: [.native], options: nil)
        self.loader = loader
        loader.delegate = self
        for _ in 0..<need {
            loader.load(GADRequest())
        }
    }

    func claim(slot: String) -> GADNativeAd? {
        if let existing = claimed[slot] { return existing }
        if skipped.contains(slot) { return nil }
        warm()
        guard let ad = ready.first else {
            skipped.insert(slot)
            return nil
        }
        ready.removeFirst()
        claimed[slot] = ad
        warm()
        return ad
    }

    func take() -> GADNativeAd? {
        warm()
        guard !ready.isEmpty else { return nil }
        return ready.removeFirst()
    }

    var hasReady: Bool { !ready.isEmpty }

    func adLoader(_ adLoader: GADAdLoader, didReceive nativeAd: GADNativeAd) {
        inflight = max(0, inflight - 1)
        ready.append(nativeAd)
    }

    func adLoader(_ adLoader: GADAdLoader, didFailToReceiveAdWithError error: Error) {
        inflight = max(0, inflight - 1)
    }
}

private struct NativeAdBox: UIViewRepresentable {
    var fullScreen: Bool
    var preloaded: GADNativeAd? = nil
    var failed: Binding<Bool> = .constant(false)
    var loaded: Binding<Bool> = .constant(false)

    func makeCoordinator() -> Coordinator { Coordinator(failed: failed, loaded: loaded) }

    func makeUIView(context: Context) -> NativeAdContainer {
        let view = NativeAdContainer(fullScreen: fullScreen)
        if let preloaded {
            view.bind(preloaded)
            view.isHidden = false
            context.coordinator.loaded.wrappedValue = true
        } else {
            view.isHidden = true
            context.coordinator.load(into: view)
        }
        return view
    }

    func updateUIView(_ uiView: NativeAdContainer, context: Context) {}

    final class Coordinator: NSObject, GADNativeAdLoaderDelegate {
        var failed: Binding<Bool>
        var loaded: Binding<Bool>
        private var loader: GADAdLoader?
        init(failed: Binding<Bool>, loaded: Binding<Bool>) {
            self.failed = failed
            self.loaded = loaded
        }

        func load(into view: NativeAdContainer) {
            guard let unit = AdConfig.nativeUnit, let root = AppChrome.topViewController() else {
                failed.wrappedValue = true
                return
            }
            let loader = GADAdLoader(adUnitID: unit, rootViewController: root, adTypes: [.native], options: nil)
            self.loader = loader
            loader.delegate = self
            loader.load(GADRequest())
            self.view = view
        }

        private weak var view: NativeAdContainer?

        func adLoader(_ adLoader: GADAdLoader, didReceive nativeAd: GADNativeAd) {
            view?.bind(nativeAd)
            view?.isHidden = false
            loaded.wrappedValue = true
        }

        func adLoader(_ adLoader: GADAdLoader, didFailToReceiveAdWithError error: Error) {
            view?.isHidden = true
            failed.wrappedValue = true
        }
    }
}

private final class NativeAdContainer: UIView {
    private let adView = GADNativeAdView()
    private let headline = UILabel()
    private let body = UILabel()
    private let advertiser = UILabel()
    private let callToAction = UIButton(type: .system)
    private let media = GADMediaView()

    init(fullScreen: Bool) {
        super.init(frame: .zero)
        headline.font = .boldSystemFont(ofSize: 16)
        headline.textColor = .white
        headline.numberOfLines = 2
        body.font = .systemFont(ofSize: 14)
        body.textColor = .white
        body.numberOfLines = 3
        advertiser.font = .systemFont(ofSize: 12)
        advertiser.textColor = .lightGray
        callToAction.setTitleColor(.white, for: .normal)
        callToAction.backgroundColor = UIColor(red: 0.91, green: 0.12, blue: 0.39, alpha: 1)
        callToAction.layer.cornerRadius = 16
        callToAction.isUserInteractionEnabled = false
        media.contentMode = .scaleAspectFit
        adView.translatesAutoresizingMaskIntoConstraints = false
        [media, headline, body, advertiser, callToAction].forEach {
            $0.translatesAutoresizingMaskIntoConstraints = false
            adView.addSubview($0)
        }
        addSubview(adView)
        adView.headlineView = headline
        adView.bodyView = body
        adView.advertiserView = advertiser
        adView.callToActionView = callToAction
        adView.mediaView = media
        let mediaHeight: CGFloat = fullScreen ? 420 : 180
        NSLayoutConstraint.activate([
            adView.leadingAnchor.constraint(equalTo: leadingAnchor),
            adView.trailingAnchor.constraint(equalTo: trailingAnchor),
            adView.topAnchor.constraint(equalTo: topAnchor),
            adView.bottomAnchor.constraint(equalTo: bottomAnchor),
            media.topAnchor.constraint(equalTo: adView.topAnchor, constant: fullScreen ? 72 : 8),
            media.leadingAnchor.constraint(equalTo: adView.leadingAnchor, constant: 12),
            media.trailingAnchor.constraint(equalTo: adView.trailingAnchor, constant: -12),
            media.heightAnchor.constraint(equalToConstant: mediaHeight),
            headline.topAnchor.constraint(equalTo: media.bottomAnchor, constant: 8),
            headline.leadingAnchor.constraint(equalTo: media.leadingAnchor),
            headline.trailingAnchor.constraint(equalTo: media.trailingAnchor),
            body.topAnchor.constraint(equalTo: headline.bottomAnchor, constant: 4),
            body.leadingAnchor.constraint(equalTo: media.leadingAnchor),
            body.trailingAnchor.constraint(equalTo: media.trailingAnchor),
            advertiser.topAnchor.constraint(equalTo: body.bottomAnchor, constant: 4),
            advertiser.leadingAnchor.constraint(equalTo: media.leadingAnchor),
            callToAction.topAnchor.constraint(equalTo: advertiser.bottomAnchor, constant: 8),
            callToAction.leadingAnchor.constraint(equalTo: media.leadingAnchor),
            callToAction.heightAnchor.constraint(equalToConstant: 36),
            callToAction.widthAnchor.constraint(greaterThanOrEqualToConstant: 88)
        ])
    }

    required init?(coder: NSCoder) { nil }

    func bind(_ ad: GADNativeAd) {
        headline.text = ad.headline
        body.text = ad.body
        advertiser.text = ad.advertiser
        callToAction.setTitle(ad.callToAction, for: .normal)
        callToAction.isHidden = ad.callToAction == nil
        media.mediaContent = ad.mediaContent
        adView.nativeAd = ad
    }
}
#endif
