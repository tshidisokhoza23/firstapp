import SwiftUI
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
}

struct CourtListSheet: View {
    let title: String
    let path: String
    @State private var rows: [(id: String, title: String, subtitle: String)] = []
    @State private var note = "Loading..."

    var body: some View {
        NavigationStack {
            List(rows, id: \.id) { row in
                VStack(alignment: .leading, spacing: 4) {
                    Text(row.title).font(.headline)
                    if !row.subtitle.isEmpty {
                        Text(row.subtitle).font(.caption).foregroundColor(.secondary)
                    }
                }
            }
            .overlay { if rows.isEmpty { Text(note).foregroundColor(.secondary) } }
            .navigationTitle(title)
            .task { await load() }
        }
    }

    private func load() async {
        do {
            let data = try await API.data(path)
            let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
            if path == "mingle/activity" {
                let items = json?["items"] as? [[String: Any]] ?? []
                rows = items.compactMap { item in
                    let id = item["id"] as? String ?? UUID().uuidString
                    let name = item["actor_name"] as? String ?? ""
                    let type = item["type"] as? String ?? ""
                    return (id, name, type)
                }
            } else {
                let chats = LiveJSON.chats(from: data).filter { $0.type == .mingle || $0.type.rawValue.lowercased() == "mingle" }
                rows = chats.map { ($0.id, $0.consistentName(), $0.lastMessage) }
            }
            note = rows.isEmpty ? "Nothing here yet" : ""
        } catch {
            note = "Could not load"
        }
    }
}

// MARK: - Mingle Models

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

// MARK: - MingleView matching Screenshot 5

public struct MingleView: View {
    @State private var profiles: [MingleProfile] = []
    @State private var feedNote = "Nearby people are in your contacts"
    @State private var selectedRadiusKm: Double = 100.0
    @State private var showRadiusSheet: Bool = false
    @State private var showProfileSheet: Bool = false
    @State private var showSecretChats: Bool = false
    @State private var showNotifications: Bool = false
    @State private var introTarget: MingleProfile? = nil
    @State private var introText: String = ""
    @State private var userCoins: Int = 0
    @State private var minBalance: Int = 40
    @State private var lastSwiped: MingleProfile?

    // Profile Edit States
    @State private var myName: String = ""
    @State private var myAge: String = ""
    @State private var myBio: String = ""
    @State private var photoItems: [PhotosPickerItem] = []

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
            // Top Controls Bar matching Screenshot 5
            HStack {
                // Secret Chats Envelope
                Button(action: { showSecretChats = true }) {
                    Image(systemName: "envelope")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.primary)
                }

                Spacer().frame(width: 16)

