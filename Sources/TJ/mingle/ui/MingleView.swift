import SwiftUI

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
    public let mutualConnections: Int

    public init(id: String, name: String, age: Int, occupation: String, distanceKm: Double, bio: String, interests: [String], avatarGradient: [Color], mutualConnections: Int = 0) {
        self.id = id
        self.name = name
        self.age = age
        self.occupation = occupation
        self.distanceKm = distanceKm
        self.bio = bio
        self.interests = interests
        self.avatarGradient = avatarGradient
        self.mutualConnections = mutualConnections
    }
}

// MARK: - MingleView

public struct MingleView: View {
    @State private var viewMode: MingleViewMode = .cards
    @State private var showFilterSheet: Bool = false
    @State private var showCoinsGateModal: Bool = false
    @State private var selectedProfile: MingleProfile? = nil

    // Filters
    @State private var maxDistanceKm: Double = 25.0
    @State private var selectedInterest: String = "All"

    // User Balance
    @State private var userCoins: Int = 45

    enum MingleViewMode: String, CaseIterable, Identifiable {
        case cards = "Match Cards"
        case grid = "Discover Grid"

        var id: String { rawValue }
    }

    private let availableInterests = ["All", "Coding", "Music", "Startups", "Gaming", "Travel", "Design", "Fitness"]

    @State private var profiles: [MingleProfile] = [
        MingleProfile(id: "p1", name: "Sophia Lin", age: 25, occupation: "iOS Developer @ TechCorp", distanceKm: 3.2, bio: "Swift & SwiftUI enthusiast. Always looking for coffee & open source collaborators! ☕💻", interests: ["Coding", "Music", "Coffee"], avatarGradient: [.purple, .pink], mutualConnections: 4),
        MingleProfile(id: "p2", name: "Liam Chen", age: 28, occupation: "Product Designer", distanceKm: 7.5, bio: "Designing dark mode UIs and playing bass guitar in a synthwave band 🎸✨", interests: ["Design", "Music", "Gaming"], avatarGradient: [.blue, .cyan], mutualConnections: 2),
        MingleProfile(id: "p3", name: "Emma Watson", age: 24, occupation: "AI Researcher", distanceKm: 12.0, bio: "Exploring LLMs & On-device ML. Let's chat tech and innovation 🚀", interests: ["Startups", "Coding", "Travel"], avatarGradient: [.orange, .red], mutualConnections: 8),
        MingleProfile(id: "p4", name: "Noah Miller", age: 27, occupation: "Fitness Coach & Vlogger", distanceKm: 18.4, bio: "Marathon runner, healthy smoothie maker and travel enthusiast 🏃‍♂️🌴", interests: ["Fitness", "Travel"], avatarGradient: [.green, .teal], mutualConnections: 1)
    ]

    public init() {}

    public var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Top Filter & Mode Header
                HStack {
                    Picker("View Mode", selection: $viewMode) {
                        ForEach(MingleViewMode.allCases) { mode in
                            Text(mode.rawValue).tag(mode)
                        }
                    }
                    .pickerStyle(.segmented)

                    Button(action: { showFilterSheet = true }) {
                        Image(systemName: "line.3.horizontal.decrease.circle")
                            .font(.title2)
                            .foregroundColor(.blue)
                    }
                }
                .padding(.horizontal)
                .padding(.top, 8)

                // Coins Header Bar
                HStack {
                    HStack(spacing: 4) {
                        Image(systemName: "sparkles")
                            .foregroundColor(.yellow)
                        Text("\(userCoins) Mingle Coins")
                            .font(.caption.bold())
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Color.yellow.opacity(0.15))
                    .cornerRadius(12)

                    Spacer()

                    Text("Nearby Discovery")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal)
                .padding(.vertical, 8)

