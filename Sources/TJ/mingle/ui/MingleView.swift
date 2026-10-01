import SwiftUI
import CoreLocation
import PhotosUI

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
    @State private var userCoins: Int = 250

    // Profile Edit States
    @State private var myName: String = "Tshidiso Khoza"
    @State private var myAge: String = "28"
    @State private var myBio: String = "Building apps and sharing music!"
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

                    Text("Nearby people are in your contacts")
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
                        onPass: {
                            withAnimation { profiles.removeFirst() }
                        },
                        onLike: {
                            withAnimation { profiles.removeFirst() }
                        },
                        onChat: {
                            introTarget = candidate
                        }
                    )
                }
            }
        }
        .task { await loadFeed() }
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
                onSave: { showProfileSheet = false }
            )
            .presentationDetents([.large])
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
                    let text = introText.trimmingCharacters(in: .whitespaces)
                    introTarget = nil
                    introText = ""
                    if !text.isEmpty {
                        onMatchClick?(target.id, target.name)
                    }
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
            let data = try await API.data("mingle/feed", query: ["radius_km": "\(Int(selectedRadiusKm))"])
            struct Feed: Decodable {
                struct Item: Decodable {
                    let id: String
                    let name: String?
                    let age: Int?
                    let bio: String?
                    let distanceKm: Double?
                    enum CodingKeys: String, CodingKey {
                        case id, name, age, bio
                        case distanceKm = "distance_km"
                    }
                }
                let items: [Item]?
                let balance: Double?
                let status: String?
            }
            let feed = try JSONDecoder().decode(Feed.self, from: data)
            if let balance = feed.balance { userCoins = Int(balance) }
            let feedItems = feed.items ?? []
            var resultProfiles: [MingleProfile] = []
            for item in feedItems {
                let nameStr = (item.name?.isEmpty == false) ? item.name! : "Nearby"
                let ageVal = item.age ?? 0
                let distVal = item.distanceKm ?? 0.0
                let bioStr = item.bio ?? ""
                let p = MingleProfile(
                    id: item.id,
                    name: nameStr,
                    age: ageVal,
                    occupation: "",
                    distanceKm: distVal,
                    bio: bioStr,
                    interests: [],
                    avatarGradient: [.purple, .blue]
                )
                resultProfiles.append(p)
            }
            profiles = resultProfiles
            if profiles.isEmpty {
                feedNote = feed.status?.isEmpty == false ? (feed.status ?? "") : "Nearby people are in your contacts"
            }
        } catch {
            feedNote = "Nearby people are in your contacts"
        }
    }

    private func rewindLastSwipe() {
        // Rewind swipe logic
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

                Image(systemName: "person.fill")
                    .font(.system(size: 100))
                    .foregroundColor(.white.opacity(0.3))
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
