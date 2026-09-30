import SwiftUI

// MARK: - Search Result Models

public enum SearchCategory: String, CaseIterable, Identifiable {
    case all = "All"
    case contacts = "Contacts"
    case messages = "Messages"
    case channels = "Channels"
    case jobs = "Jobs"

    public var id: String { rawValue }

    public var iconName: String {
        switch self {
        case .all: return "magnifyingglass"
        case .contacts: return "person.crop.circle"
        case .messages: return "bubble.left.and.bubble.right"
        case .channels: return "megaphone"
        case .jobs: return "briefcase"
        }
    }
}

public struct SearchResultItem: Identifiable, Hashable {
    public let id: String
    public let title: String
    public let subtitle: String
    public let category: SearchCategory
    public let timestampOrBadge: String?

    public init(id: String, title: String, subtitle: String, category: SearchCategory, timestampOrBadge: String? = nil) {
        self.id = id
        self.title = title
        self.subtitle = subtitle
        self.category = category
        self.timestampOrBadge = timestampOrBadge
    }
}

// MARK: - SearchView

public struct SearchView: View {
    @EnvironmentObject private var router: NavigationRouter
    @State private var searchQuery: String = ""
    @State private var selectedCategory: SearchCategory = .all
    @State private var recentSearches: [String] = ["SwiftUI Navigation 3", "Kotlin Multiplatform", "Senior Android Developer", "TJ Announcements"]
    @State private var selectedResult: SearchResultItem? = nil

    private let sampleResults: [SearchResultItem] = [
        SearchResultItem(id: "r1", title: "Alex Morgan", subtitle: "Senior iOS Engineer • @alexmorgan", category: .contacts, timestampOrBadge: "Online"),
        SearchResultItem(id: "r2", title: "Sarah Connor", subtitle: "Product Manager • @sconnor", category: .contacts, timestampOrBadge: "Away"),
        SearchResultItem(id: "r3", title: "Build deployed to production", subtitle: "Dev Team Lead: Hey everyone, version 2.4 is live now!", category: .messages, timestampOrBadge: "10:42 AM"),
        SearchResultItem(id: "r4", title: "SwiftUI Architecture Discussion", subtitle: "Alex Morgan: Have you tested Navigation 3 scenes yet?", category: .messages, timestampOrBadge: "Yesterday"),
        SearchResultItem(id: "r5", title: "iOS & Swift Developers World", subtitle: "12,400 Members • Public Channel", category: .channels, timestampOrBadge: "Official"),
        SearchResultItem(id: "r6", title: "Kotlin & Android Tech Hub", subtitle: "8,900 Members • Tech Community", category: .channels, timestampOrBadge: "Community"),
        SearchResultItem(id: "r7", title: "Senior Android Engineer (Jetpack Compose)", subtitle: "TJ Technologies • Remote • $140k - $180k", category: .jobs, timestampOrBadge: "Full-Time"),
        SearchResultItem(id: "r8", title: "Lead iOS Architect (SwiftUI & SPM)", subtitle: "Global Tech Inc • Hybrid San Francisco", category: .jobs, timestampOrBadge: "Featured")
    ]

    public init() {}

