import SwiftUI
import UIKit
import CoreLocation
import PhotosUI

final class CourtLocation: NSObject, CLLocationManagerDelegate {
    static let shared = CourtLocation()
    private let manager = CLLocationManager()
    private var continuation: CheckedContinuation<CLLocation?, Never>?

    func current() async -> CLLocation? {
        await withCheckedContinuation { continuation in
            self.continuation = continuation
            manager.delegate = self
            manager.requestWhenInUseAuthorization()
            manager.requestLocation()
        }
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        continuation?.resume(returning: locations.first)
        continuation = nil
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        continuation?.resume(returning: nil)
        continuation = nil
    }

    var isDenied: Bool {
        let status = manager.authorizationStatus
        return status == .denied || status == .restricted
    }
}

public struct MingleProfile: Identifiable, Hashable {
    public let id: String
    public let name: String
    public let age: Int
    public let occupation: String
    public let distanceKm: Double
    public let bio: String
    public let interests: [String]
    public let avatarGradient: [Color]
    public let photos: [String]
    public let mutualConnections: Int

    public init(
        id: String,
        name: String,
        age: Int,
        occupation: String,
        distanceKm: Double,
        bio: String,
        interests: [String],
        avatarGradient: [Color],
        photos: [String] = [],
        mutualConnections: Int = 0
    ) {
        self.id = id
        self.name = name
        self.age = age
        self.occupation = occupation
        self.distanceKm = distanceKm
        self.bio = bio
        self.interests = interests
        self.avatarGradient = avatarGradient
        self.photos = photos
        self.mutualConnections = mutualConnections
    }
}

public struct MingleView: View {
    @State private var profiles: [MingleProfile] = []
    @State private var note = ""
    @State private var selectedRadiusKm: Double = 25
    @State private var showRadiusSheet = false
    @State private var showProfileSheet = false
    @State private var showSecretChats = false
    @State private var showNotifications = false
    @State private var showPointsGate = false
    @State private var showMingleAd = false
    @State private var swipesSinceAd = 0
    @State private var showWallet = false
    @State private var locationDenied = false
    @State private var emptyReason: String?
    @State private var isLoadingFeed = false
    @State private var likeCost = 10
    @State private var nextLikeCost = 0
    @State private var introTarget: MingleProfile?
    @State private var introText = ""
    @State private var userCoins = 0
    @State private var minBalance = 0
    @State private var freeLikesRemaining = 0
    @State private var introCost = 0
    @State private var superLikeCost = 0
    @State private var boostCost = 0
    @State private var extendCost = 0
    @State private var lastSwiped: MingleProfile?
    @State private var myName = ""
    @State private var myAge = ""
    @State private var myBio = ""
    @State private var myGender = ""
    @State private var myLookingFor = "everyone"
    @State private var myTargetAgeRange = "18 - 35"
    @State private var myPhotos: [String] = []
    @State private var myPhotoIds: [String] = []
    @State private var feedOffset = 0

    private var onMatchClick: ((String, String) -> Void)?
    private var onUpgradeClick: (() -> Void)?

    public init(
        onMatchClick: ((String, String) -> Void)? = nil,
        onUpgradeClick: (() -> Void)? = nil
    ) {
        self.onMatchClick = onMatchClick
        self.onUpgradeClick = onUpgradeClick
    }

