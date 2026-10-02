import SwiftUI

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
    public let kind: String

    public init(id: String, title: String, subtitle: String, category: SearchCategory, timestampOrBadge: String? = nil, kind: String = "chat") {
        self.id = id
        self.title = title
        self.subtitle = subtitle
        self.category = category
        self.timestampOrBadge = timestampOrBadge
        self.kind = kind
    }
}

public struct SearchView: View {
    @EnvironmentObject private var router: NavigationRouter
    @State private var searchQuery = ""
    @State private var selectedCategory: SearchCategory = .all
    @State private var recentSearches: [String] = UserDefaults.standard.stringArray(forKey: "tj_recent_searches") ?? []
    @State private var liveResults: [SearchResultItem] = []
    @State private var selectedResult: SearchResultItem?
    @State private var status = ""

    public init() {}

    public var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                HStack(spacing: 10) {
                    Image(systemName: "magnifyingglass").foregroundColor(.secondary)
                    TextField("Search contacts, messages, channels & jobs...", text: $searchQuery)
                        .textFieldStyle(.plain)
                        .autocapitalization(.none)
                        .submitLabel(.search)
                        .onSubmit { Task { await runSearch() } }
                    if !searchQuery.isEmpty {
                        Button { searchQuery = ""; liveResults = [] } label: {
                            Image(systemName: "xmark.circle.fill").foregroundColor(.secondary)
                        }
                    }
                }
                .padding(12)
                .background(Color(.secondarySystemBackground))
                .cornerRadius(14)
                .padding(.horizontal)
                .padding(.top, 8)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(SearchCategory.allCases) { cat in
                            Button {
                                selectedCategory = cat
                                Task { await runSearch() }
                            } label: {
                                HStack(spacing: 6) {
                                    Image(systemName: cat.iconName).font(.caption)
                                    Text(cat.rawValue).font(.footnote).fontWeight(selectedCategory == cat ? .bold : .medium)
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

                if searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    recentSection
                } else if liveResults.isEmpty {
                    Spacer()
                    Text(status.isEmpty ? "No matching results" : status)
                        .foregroundColor(.secondary)
                    Spacer()
                } else {
                    List(liveResults) { item in
                        Button { selectedResult = item } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.title).foregroundColor(.primary)
                                if !item.subtitle.isEmpty {
                                    Text(item.subtitle).font(.caption).foregroundColor(.secondary)
                                }
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                }
            }
            .navigationTitle("Global Search")
            .sheet(item: $selectedResult) { item in
                VStack(alignment: .leading, spacing: 12) {
                    Text(item.title).font(.title3.bold())
                    if !item.subtitle.isEmpty { Text(item.subtitle).foregroundColor(.secondary) }
                    if item.kind == "profile" || item.kind == "chat" {
                        Button(item.kind == "profile" ? "Open profile" : "Open chat") {
                            selectedResult = nil
                            open(item)
                        }
                    }
                    Button("Close") { selectedResult = nil }
                }
                .padding()
                .presentationDetents([.medium])
            }
        }
    }

    private var recentSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("RECENT SEARCHES").font(.caption.bold()).foregroundColor(.secondary)
                Spacer()
                if !recentSearches.isEmpty {
                    Button("Clear All") {
                        recentSearches = []
                        UserDefaults.standard.removeObject(forKey: "tj_recent_searches")
                    }
                    .font(.caption.bold())
                    .foregroundColor(.red)
                }
            }
            if recentSearches.isEmpty {
                Text("No recent searches").foregroundColor(.secondary)
            } else {
                ForEach(recentSearches, id: \.self) { term in
                    Button(term) {
                        searchQuery = term
                        Task { await runSearch() }
                    }
                    .foregroundColor(.primary)
                }
            }
            Spacer()
        }
        .padding()
    }

    private func open(_ item: SearchResultItem) {
        switch item.kind {
        case "profile":
            router.navigate(to: .profile(userId: item.id))
        case "chat":
            router.navigate(to: .chatDetail(conversationId: item.id, participantName: item.title))
        default:
            break
        }
    }

    private func runSearch() async {
        let text = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else {
            liveResults = []
            status = ""
            return
        }
        var saved = recentSearches.filter { $0 != text }
        saved.insert(text, at: 0)
        recentSearches = Array(saved.prefix(8))
        UserDefaults.standard.set(recentSearches, forKey: "tj_recent_searches")
        guard let data = try? await API.data("search", query: ["query": text, "type": searchType]) else {
            liveResults = []
            status = "Search failed"
            return
        }
        liveResults = LiveSearchView.parse(data, category: selectedCategory)
        status = liveResults.isEmpty ? "No matching results" : ""
    }

    private var searchType: String {
        switch selectedCategory {
        case .all: return "global"
        case .contacts, .messages: return "chats"
        case .channels: return "channels"
        case .jobs: return "jobs"
        }
    }
}