    private var filteredResults: [SearchResultItem] {
        if searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return []
        }
        return sampleResults.filter { item in
            let matchesCategory = (selectedCategory == .all || item.category == selectedCategory)
            let matchesQuery = item.title.localizedCaseInsensitiveContains(searchQuery) ||
                               item.subtitle.localizedCaseInsensitiveContains(searchQuery)
            return matchesCategory && matchesQuery
        }
    }

    public var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Search Input Bar
                HStack(spacing: 10) {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)

                    TextField("Search contacts, messages, channels & jobs...", text: $searchQuery)
                        .textFieldStyle(.plain)
                        .autocapitalization(.none)

                    if !searchQuery.isEmpty {
                        Button(action: { searchQuery = "" }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding(12)
                .background(Color(.secondarySystemBackground))
                .cornerRadius(14)
                .padding(.horizontal)
                .padding(.top, 8)

                // Category Filter Tabs
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(SearchCategory.allCases) { cat in
                            Button(action: { selectedCategory = cat }) {
                                HStack(spacing: 6) {
                                    Image(systemName: cat.iconName)
                                        .font(.caption)
                                    Text(cat.rawValue)
                                        .font(.footnote)
                                        .fontWeight(selectedCategory == cat ? .bold : .medium)
                                }
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(selectedCategory == cat ? Color.blue : Color(.tertiarySystemBackground))
                                .foregroundColor(selectedCategory == cat ? .white : .primary)
                                .cornerRadius(16)
                            }
                        }
                    }
                    .padding(.horizontal)
                    .padding(.vertical, 10)
                }

                Divider()

                // Search Body
                if searchQuery.isEmpty {
                    recentSearchesSection
                } else if filteredResults.isEmpty {
                    Spacer()
                    VStack(spacing: 12) {
                        Image(systemName: "doc.text.magnifyingglass")
                            .font(.system(size: 56))
                            .foregroundColor(.secondary)
                        Text("No matching results")
                            .font(.title3.bold())
                        Text("Try checking your spelling or searching for a different keyword.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 32)
                    }
                    Spacer()
                } else {
                    List {
                        ForEach(SearchCategory.allCases.filter { $0 != .all }) { cat in
                            let catResults = filteredResults.filter { $0.category == cat }
                            if !catResults.isEmpty {
                                Section(header:
                                    HStack {
                                        Label(cat.rawValue, systemImage: cat.iconName)
                                            .font(.caption.bold())
                                        Spacer()
                                        Text("\(catResults.count)")
                                            .font(.caption2)
                                            .foregroundColor(.secondary)
                                    }
                                ) {
                                    ForEach(catResults) { item in
                                        resultRowView(item)
                                    }
                                }
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                }
            }
            .navigationTitle("Global Search")
            .sheet(item: $selectedResult) { item in
                resultDetailModal(item)
            }
        }
    }

    // MARK: - Subviews

    private var recentSearchesSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("RECENT SEARCHES")
                    .font(.caption.bold())
                    .foregroundColor(.secondary)

                Spacer()

                if !recentSearches.isEmpty {
                    Button("Clear All") {
                        recentSearches.removeAll()
                    }
                    .font(.caption.bold())
                    .foregroundColor(.red)
                }
            }
            .padding(.horizontal)
            .padding(.top, 16)

            if recentSearches.isEmpty {
                Text("No recent searches")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .padding(.horizontal)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(recentSearches, id: \.self) { search in
                            HStack(spacing: 6) {
                                Text(search)
                                    .font(.subheadline)

                                Button(action: {
                                    recentSearches.removeAll { $0 == search }
                                }) {
                                    Image(systemName: "xmark")
                                        .font(.caption2)
                                        .foregroundColor(.gray)
                                }
                            }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(Color(.secondarySystemBackground))
                            .cornerRadius(16)
                            .onTapGesture {
                                searchQuery = search
                            }
                        }
                    }
                    .padding(.horizontal)
                }
            }

            Spacer()
        }
    }

    private func resultRowView(_ item: SearchResultItem) -> some View {
        HStack(spacing: 12) {
            Circle()
                .fill(categoryColor(item.category).opacity(0.15))
                .frame(width: 40, height: 40)
                .overlay(
                    Image(systemName: item.category.iconName)
                        .font(.system(size: 18))
                        .foregroundColor(categoryColor(item.category))
                )

            VStack(alignment: .leading, spacing: 2) {
                Text(item.title)
                    .font(.subheadline.bold())
                    .lineLimit(1)

                Text(item.subtitle)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }

            Spacer()

            if let badge = item.timestampOrBadge {
                Text(badge)
                    .font(.caption2.bold())
                    .foregroundColor(categoryColor(item.category))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(categoryColor(item.category).opacity(0.12))
                    .cornerRadius(8)
            }
        }
        .padding(.vertical, 4)
        .contentShape(Rectangle())
        .onTapGesture {
            selectedResult = item
            if !recentSearches.contains(item.title) {
                recentSearches.insert(item.title, at: 0)
            }
        }
    }

    private func resultDetailModal(_ item: SearchResultItem) -> some View {
        NavigationStack {
            VStack(spacing: 20) {
                Circle()
                    .fill(categoryColor(item.category).opacity(0.15))
                    .frame(width: 80, height: 80)
                    .overlay(
                        Image(systemName: item.category.iconName)
                            .font(.system(size: 36))
                            .foregroundColor(categoryColor(item.category))
                    )

                Text(item.title)
                    .font(.title2.bold())
                    .multilineTextAlignment(.center)

                Text(item.subtitle)
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)

                Button("Open \(item.category.rawValue)") {
                    selectedResult = nil
                    openResult(item)
                }
                .fontWeight(.bold)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding()
                .background(categoryColor(item.category))
                .cornerRadius(12)
                .padding(.horizontal)

                Spacer()
            }
            .padding(.top, 24)
            .navigationBarItems(trailing: Button("Close") { selectedResult = nil })
        }
        .presentationDetents([.height(340)])
    }

    private func openResult(_ item: SearchResultItem) {
        switch item.category {
        case .contacts:
            router.navigate(to: .profile(userId: item.id))
        case .messages, .channels, .jobs, .all:
            router.navigate(to: .chatDetail(conversationId: item.id, participantName: item.title))
        }
    }

    private func categoryColor(_ category: SearchCategory) -> Color {
        switch category {
        case .all: return .blue
        case .contacts: return .purple
        case .messages: return .green
        case .channels: return .orange
        case .jobs: return .indigo
        }
    }
}

#Preview {
    SearchView()
}