    public var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button(action: { showSecretChats = true }) {
                    Image(systemName: "envelope")
                        .font(.system(size: 20, weight: .bold))
                }
                Spacer().frame(width: 16)
                Button(action: { showNotifications = true }) {
                    Image(systemName: "bell")
                        .font(.system(size: 20, weight: .bold))
                }
                Spacer().frame(width: 16)
                Button(action: rewindLastSwipe) {
                    Image(systemName: "arrow.counterclockwise")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.appPrimary)
                }
                Spacer()
                AppLogo(size: 32)
                Spacer()
                Button(action: { showRadiusSheet = true }) {
                    HStack(spacing: 4) {
                        Image(systemName: "location.fill")
                            .font(.system(size: 12))
                            .foregroundColor(.appPrimary)
                        Text(selectedRadiusKm >= 100 ? "100+ km" : "\(Int(selectedRadiusKm)) km")
                            .font(.system(size: 13, weight: .bold))
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color(UIColor.secondarySystemBackground))
                    .clipShape(Capsule())
                }
                Spacer().frame(width: 12)
                Button(action: { showProfileSheet = true }) {
                    Image(systemName: "camera.fill")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(hasPhoto ? .appPrimary : .red)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            Divider()

            if !hasPhoto {
                Button(action: { showProfileSheet = true }) {
                    HStack {
                        Image(systemName: "camera.fill")
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Profile Requirement").font(.system(size: 13, weight: .bold))
                            Text("Upload photos to complete your Mingle profile to like candidates.")
                                .font(.system(size: 11))
                        }
                        Spacer()
                        Text("Upload Now")
                            .font(.system(size: 12, weight: .bold))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(Color.red)
                            .foregroundColor(.white)
                            .clipShape(Capsule())
                    }
                    .padding(12)
                    .background(Color.red.opacity(0.12))
                    .cornerRadius(12)
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
            }

            if profiles.isEmpty {
                Spacer()
                emptyDeck
                Spacer()
            } else if let candidate = profiles.first {
                CandidateCardView(
                    profile: candidate,
                    onPass: { Task { await swipe(candidate, action: "pass") } },
                    onLike: { like(candidate) },
                    onChat: {
                        introText = ""
                        introTarget = candidate
                    },
                    onPreview: { note = "Secret Mingle Profile - Profile details are kept private." },
                    onSuperLike: { Task { await swipe(candidate, action: "super_like") } },
                    onBoost: { Task { await boost() } },
                    superLikeCost: superLikeCost,
                    boostCost: boostCost
                )
            }
        }
        .overlay(alignment: .bottom) {
            if !note.isEmpty {
                Text(note)
                    .font(.subheadline)
                    .foregroundColor(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.black.opacity(0.82))
                    .cornerRadius(12)
                    .padding(.bottom, 24)
            }
        }
        .task {
            await loadMe()
            await loadFeed()
        }
        .sheet(isPresented: $showRadiusSheet) {
            RadiusFilterSheetView(radiusKm: $selectedRadiusKm) {
                showRadiusSheet = false
                Task { await loadFeed() }
            }
            .presentationDetents([.height(320)])
        }
        .sheet(isPresented: $showProfileSheet) {
            MingleProfileEditSheetView(
                myName: $myName,
                myAge: $myAge,
                myBio: $myBio,
                myGender: $myGender,
                myLookingFor: $myLookingFor,
                myTargetAgeRange: $myTargetAgeRange,
                photos: $myPhotos,
                photoIds: $myPhotoIds,
                onPhoto: { data in Task { await uploadPhoto(data) } },
                onDelete: { id in Task { await deletePhoto(id) } },
                onSave: {
                    showProfileSheet = false
                    Task { await saveMe() }
                }
            )
            .presentationDetents([.large])
        }
        .onReceive(NotificationCenter.default.publisher(for: TJEvents.openMingle)) { _ in
            showSecretChats = true
        }
        .sheet(isPresented: $showSecretChats) {
            MingleInboxSheet { conversationId, name in
                showSecretChats = false
                onMatchClick?(conversationId, name)
            }
        }
        .sheet(isPresented: $showNotifications) {
            MingleActivitySheet { conversationId, name in
                showNotifications = false
                onMatchClick?(conversationId, name)
            }
        }
        .sheet(isPresented: $showPointsGate) {
            MinglePointsGate(points: userCoins, onWallet: {
                showPointsGate = false
                showWallet = true
            }, onWatchAd: {
                RewardedAds.shared.show(taskId: "") {
                    Task {
                        if let data = try? await API.data("rewards/balance"),
                           let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                            if let value = json["balance"] as? Double { userCoins = Int(value) }
                            else if let value = json["balance"] as? Int { userCoins = value }
                        }
                    }
                }
            }, onDismiss: { showPointsGate = false })
            .presentationDetents([.medium])
        }
        .fullScreenCover(isPresented: $showMingleAd) {
            NativeAdCover(continueTitle: "Continue Mingling") { showMingleAd = false }
        }
        .sheet(isPresented: $showWallet) {
            WalletView()
        }
        .sheet(item: $introTarget) { target in
            introSheet(target)
        }
    }

    private var hasPhoto: Bool { myPhotos.contains { !$0.isEmpty } }

    private var emptyDeck: some View {
        let copy = MingleCopy.emptyDeck(reason: emptyReason, radiusKm: Int(selectedRadiusKm), denied: locationDenied)
        return VStack(spacing: 16) {
            Image(systemName: "location.slash.fill")
                .font(.system(size: 42))
                .foregroundColor(.appPrimary)
                .frame(width: 90, height: 90)
                .background(Color.appPrimary.opacity(0.12))
                .clipShape(Circle())
            Text(isLoadingFeed ? "Loading..." : copy.title)
                .font(.system(size: 20, weight: .bold))
                .multilineTextAlignment(.center)
            Text(copy.body)
                .font(.system(size: 14))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            if emptyReason == "DAILY_LIMIT" {
                Button(action: { Task { await extendFeed("coins") } }) {
                    Text(extendCost > 0 ? "See 30 more for \(extendCost) coins" : "Daily limit")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                        .background(Color.appPrimary)
                        .clipShape(Capsule())
                }
                .disabled(extendCost <= 0)
                Button(action: {
                    RewardedAds.shared.show(taskId: "mingle_feed_extend") {
                        Task { await extendFeed("ad") }
                    }
                }) {
                    Text("Watch 1 ad for 20 more profiles")
                        .font(.system(size: 15, weight: .bold))
                }
            } else if !copy.action.isEmpty {
                Button(action: { emptyAction(copy.action) }) {
                    Text(copy.action)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                        .background(Color.appPrimary)
                        .clipShape(Capsule())
                }
            }
        }
    }

    private func introSheet(_ target: MingleProfile) -> some View {
        VStack(spacing: 16) {
            Text(target.name.isEmpty ? "Say hi" : "Say hi to \(target.name)")
                .font(.title3.bold())
                .padding(.top, 24)
            Text("One message, up to 150 characters. This costs \(introCost) points and cannot be refunded. They will not see that it was paid.")
                .font(.system(size: 13))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
            TextEditor(text: Binding(get: { introText }, set: { introText = String($0.prefix(150)) }))
                .frame(height: 100)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.gray.opacity(0.3), lineWidth: 1))
                .padding(.horizontal, 24)
            Text("\(introText.count)/150  Plain text only. No links.")
                .font(.caption)
                .foregroundColor(.secondary)
            Button(action: {
                let text = introText.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !text.isEmpty, introCost > 0 else { return }
                if MingleCopy.hasLink(text) {
                    note = "intro must be plain text up to 150 characters with no links"
                    return
                }
                introTarget = nil
                introText = ""
                Task { await sendIntro(recipientId: target.id, name: target.name, message: text) }
            }) {
                Text(introCost > 0 ? "Send for \(introCost) points" : "Waiting for the server price")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(Color.appPrimary)
                    .cornerRadius(12)
                    .padding(.horizontal, 24)
            }
            Spacer()
        }
    }

    private func emptyAction(_ label: String) {
        if label == "Edit filters" {
            showProfileSheet = true
        } else if label == "Increase Radius" || label == "Look farther" {
            showRadiusSheet = true
        } else {
            Task { await loadFeed() }
        }
    }

    private func like(_ profile: MingleProfile) {
        guard hasPhoto else {
            showProfileSheet = true
            note = "You must upload at least one photo or complete your Mingle profile before you can like people."
            return
        }
        Task { await swipe(profile, action: "like") }
    }

    private func loadFeed(reset: Bool = true) async {
        await SessionActions.syncContacts()
        if reset { feedOffset = 0 }
        isLoadingFeed = true
        defer { isLoadingFeed = false }
        guard let loc = await CourtLocation.shared.current() else {
            locationDenied = CourtLocation.shared.isDenied
            emptyReason = "NO_LOCATION"
            if reset { profiles = [] }
            return
        }
        locationDenied = false
        let lat = loc.coordinate.latitude
        let lng = loc.coordinate.longitude
        guard lat != 0 || lng != 0 else {
            emptyReason = "NO_LOCATION"
            if reset { profiles = [] }
            return
        }
        do {
            _ = try? await API.data("mingle/location", method: "POST", json: [
                "latitude": lat,
                "longitude": lng,
                "radius_km": Int(selectedRadiusKm)
            ])
            let data = try await API.data("mingle/feed", query: [
                "lat": "\(lat)",
                "lng": "\(lng)",
                "radius_km": "\(Int(selectedRadiusKm))",
                "limit": "20",
                "offset": "\(feedOffset)"
            ])
            let json = LiveJSON.object(data)
            applyEconomy(json)
            let items = LiveJSON.list(data, keys: ["items"])
            var resultProfiles: [MingleProfile] = []
            for item in items {
                let primaryId = LiveJSON.string(item["id"])
                let id = primaryId.isEmpty ? LiveJSON.string(item["user_id"]) : primaryId
                let photos = photoURLs(item["photos"])
                guard !id.isEmpty, !photos.isEmpty else { continue }
                let primaryName = LiveJSON.string(item["name"])
                let rawName = primaryName.isEmpty ? LiveJSON.string(item["display_name"]) : primaryName
                resultProfiles.append(MingleProfile(
                    id: id,
                    name: MingleCopy.realName(rawName),
                    age: Int(LiveJSON.string(item["age"])) ?? 0,
                    occupation: "",
                    distanceKm: Double(LiveJSON.string(item["distance_km"])) ?? 0,
                    bio: LiveJSON.string(item["bio"]),
                    interests: [],
                    avatarGradient: [.purple, .blue],
                    photos: photos
                ))
            }
            if reset {
                profiles = resultProfiles
            } else {
                let known = Set(profiles.map(\.id))
                profiles.append(contentsOf: resultProfiles.filter { !known.contains($0.id) })
            }
            feedOffset += resultProfiles.count
            if profiles.isEmpty {
                let status = LiveJSON.string(json["status"])
                emptyReason = status.isEmpty ? "FEED_EXHAUSTED" : status
            } else {
                emptyReason = nil
            }
        } catch {
            note = MingleCopy.serverMessage(error)
        }
    }

    private func photoURLs(_ value: Any?) -> [String] {
        guard let rows = value as? [Any] else { return [] }
        return rows.compactMap { row in
            if let url = row as? String, !url.isEmpty { return url }
            guard let photo = row as? [String: Any] else { return nil }
            let url = LiveJSON.string(photo["url"])
            if !url.isEmpty { return url }
            let media = LiveJSON.string(photo["media_url"])
            return media.isEmpty ? nil : media
        }
    }

    private func applyEconomy(_ json: [String: Any]) {
        if let balance = json["balance"] as? Double { userCoins = Int(balance) }
        else if let balance = json["balance"] as? Int { userCoins = balance }
        if let floor = json["min_balance"] as? Double { minBalance = Int(floor) }
        else if let floor = json["min_balance"] as? Int { minBalance = floor }
        if let free = json["free_likes_remaining"] as? Int { freeLikesRemaining = free }
        else if let free = json["free_likes_remaining"] as? Double { freeLikesRemaining = Int(free) }
        if let cost = json["intro_cost"] as? Double, cost > 0 { introCost = Int(cost) }
        else if let cost = json["intro_cost"] as? Int, cost > 0 { introCost = cost }
        if let cost = json["like_cost"] as? Double, cost > 0 { likeCost = Int(cost) }
        else if let cost = json["like_cost"] as? Int, cost > 0 { likeCost = cost }
        if let next = json["next_like_cost"] as? Double { nextLikeCost = Int(next) }
        else if let next = json["next_like_cost"] as? Int { nextLikeCost = next }
        if let cost = json["super_like_cost"] as? Double, cost > 0 { superLikeCost = Int(cost) }
        else if let cost = json["super_like_cost"] as? Int, cost > 0 { superLikeCost = cost }
        if let cost = json["boost_cost"] as? Double, cost > 0 { boostCost = Int(cost) }
        else if let cost = json["boost_cost"] as? Int, cost > 0 { boostCost = cost }
        if let cost = json["extend_cost"] as? Double, cost > 0 { extendCost = Int(cost) }
        else if let cost = json["extend_cost"] as? Int, cost > 0 { extendCost = cost }
    }

    private func saveMe() async {
        let name = myName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else {
            note = "Please enter your preferred Mingle display name"
            showProfileSheet = true
            return
        }
        guard let age = Int(myAge), (18...120).contains(age) else {
            note = "Enter an age from 18 to 120"
            showProfileSheet = true
            return
        }
        do {
            _ = try await API.data("mingle/me", method: "PUT", json: [
                "name": name,
                "bio": myBio,
                "age": age,
                "gender": myGender.isEmpty ? "other" : myGender,
                "looking_for": myLookingFor,
                "target_age_range": myTargetAgeRange
            ])
            note = "Mingle Profile Updated"
            _ = try? await API.data("users/me", method: "PUT", json: ["display_name": name])
            if let me = try? await API.data("users/me"),
               let envelope = try? JSONDecoder().decode(UserEnvelope.self, from: me) {
                SessionStore.shared.updateUser(envelope.user)
            }
            await loadMe()
        } catch {
            note = MingleCopy.serverMessage(error)
            showProfileSheet = true
        }
    }

    private func uploadPhoto(_ data: Data) async {
        guard myPhotos.filter({ !$0.isEmpty }).count < 5 else {
            note = "maximum of 5 photos allowed"
            return
        }
        let boundary = "Boundary-\(UUID().uuidString)"
        let jpeg = UIImage(data: data)?.jpegData(compressionQuality: 0.85) ?? data
        var body = Data()
        body.append(Data("--\(boundary)\r\nContent-Disposition: form-data; name=\"photo\"; filename=\"photo.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n".utf8))
        body.append(jpeg)
        body.append(Data("\r\n--\(boundary)--\r\n".utf8))
        guard var req = API.request("mingle/photos", method: "POST", body: body) else { return }
        req.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        do {
            let (responseData, response) = try await URLSession.shared.data(for: req)
            let code = (response as? HTTPURLResponse)?.statusCode ?? 0
            guard (200...299).contains(code) else {
                note = String(data: responseData, encoding: .utf8) ?? "Photo upload failed"
                return
            }
            await loadMe()
        } catch {
            note = "Photo upload failed"
        }
    }

    private func deletePhoto(_ id: String) async {
        guard !id.isEmpty else { return }
        do {
            _ = try await API.data("mingle/photos/\(id)", method: "DELETE")
            await loadMe()
        } catch {
            note = MingleCopy.serverMessage(error)
        }
    }

    private func loadMe() async {
        guard let data = try? await API.data("mingle/me"),
              let profile = LiveJSON.object(data)["profile"] as? [String: Any] else { return }
        let name = MingleCopy.realName(LiveJSON.string(profile["name"]))
        let sessionName = MingleCopy.realName(SessionStore.shared.user?.displayName ?? "")
        myName = name.isEmpty ? sessionName : name
        let bio = LiveJSON.string(profile["bio"])
        myBio = bio.isEmpty ? (SessionStore.shared.user?.bio ?? "") : bio
        let age = LiveJSON.string(profile["age"])
        if age != "0" && !age.isEmpty { myAge = age }
        myGender = LiveJSON.string(profile["gender"])
        let looking = LiveJSON.string(profile["looking_for"])
        if !looking.isEmpty { myLookingFor = looking }
        let range = LiveJSON.string(profile["target_age_range"])
        if !range.isEmpty { myTargetAgeRange = range }
        let radius = Double(LiveJSON.string(profile["max_distance_km"])) ?? 0
        if radius > 0 { selectedRadiusKm = min(100, max(1, radius)) }
        if let photos = profile["photos"] as? [[String: Any]] {
            myPhotos = photos.map { LiveJSON.string($0["url"]) }.filter { !$0.isEmpty }
            myPhotoIds = photos.map { LiveJSON.string($0["id"]) }
        }
    }

    private func swipe(_ profile: MingleProfile, action: String) async {
        let price = action == "super_like" ? superLikeCost : (freeLikesRemaining > 0 ? 0 : max(nextLikeCost, likeCost))
        if action == "like" || action == "super_like", price > 0, userCoins < price + minBalance {
            showPointsGate = true
            return
        }
        do {
            let data = try await API.data("mingle/swipe", method: "POST", json: [
                "target_user_id": profile.id,
                "action": action
            ])
            let json = LiveJSON.object(data)
            applyEconomy(json)
            lastSwiped = profile
            profiles.removeAll { $0.id == profile.id }
            swipesSinceAd += 1
            if swipesSinceAd >= 8 {
                swipesSinceAd = 0
                showMingleAd = true
            }
            if profiles.count < 3 { await loadFeed(reset: false) }
            let conversationId = LiveJSON.string(json["conversation_id"])
            if json["matched"] as? Bool == true, !conversationId.isEmpty {
                note = profile.name.isEmpty ? "It's a match" : "It's a match with \(profile.name)"
                onMatchClick?(conversationId, profile.name)
            }
        } catch {
            if !profiles.contains(where: { $0.id == profile.id }) {
                profiles.insert(profile, at: 0)
            }
            let message = MingleCopy.serverMessage(error)
            if message.localizedCaseInsensitiveContains("insufficient") {
                showPointsGate = true
            } else {
                note = message
            }
        }
    }

    private func sendIntro(recipientId: String, name: String, message: String) async {
        if userCoins < introCost + minBalance {
            showPointsGate = true
            return
        }
        do {
            let data = try await API.data("mingle/intro", method: "POST", json: [
                "recipient_id": recipientId,
                "message": String(message.prefix(150))
            ])
            let json = LiveJSON.object(data)
            applyEconomy(json)
            let conversationId = LiveJSON.string(json["conversation_id"])
            if !conversationId.isEmpty {
                onMatchClick?(conversationId, name)
            } else {
                note = "Request sent"
            }
        } catch {
            let message = MingleCopy.serverMessage(error)
            if message.localizedCaseInsensitiveContains("insufficient") {
                showPointsGate = true
            } else {
                note = message
            }
        }
    }

    private func extendFeed(_ method: String) async {
        do {
            let data = try await API.data("mingle/feed/extend", method: "POST", json: ["method": method])
            applyEconomy(LiveJSON.object(data))
            await loadFeed()
        } catch {
            let message = MingleCopy.serverMessage(error)
            if message.localizedCaseInsensitiveContains("insufficient") {
                showPointsGate = true
            } else if (error as NSError).code == 404 || message.localizedCaseInsensitiveContains("not found") {
                note = "This action is not on the server this phone uses"
            } else {
                note = message
            }
        }
    }

    private func boost() async {
        guard boostCost > 0 else {
            note = "Boost price has not loaded from the server"
            return
        }
        if userCoins < boostCost + minBalance {
            showPointsGate = true
            return
        }
        do {
            let data = try await API.data("mingle/boost", method: "POST", json: [:])
            applyEconomy(LiveJSON.object(data))
            note = "Profile boosted for 30 minutes"
        } catch {
            let message = MingleCopy.serverMessage(error)
            if message.localizedCaseInsensitiveContains("insufficient") {
                showPointsGate = true
            } else {
                note = message
            }
        }
    }

    private func rewindLastSwipe() {
        Task {
            do {
                let data = try await API.data("mingle/rewind", method: "POST")
                let json = LiveJSON.object(data)
                guard json["rewound"] as? Bool == true else {
                    note = "Nothing to rewind"
                    return
                }
                applyEconomy(json)
                let target = LiveJSON.string(json["target_user_id"])
                if let last = lastSwiped, last.id == target {
                    profiles.insert(last, at: 0)
                } else {
                    await loadFeed()
                }
                note = "Restored previous candidate"
            } catch {
                note = MingleCopy.serverMessage(error)
            }
        }
    }
}