                // Notifications Bell
                Button(action: { showNotifications = true }) {
                    Image(systemName: "bell")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.primary)
                }

                Spacer().frame(width: 16)

                // Rewind Swipe Icon
                Button(action: rewindLastSwipe) {
                    Image(systemName: "arrow.counterclockwise")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.appPrimary)
                }

                Spacer()

                // Center TJ Logo
                AppLogo(size: 32)

                Spacer()

                // Radius Filter Pill matching Screenshot 5 (📍 100+ km)
                Button(action: { showRadiusSheet = true }) {
                    HStack(spacing: 4) {
                        Image(systemName: "location.fill")
                            .font(.system(size: 12))
                            .foregroundColor(.appPrimary)

                        Text(selectedRadiusKm >= 100 ? "100+ km" : "\(Int(selectedRadiusKm)) km")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(.primary)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Color(UIColor.secondarySystemBackground))
                    .clipShape(Capsule())
                }

                Spacer().frame(width: 12)

                // My Profile Camera Icon matching Screenshot 5
                Button(action: { showProfileSheet = true }) {
                    Image(systemName: "camera.fill")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.appPrimary)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)

            Divider()

            // Main Display Area
            if profiles.isEmpty {
                // Empty Deck State matching Screenshot 5
                Spacer()

                VStack(spacing: 16) {
                    ZStack {
                        Circle()
                            .fill(Color.appPrimary.opacity(0.12))
                            .frame(width: 90, height: 90)

                        Image(systemName: "location.slash.fill")
                            .font(.system(size: 42))
                            .foregroundColor(.appPrimary)
                    }

                    Text(feedNote)
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.primary)
                        .multilineTextAlignment(.center)

                    Text("Mingle hides people saved in your contacts, including matching phone numbers.")
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 40)

                    Spacer().frame(height: 12)

                    Button(action: { showRadiusSheet = true }) {
                        Text("Increase Radius")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 12)
                            .background(Color.appPrimary)
                            .clipShape(Capsule())
                    }
                }

                Spacer()
            } else {
                // Single Candidate Card View matching Screenshot 5
                if let candidate = profiles.first {
                    CandidateCardView(
                        profile: candidate,
                        onPass: { Task { await swipe(candidate, action: "pass") } },
                        onLike: { Task { await swipe(candidate, action: "like") } },
                        onChat: {
                            introTarget = candidate
                        }
                    )
                }
            }
        }
        .task {
            await loadMe()
            await loadFeed()
        }
        .sheet(isPresented: $showRadiusSheet) {
            RadiusFilterSheetView(
                radiusKm: $selectedRadiusKm,
                onApply: {
                    showRadiusSheet = false
                    Task { await loadFeed() }
                }
            )
            .presentationDetents([.height(280)])
        }
        .sheet(isPresented: $showProfileSheet) {
            MingleProfileEditSheetView(
                myName: $myName,
                myAge: $myAge,
                myBio: $myBio,
                onSave: {
                    showProfileSheet = false
                    Task { await saveMe() }
                }
            )
            .presentationDetents([.large])
        }
        .sheet(isPresented: $showSecretChats) {
            CourtListSheet(title: "Mingle chats", path: "conversations")
        }
        .sheet(isPresented: $showNotifications) {
            CourtListSheet(title: "Activity", path: "mingle/activity")
        }
        .sheet(item: $introTarget) { target in
            VStack(spacing: 16) {
                Text("Say hi to \(target.name)")
                    .font(.system(size: 18, weight: .bold))
                    .padding(.top, 16)

                Text("Send a short message (up to 150 characters) to start a chat.")
                    .font(.system(size: 13))
                    .foregroundColor(.secondary)

                TextEditor(text: $introText)
                    .frame(height: 100)
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.gray.opacity(0.3), lineWidth: 1))
                    .padding(.horizontal, 24)

                Button(action: {
                    let text = introText.trimmingCharacters(in: .whitespacesAndNewlines)
                    let recipient = target.id
                    let name = target.name
                    introTarget = nil
                    introText = ""
                    guard !text.isEmpty else { return }
                    Task { await sendIntro(recipientId: recipient, name: name, message: text) }
                }) {
                    Text("Send Message")
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
    }

    private func loadFeed() async {
        do {
            guard let loc = await CourtLocation.shared.current() else {
                feedNote = "Location is required to load Court"
                profiles = []
                return
            }
            let lat = loc.coordinate.latitude
            let lng = loc.coordinate.longitude
            guard lat != 0 || lng != 0 else {
                feedNote = "Location is required to load Court"
                profiles = []
                return
            }
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
                "offset": "0"
            ])
            struct Feed: Decodable {
                struct Photo: Decodable { let url: String? }
                struct Item: Decodable {
                    let id: String
                    let name: String?
                    let age: Int?
                    let bio: String?
                    let distanceKm: Double?
                    let photos: [Photo]?
                    enum CodingKeys: String, CodingKey {
                        case id, name, age, bio, photos
                        case distanceKm = "distance_km"
                    }
                }
                let items: [Item]?
                let balance: Double?
                let status: String?
                let minBalance: Double?
                enum CodingKeys: String, CodingKey {
                    case items, balance, status
                    case minBalance = "min_balance"
                }
            }
            let feed = try JSONDecoder().decode(Feed.self, from: data)
            if let balance = feed.balance { userCoins = Int(balance) }
            if let minBalance = feed.minBalance { self.minBalance = Int(minBalance) }
            let feedItems = feed.items ?? []
            var resultProfiles: [MingleProfile] = []
            for item in feedItems {
                guard !item.id.isEmpty else { continue }
                let nameStr = item.name?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                let photos = (item.photos ?? []).compactMap(\.url).filter { !$0.isEmpty }
                resultProfiles.append(MingleProfile(
                    id: item.id,
                    name: nameStr,
                    age: item.age ?? 0,
                    occupation: "",
                    distanceKm: item.distanceKm ?? 0,
                    bio: item.bio ?? "",
                    interests: [],
                    avatarGradient: [.purple, .blue],
                    photos: photos
                ))
            }
            profiles = resultProfiles
            if profiles.isEmpty {
                feedNote = Self.courtStatus(feed.status)
            }
        } catch {
            feedNote = "Could not load people nearby"
        }
    }

    private func saveMe() async {
        var body: [String: Any] = ["name": myName, "bio": myBio]
        if let age = Int(myAge), age > 0 { body["age"] = age }
        _ = try? await API.data("mingle/me", method: "PUT", json: body)
    }

    private func loadMe() async {
        guard let data = try? await API.data("mingle/me"),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let profile = json["profile"] as? [String: Any] else { return }
        if let name = profile["name"] as? String, !name.isEmpty { myName = name }
        if let bio = profile["bio"] as? String { myBio = bio }
        if let age = profile["age"] as? Int, age > 0 { myAge = "\(age)" }
        else if let age = profile["age"] as? Double, age > 0 { myAge = "\(Int(age))" }
    }

    private func swipe(_ profile: MingleProfile, action: String) async {
        if action == "like", userCoins < minBalance {
            feedNote = "You need at least \(minBalance) points to like"
            return
        }
        do {
            let data = try await API.data("mingle/swipe", method: "POST", json: [
                "target_user_id": profile.id,
                "action": action
            ])
            let json = (try? JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
            if let balance = json["balance"] as? Double { userCoins = Int(balance) }
            else if let balance = json["balance"] as? Int { userCoins = balance }
            lastSwiped = profile
            profiles.removeAll { $0.id == profile.id }
            if json["matched"] as? Bool == true, let conversationId = json["conversation_id"] as? String, !conversationId.isEmpty {
                onMatchClick?(conversationId, profile.name)
            }
        } catch {
            feedNote = "Swipe was not saved"
        }
    }

    private func sendIntro(recipientId: String, name: String, message: String) async {
        do {
            let data = try await API.data("mingle/intro", method: "POST", json: [
                "recipient_id": recipientId,
                "message": String(message.prefix(150))
            ])
            let json = (try? JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
            if let balance = json["balance"] as? Double { userCoins = Int(balance) }
            if let conversationId = json["conversation_id"] as? String, !conversationId.isEmpty {
                onMatchClick?(conversationId, name)
            }
        } catch {
            feedNote = "Message was not sent"
        }
    }

    private func rewindLastSwipe() {
        Task {
            guard let data = try? await API.data("mingle/rewind", method: "POST"),
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  json["rewound"] as? Bool == true else { return }
            if let balance = json["balance"] as? Double { userCoins = Int(balance) }
            await loadFeed()
        }
    }

    private static func courtStatus(_ raw: String?) -> String {
        switch raw?.uppercased() {
        case "CONTACTS_HIDDEN": return "Nearby people are in your contacts"
        case "OUT_OF_RADIUS": return "No one in this radius"
        case "NO_PHOTO": return "People nearby have no photo"
        case "DIFFERENT_COUNTRY": return "No one in your country"
        case "ALREADY_SWIPED": return "You have seen everyone nearby"
        case "PREFERENCES": return "No one matches your preferences"
        case "NO_CANDIDATE_LOCATION": return "No one nearby has a location"
        case "FEED_EXHAUSTED": return "No more people nearby"
        default: return raw?.isEmpty == false ? (raw ?? "") : "No one nearby"
        }
    }
}

// MARK: - Candidate Card View matching Screenshot 5

public struct CandidateCardView: View {
    public let profile: MingleProfile
    public var onPass: () -> Void
    public var onLike: () -> Void
    public var onChat: () -> Void

    public var body: some View {
        ZStack(alignment: .bottom) {
            // Main Photo
            ZStack {
                LinearGradient(colors: profile.avatarGradient, startPoint: .topLeading, endPoint: .bottomTrailing)
                if let url = FeedPost.resolve(profile.photos.first) {
                    AsyncImage(url: url) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Color.clear
                    }
                } else {
                    Image(systemName: "person.fill")
                        .font(.system(size: 100))
                        .foregroundColor(.white.opacity(0.3))
                }
            }
            .ignoresSafeArea()

            // Bottom Gradient Overlay
            LinearGradient(
                colors: [.clear, .black.opacity(0.85)],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 280)

            // Details & Action Bar Overlay
            VStack(alignment: .leading, spacing: 12) {
                // Name & Age
                Text("\(profile.name), \(profile.age)")
                    .font(.system(size: 28, weight: .black))
                    .foregroundColor(.white)

                // Distance
                HStack(spacing: 4) {
                    Image(systemName: "location.fill")
                        .font(.system(size: 13))
                    Text("\(String(format: "%.1f", profile.distanceKm)) km away")
                        .font(.system(size: 14, weight: .semibold))
                }
                .foregroundColor(.white.opacity(0.9))

                if !profile.bio.isEmpty {
                    Text(profile.bio)
                        .font(.system(size: 14))
                        .foregroundColor(.white.opacity(0.85))
                        .lineLimit(2)
                }

                Spacer().frame(height: 12)

                // Action Bar matching Screenshot 5 (Pass, Like, Meet Chat)
                HStack(spacing: 24) {
                    // Pass (X)
                    Button(action: onPass) {
                        ZStack {
                            Circle()
                                .fill(Color.white)
                                .frame(width: 54, height: 54)
                                .shadow(radius: 4)

                            Image(systemName: "xmark")
                                .font(.system(size: 22, weight: .bold))
                                .foregroundColor(.black)
                        }
                    }

                    // Like (Heart)
                    Button(action: onLike) {
                        ZStack {
                            Circle()
                                .fill(Color(red: 0.91, green: 0.12, blue: 0.39)) // #E91E63
                                .frame(width: 66, height: 66)
                                .shadow(color: Color.red.opacity(0.4), radius: 6, x: 0, y: 3)

                            Image(systemName: "heart.fill")
                                .font(.system(size: 28, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }

                    // Meet Chat (Bubble)
                    Button(action: onChat) {
                        ZStack {
                            Circle()
                                .fill(Color.appPrimary)
                                .frame(width: 54, height: 54)
                                .shadow(radius: 4)

                            Image(systemName: "message.fill")
                                .font(.system(size: 22, weight: .bold))
                                .foregroundColor(.white)
                        }
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.bottom, 24)
            }
            .padding(.horizontal, 24)
        }
    }
}

// MARK: - Radius Filter Sheet View matching Screenshot 5

public struct RadiusFilterSheetView: View {
    @Binding public var radiusKm: Double
    public var onApply: () -> Void

    public var body: some View {
        VStack(spacing: 20) {
            Text("Discovery Radius")
                .font(.system(size: 20, weight: .bold))
                .padding(.top, 20)

            VStack(spacing: 8) {
                Text("\(Int(radiusKm)) km")
                    .font(.system(size: 24, weight: .black))
                    .foregroundColor(.appPrimary)

                Slider(value: $radiusKm, in: 1...100, step: 1)
                    .accentColor(.appPrimary)
                    .padding(.horizontal, 24)
            }

            Button(action: onApply) {
                Text("Apply Filter")
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
}

// MARK: - Mingle Profile Edit Sheet View matching Screenshot 5

public struct MingleProfileEditSheetView: View {
    @Binding public var myName: String
    @Binding public var myAge: String
    @Binding public var myBio: String
    public var onSave: () -> Void

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    Text("My Mingle Profile")
                        .font(.system(size: 20, weight: .bold))
                        .padding(.top, 16)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Display Name")
                            .font(.system(size: 14, weight: .bold))
                        TextField("Name", text: $myName)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Age")
                            .font(.system(size: 14, weight: .bold))
                        TextField("Age", text: $myAge)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text("About Me / Bio")
                            .font(.system(size: 14, weight: .bold))
                        TextEditor(text: $myBio)
                            .frame(height: 80)
                            .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.gray.opacity(0.3), lineWidth: 1))
                    }

                    Button(action: onSave) {
                        Text("Save Profile")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(Color.appPrimary)
                            .cornerRadius(12)
                    }

                    Spacer()
                }
                .padding(.horizontal, 24)
            }
        }
    }
}