                // Main Display
                if profiles.isEmpty {
                    Spacer()
                    VStack(spacing: 12) {
                        Image(systemName: "person.2.slash")
                            .font(.system(size: 56))
                            .foregroundColor(.secondary)
                        Text("No more profiles nearby")
                            .font(.title3.bold())
                        Text("Try widening your distance filter or changing interest preferences.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)
                    }
                    Spacer()
                } else if viewMode == .cards {
                    cardSwipeView
                } else {
                    gridView
                }
            }
            .navigationTitle("Mingle Social")
            .sheet(isPresented: $showFilterSheet) {
                filterSheetView
            }
            .sheet(isPresented: $showCoinsGateModal) {
                coinsGateSheetView
            }
            .sheet(item: $selectedProfile) { profile in
                profileDetailModal(profile)
            }
        }
    }

    // MARK: - Subviews

    private var cardSwipeView: some View {
        VStack(spacing: 16) {
            Spacer()

            if let current = profiles.first {
                VStack(alignment: .leading, spacing: 14) {
                    ZStack(alignment: .bottomLeading) {
                        RoundedRectangle(cornerRadius: 24)
                            .fill(LinearGradient(colors: current.avatarGradient, startPoint: .topLeading, endPoint: .bottomTrailing))
                            .frame(height: 380)

                        Image(systemName: "person.fill")
                            .font(.system(size: 120))
                            .foregroundColor(.white.opacity(0.3))
                            .frame(maxWidth: .infinity, maxHeight: .infinity)

                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Text("\(current.name), \(current.age)")
                                    .font(.title.bold())
                                    .foregroundColor(.white)

                                Circle()
                                    .fill(Color.green)
                                    .frame(width: 10, height: 10)
                            }

                            Text(current.occupation)
                                .font(.subheadline)
                                .foregroundColor(.white.opacity(0.9))

                            HStack(spacing: 4) {
                                Image(systemName: "location.fill")
                                Text("\(String(format: "%.1f", current.distanceKm)) km away")
                            }
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.8))
                        }
                        .padding(20)
                    }
                    .onTapGesture {
                        selectedProfile = current
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text(current.bio)
                            .font(.subheadline)
                            .lineLimit(2)

                        HStack {
                            ForEach(current.interests, id: \.self) { interest in
                                Text("#\(interest)")
                                    .font(.caption.bold())
                                    .foregroundColor(.blue)
                                    .padding(.horizontal, 8)
                                    .padding(.vertical, 4)
                                    .background(Color.blue.opacity(0.12))
                                    .cornerRadius(8)
                            }
                        }
                    }
                    .padding(.horizontal, 8)
                }
                .padding()
                .background(Color(.secondarySystemGroupedBackground))
                .cornerRadius(24)
                .padding(.horizontal, 20)
            }

            Spacer()

            // Action Buttons
            HStack(spacing: 24) {
                // Pass (X)
                Button(action: {
                    if !profiles.isEmpty { profiles.removeFirst() }
                }) {
                    Image(systemName: "xmark")
                        .font(.title.bold())
                        .foregroundColor(.red)
                        .frame(width: 60, height: 60)
                        .background(Color.red.opacity(0.12))
                        .clipShape(Circle())
                }

                // Super Like (Star)
                Button(action: {
                    showCoinsGateModal = true
                }) {
                    Image(systemName: "star.fill")
                        .font(.title2.bold())
                        .foregroundColor(.yellow)
                        .frame(width: 50, height: 50)
                        .background(Color.yellow.opacity(0.15))
                        .clipShape(Circle())
                }

                // Like (Heart)
                Button(action: {
                    if !profiles.isEmpty { profiles.removeFirst() }
                }) {
                    Image(systemName: "heart.fill")
                        .font(.title.bold())
                        .foregroundColor(.green)
                        .frame(width: 60, height: 60)
                        .background(Color.green.opacity(0.12))
                        .clipShape(Circle())
                }
            }
            .padding(.bottom, 20)
        }
    }

    private var gridView: some View {
        ScrollView {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                ForEach(profiles) { profile in
                    VStack(alignment: .leading, spacing: 8) {
                        ZStack(alignment: .topTrailing) {
                            RoundedRectangle(cornerRadius: 16)
                                .fill(LinearGradient(colors: profile.avatarGradient, startPoint: .topLeading, endPoint: .bottomTrailing))
                                .frame(height: 160)
                                .overlay(
                                    Image(systemName: "person.fill")
                                        .font(.system(size: 60))
                                        .foregroundColor(.white.opacity(0.3))
                                )

                            Text("\(String(format: "%.1f", profile.distanceKm)) km")
                                .font(.caption2.bold())
                                .foregroundColor(.white)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.black.opacity(0.5))
                                .cornerRadius(8)
                                .padding(8)
                        }

                        Text("\(profile.name), \(profile.age)")
                            .font(.headline)

                        Text(profile.occupation)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }
                    .padding(8)
                    .background(Color(.secondarySystemGroupedBackground))
                    .cornerRadius(16)
                    .onTapGesture {
                        selectedProfile = profile
                    }
                }
            }
            .padding(.horizontal)
            .padding(.top, 12)
        }
    }

    // MARK: - Sheets & Modals

    private var filterSheetView: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 20) {
                Text("Discovery Filters")
                    .font(.title2.bold())

                VStack(alignment: .leading, spacing: 8) {
                    Text("Maximum Distance: \(Int(maxDistanceKm)) km")
                        .font(.subheadline.bold())

                    Slider(value: $maxDistanceKm, in: 5...100, step: 5)
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text("Interest Tag")
                        .font(.subheadline.bold())

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(availableInterests, id: \.self) { tag in
                                Button(action: { selectedInterest = tag }) {
                                    Text(tag)
                                        .font(.caption.bold())
                                        .padding(.horizontal, 14)
                                        .padding(.vertical, 8)
                                        .background(selectedInterest == tag ? Color.blue : Color(.secondarySystemBackground))
                                        .foregroundColor(selectedInterest == tag ? .white : .primary)
                                        .cornerRadius(12)
                                }
                            }
                        }
                    }
                }

                Spacer()

                Button("Apply Filters") {
                    showFilterSheet = false
                }
                .fontWeight(.bold)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color.blue)
                .cornerRadius(12)
            }
            .padding()
            .navigationBarItems(trailing: Button("Reset") {
                maxDistanceKm = 25.0
                selectedInterest = "All"
            })
        }
        .presentationDetents([.height(340)])
    }

    private var coinsGateSheetView: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Image(systemName: "star.circle.fill")
                    .font(.system(size: 64))
                    .foregroundColor(.yellow)

                Text("Send Super Like?")
                    .font(.title2.bold())

                Text("Super Likes prioritize your profile at the top of their match stack!")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)

                HStack {
                    Text("Cost:")
                        .font(.headline)
                    Text("5 Mingle Coins")
                        .font(.headline.bold())
                        .foregroundColor(.yellow)
                }

                Button(action: {
                    if userCoins >= 5 {
                        userCoins -= 5
                        showCoinsGateModal = false
                        if !profiles.isEmpty { profiles.removeFirst() }
                    }
                }) {
                    Text(userCoins >= 5 ? "Confirm Super Like (-5 Coins)" : "Get More Coins")
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.yellow)
                        .cornerRadius(12)
                        .padding(.horizontal)
                }

                Spacer()
            }
            .padding(.top, 24)
            .navigationBarItems(trailing: Button("Cancel") { showCoinsGateModal = false })
        }
        .presentationDetents([.height(320)])
    }

    private func profileDetailModal(_ profile: MingleProfile) -> some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    RoundedRectangle(cornerRadius: 20)
                        .fill(LinearGradient(colors: profile.avatarGradient, startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(height: 280)
                        .overlay(
                            Image(systemName: "person.fill")
                                .font(.system(size: 100))
                                .foregroundColor(.white.opacity(0.3))
                        )

                    VStack(alignment: .leading, spacing: 8) {
                        Text("\(profile.name), \(profile.age)")
                            .font(.title.bold())

                        Text(profile.occupation)
                            .font(.headline)
                            .foregroundColor(.secondary)

                        Text("\(String(format: "%.1f", profile.distanceKm)) km away • \(profile.mutualConnections) mutual friends")
                            .font(.subheadline)
                            .foregroundColor(.blue)

                        Divider().padding(.vertical, 8)

                        Text("About")
                            .font(.headline)

                        Text(profile.bio)
                            .font(.body)

                        Divider().padding(.vertical, 8)

                        Text("Interests")
                            .font(.headline)

                        HStack {
                            ForEach(profile.interests, id: \.self) { tag in
                                Text("#\(tag)")
                                    .font(.caption.bold())
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 6)
                                    .background(Color.blue.opacity(0.12))
                                    .foregroundColor(.blue)
                                    .cornerRadius(10)
                            }
                        }
                    }
                    .padding(.horizontal)
                }
            }
            .navigationBarItems(trailing: Button("Close") { selectedProfile = nil })
        }
    }
}

#Preview {
    MingleView()
}