public struct CandidateCardView: View {
    let profile: MingleProfile
    var onPass: () -> Void
    var onLike: () -> Void
    var onChat: () -> Void
    var onPreview: () -> Void
    var onSuperLike: () -> Void = {}
    var onBoost: () -> Void = {}
    var superLikeCost: Int = 0
    var boostCost: Int = 0
    @State private var photoIndex = 0

    public var body: some View {
        VStack(spacing: 0) {
            ZStack(alignment: .bottomLeading) {
                photo
                LinearGradient(colors: [.clear, .black.opacity(0.65)], startPoint: .center, endPoint: .bottom)
                VStack(alignment: .leading, spacing: 4) {
                    Button(action: onPreview) {
                        Text(cardTitle)
                            .font(.title2.bold())
                            .foregroundColor(.white)
                    }
                    if !profile.bio.isEmpty {
                        Text(profile.bio)
                            .font(.subheadline)
                            .foregroundColor(.white.opacity(0.9))
                            .lineLimit(3)
                    }
                }
                .padding(16)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .padding(.horizontal, 16)
            .padding(.top, 12)

            HStack(spacing: 28) {
                round("xmark", .red, onPass)
                if superLikeCost > 0 {
                    round("star.fill", .yellow, onSuperLike)
                }
                round("bubble.left.fill", .appPrimary, onChat)
                round("heart.fill", .appPrimary, onLike)
            }
            .padding(.vertical, 16)
            if boostCost > 0 {
                Button("Boost for \(boostCost) coins, 30 minutes", action: onBoost)
                    .font(.system(size: 13, weight: .bold))
                    .padding(.bottom, 8)
            }
        }
    }

    private var cardTitle: String {
        var parts: [String] = []
        if !profile.name.isEmpty { parts.append(profile.name) }
        if profile.age > 0 { parts.append("\(profile.age)") }
        if profile.distanceKm > 0 { parts.append(String(format: "%.0f km", profile.distanceKm)) }
        return parts.joined(separator: ", ")
    }

    private var photo: some View {
        let url = profile.photos.indices.contains(photoIndex) ? profile.photos[photoIndex] : profile.photos.first
        return Group {
            if let resolved = FeedPost.resolve(url) {
                AsyncImage(url: resolved) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Color.gray.opacity(0.2)
                }
            } else {
                Color.gray.opacity(0.2)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
        .contentShape(Rectangle())
        .onTapGesture {
            guard profile.photos.count > 1 else { return }
            photoIndex = (photoIndex + 1) % profile.photos.count
        }
    }

    private func round(_ icon: String, _ color: Color, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(color)
                .frame(width: 58, height: 58)
                .background(Color(UIColor.secondarySystemBackground))
                .clipShape(Circle())
        }
    }
}

struct RadiusFilterSheetView: View {
    @Binding var radiusKm: Double
    var onApply: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Distance")
                .font(.title3.bold())
            Text(radiusKm >= 100 ? "Showing people everywhere (expanding beyond 100 km)" : "Show people within \(Int(radiusKm)) km")
                .foregroundColor(.secondary)
            Slider(value: $radiusKm, in: 1...100, step: 1)
            HStack {
                Text("1 km")
                Spacer()
                Text("100 km")
            }
            .font(.caption)
            .foregroundColor(.secondary)
            Button("Apply Filter", action: onApply)
                .buttonStyle(.borderedProminent)
        }
        .padding(24)
    }
}

public struct MingleProfileEditSheetView: View {
    @Binding public var myName: String
    @Binding public var myAge: String
    @Binding public var myBio: String
    @Binding public var myGender: String
    @Binding public var myLookingFor: String
    @Binding public var myTargetAgeRange: String
    @Binding public var photos: [String]
    @Binding public var photoIds: [String]
    public var onPhoto: (Data) -> Void
    public var onDelete: (String) -> Void
    public var onSave: () -> Void
    @State private var pickerItems: [PhotosPickerItem] = []
    @State private var nameError = false

    private let ageRanges = ["18 - 25", "18 - 30", "18 - 35", "25 - 35", "30 - 45", "35 - 50", "18+ (All ages 18+)"]

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: 10) {
                    Image(systemName: "person.crop.circle.fill")
                        .font(.system(size: 28))
                        .foregroundColor(.appPrimary)
                    Text("My Mingle Profile")
                        .font(.system(size: 22, weight: .bold))
                }
                .padding(.top, 8)
                Text("Update your photos and profile details visible to people on Mingle.")
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)
                    .padding(.top, 4)

                Text("Photos (Up to 5)")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.appPrimary)
                    .padding(.top, 20)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(0..<5, id: \.self) { index in
                            photoSlot(index)
                        }
                    }
                    .padding(.top, 8)
                }
                galleryButton
                    .padding(.top, 12)

                Text("Profile Details")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(.appPrimary)
                    .padding(.top, 20)
                outlinedField(
                    icon: "person.text.rectangle",
                    label: "Preferred Mingle Display Name *",
                    text: $myName,
                    placeholder: "e.g. Alex, Sam, Joy",
                    error: nameError ? "Mingle display name is required" : nil,
                    help: "The display name shown on your Mingle profile"
                )
                .padding(.top, 12)
                outlinedField(icon: "birthday.cake", label: "Age", text: ageBinding, keyboard: .numberPad)
                    .padding(.top, 12)
                outlinedField(icon: "text.alignleft", label: "About Me / Bio", text: $myBio, lineLimit: 4)
                    .padding(.top, 12)

                Text("Gender")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.secondary)
                    .padding(.top, 16)
                chipRow(["Male", "Female", "Non-binary", "Other"], selected: genderSelected) { chip in
                    myGender = Self.storedGender(chip)
                }
                .padding(.top, 6)

                Text("Looking For")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.secondary)
                    .padding(.top, 12)
                chipRow(["Men", "Women", "Non-binary", "Everyone"], selected: lookingSelected) { chip in
                    myLookingFor = Self.storedLooking(chip)
                }
                .padding(.top, 6)

                Text("Target Age Range (18+)")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.secondary)
                    .padding(.top, 14)
                Menu {
                    ForEach(ageRanges, id: \.self) { option in
                        Button(option) { myTargetAgeRange = option.hasPrefix("18+") ? "18 - 120" : option }
                    }
                } label: {
                    HStack {
                        Image(systemName: "calendar")
                            .foregroundColor(.secondary)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Target Age Range")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Text(ageRangeLabel)
                                .foregroundColor(.primary)
                        }
                        Spacer()
                        Image(systemName: "chevron.down")
                            .foregroundColor(.secondary)
                    }
                    .padding(12)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.gray.opacity(0.35), lineWidth: 1))
                }
                .padding(.top, 6)

                Button(action: save) {
                    HStack(spacing: 8) {
                        Image(systemName: "checkmark")
                        Text("Save & Update Profile")
                            .font(.system(size: 15, weight: .bold))
                    }
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(Color.appPrimary)
                    .clipShape(Capsule())
                }
                .padding(.top, 24)
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 24)
        }
        .onChange(of: pickerItems) { items in
            Task {
                for item in items {
                    if let data = try? await item.loadTransferable(type: Data.self) {
                        onPhoto(data)
                    }
                }
                pickerItems = []
            }
        }
    }

    private var galleryButton: some View {
        PhotosPicker(selection: $pickerItems, maxSelectionCount: 1, matching: .images) {
            HStack(spacing: 8) {
                Image(systemName: "photo.on.rectangle")
                Text("Select Photos from Gallery")
            }
            .font(.system(size: 15, weight: .semibold))
            .foregroundColor(.appPrimary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
            .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.appPrimary, lineWidth: 1))
        }
    }

    private var ageBinding: Binding<String> {
        Binding(get: { myAge }, set: { myAge = String($0.filter(\.isNumber).prefix(3)) })
    }

    private var ageRangeLabel: String {
        if myTargetAgeRange == "18 - 120" || myTargetAgeRange.hasPrefix("18+") {
            return "18+ (All ages 18+)"
        }
        return myTargetAgeRange.isEmpty ? "18 - 35" : myTargetAgeRange
    }

    private func save() {
        let name = myName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else {
            nameError = true
            return
        }
        nameError = false
        onSave()
    }

    private func photoSlot(_ index: Int) -> some View {
        ZStack(alignment: .topTrailing) {
            PhotosPicker(selection: $pickerItems, maxSelectionCount: 1, matching: .images) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(Color(UIColor.secondarySystemBackground))
                    if photos.indices.contains(index), let url = FeedPost.resolve(photos[index]) {
                        AsyncImage(url: url) { image in
                            image.resizable().scaledToFill()
                        } placeholder: {
                            Color(UIColor.secondarySystemBackground)
                        }
                    } else {
                        VStack(spacing: 4) {
                            Image(systemName: "plus")
                                .foregroundColor(.appPrimary)
                            Text("\(index + 1)")
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .frame(width: 90, height: 120)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.gray.opacity(0.3), lineWidth: 1))
            }
            if photoIds.indices.contains(index), !photoIds[index].isEmpty, photos.indices.contains(index) {
                Button(action: { onDelete(photoIds[index]) }) {
                    Image(systemName: "xmark")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 22, height: 22)
                        .background(Color.black.opacity(0.6))
                        .clipShape(Circle())
                }
                .padding(4)
            }
        }
    }

    private func chipRow(_ chips: [String], selected: @escaping (String) -> Bool, onSelect: @escaping (String) -> Void) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(chips, id: \.self) { chip in
                    Button(action: { onSelect(chip) }) {
                        Text(chip)
                            .font(.system(size: 12, weight: .semibold))
                            .lineLimit(1)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .foregroundColor(selected(chip) ? .white : .primary)
                            .background(selected(chip) ? Color.appPrimary : Color(UIColor.secondarySystemBackground))
                            .clipShape(Capsule())
                    }
                }
            }
        }
    }

    private func genderSelected(_ chip: String) -> Bool {
        let value = myGender.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        switch chip {
        case "Male": return value == "male" || value == "man"
        case "Female": return value == "female" || value == "woman"
        case "Non-binary": return value == "nonbinary" || value == "non-binary"
        default: return value.isEmpty || value == "other"
        }
    }

    private func lookingSelected(_ chip: String) -> Bool {
        let value = myLookingFor.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        switch chip {
        case "Men": return value == "men" || value == "male" || value == "man"
        case "Women": return value == "women" || value == "female" || value == "woman"
        case "Non-binary": return value == "nonbinary" || value == "non-binary"
        default: return value.isEmpty || value == "everyone" || value == "all"
        }
    }

    private static func storedGender(_ chip: String) -> String {
        switch chip {
        case "Male": return "male"
        case "Female": return "female"
        case "Non-binary": return "nonbinary"
        default: return "other"
        }
    }

    private static func storedLooking(_ chip: String) -> String {
        switch chip {
        case "Men": return "male"
        case "Women": return "female"
        case "Non-binary": return "nonbinary"
        default: return "everyone"
        }
    }

    private func outlinedField(
        icon: String,
        label: String,
        text: Binding<String>,
        placeholder: String = "",
        keyboard: UIKeyboardType = .default,
        lineLimit: Int = 1,
        error: String? = nil,
        help: String? = nil
    ) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .top, spacing: 8) {
                Image(systemName: icon)
                    .foregroundColor(.secondary)
                    .padding(.top, 2)
                VStack(alignment: .leading, spacing: 2) {
                    Text(label)
                        .font(.caption)
                        .foregroundColor(error == nil ? .secondary : .red)
                    TextField(placeholder, text: text, axis: lineLimit > 1 ? .vertical : .horizontal)
                        .lineLimit(lineLimit, reservesSpace: lineLimit > 1)
                        .keyboardType(keyboard)
                }
            }
            .padding(12)
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(error == nil ? Color.gray.opacity(0.35) : Color.red, lineWidth: 1))
            if let error {
                Text(error).font(.caption).foregroundColor(.red)
            } else if let help {
                Text(help).font(.caption).foregroundColor(.secondary)
            }
        }
    }
}

struct MingleInboxSheet: View {
    var onOpenChat: (String, String) -> Void
    @State private var chats: [[String: Any]] = []
    @State private var requests: [[String: Any]] = []
    @State private var query = ""
    @State private var showRequests = false
    @State private var note = ""

    var body: some View {
        NavigationStack {
            List {
                if filtered.isEmpty {
                    VStack(spacing: 8) {
                        Text(query.isEmpty ? "No Mingle Secret Chats Yet" : "No matching secret chats")
                            .font(.headline)
                        Text("Private secret chats with your mutual matches")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
                Section("Matches") {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack {
                            ForEach(filtered.indices, id: \.self) { index in
                                let chat = filtered[index]
                                Button(action: { open(chat) }) {
                                    VStack {
                                        Text(String(name(chat).prefix(1)))
                                            .frame(width: 52, height: 52)
                                            .background(Color.appPrimary.opacity(0.15))
                                            .clipShape(Circle())
                                        Text(name(chat)).font(.caption).lineLimit(1)
                                    }
                                    .frame(width: 72)
                                }
                            }
                        }
                    }
                }
                Button(action: { showRequests = true }) {
                    HStack {
                        Text("Message Requests (\(requests.count))")
                        Spacer()
                        Image(systemName: "chevron.right")
                    }
                }
                Section("Chats") {
                    ForEach(filtered.indices, id: \.self) { index in
                        let chat = filtered[index]
                        Button(action: { open(chat) }) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(name(chat)).font(.headline)
                                Text("Encrypted").font(.caption).foregroundColor(.secondary)
                                Text(LiveJSON.string(chat["last_message"])).font(.subheadline).lineLimit(1)
                            }
                        }
                    }
                }
            }
            .searchable(text: $query, prompt: "Search Mingle secret chats...")
            .navigationTitle("Mingle Secret Chats")
            .navigationBarTitleDisplayMode(.inline)
            .task { await load() }
            .sheet(isPresented: $showRequests) {
                MingleRequestsSheet(onOpenChat: onOpenChat)
            }
            .overlay(alignment: .bottom) {
                if !note.isEmpty {
                    Text(note).font(.caption).padding()
                }
            }
        }
    }

    private var filtered: [[String: Any]] {
        let needle = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !needle.isEmpty else { return chats }
        return chats.filter { name($0).lowercased().contains(needle) || LiveJSON.string($0["last_message"]).lowercased().contains(needle) }
    }

    private func name(_ chat: [String: Any]) -> String {
        let candidates = [
            LiveJSON.string(chat["display_name"]),
            LiveJSON.string(chat["contact_name"]),
            LiveJSON.string(chat["username"]),
            LiveJSON.string(chat["title"]),
            LiveJSON.string(chat["sender_name"])
        ]
        return candidates.compactMap { value in
            let cleaned = MingleCopy.realName(value)
            return cleaned.isEmpty ? nil : cleaned
        }.first ?? ""
    }

    private func open(_ chat: [String: Any]) {
        let conversation = LiveJSON.string(chat["conversation_id"])
        let id = conversation.isEmpty ? LiveJSON.string(chat["id"]) : conversation
        guard !id.isEmpty else { return }
        onOpenChat(id, name(chat))
    }

    private func load() async {
        do {
            let data = try await API.data("conversations")
            chats = LiveJSON.list(data, keys: ["chats", "conversations"]).filter { chat in
                let rawType = LiveJSON.string(chat["type"]).lowercased()
                let id = LiveJSON.string(chat["conversation_id"]).nilIfEmpty ?? LiveJSON.string(chat["id"])
                return (rawType == "mingle" || id.hasPrefix("mingle_")) && !name(chat).isEmpty
            }
            let intros = try await API.data("mingle/intros")
            requests = LiveJSON.list(intros, keys: ["items", "intros"])
        } catch {
            note = MingleCopy.serverMessage(error)
        }
    }
}
