import SwiftUI

public struct ChatTabView: View {
    @StateObject public var viewModel: ChatViewModel
    @StateObject public var rewardViewModel: RewardViewModel

    public var onChatSelected: (ChatMessage) -> Void = { _ in }
    public var onNewChatClick: () -> Void = {}
    public var onArchivedClick: () -> Void = {}
    public var onProfileClick: () -> Void = {}

    @State private var selectedTab: Int = 0 // 0: Chats, 1: Channels, 2: Jobs, 3: Rewards
    @State private var searchQuery: String = ""
    @State private var isSearchActive: Bool = false

    @MainActor public init(
        viewModel: ChatViewModel = ChatViewModel(),
        rewardViewModel: RewardViewModel = RewardViewModel(),
        onChatSelected: @escaping (ChatMessage) -> Void = { _ in },
        onNewChatClick: @escaping () -> Void = {},
        onArchivedClick: @escaping () -> Void = {},
        onProfileClick: @escaping () -> Void = {}
    ) {
        self._viewModel = StateObject(wrappedValue: viewModel)
        self._rewardViewModel = StateObject(wrappedValue: rewardViewModel)
        self.onChatSelected = onChatSelected
        self.onNewChatClick = onNewChatClick
        self.onArchivedClick = onArchivedClick
        self.onProfileClick = onProfileClick
    }

    private var chatsList: [ChatMessage] {
        let all = viewModel.uiState.chats.data ?? []
        if searchQuery.isEmpty {
            return all.filter { !$0.isArchived }
        } else {
            return all.filter {
                !$0.isArchived &&
                ($0.consistentName().localizedCaseInsensitiveContains(searchQuery) ||
                 $0.lastMessage.localizedCaseInsensitiveContains(searchQuery))
            }
        }
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Top Bar
            HStack {
                if isSearchActive {
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.gray)
                        TextField("Search...", text: $searchQuery)
                            .textFieldStyle(PlainTextFieldStyle())
                        Button(action: {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.gray)
                        }
                    }
                    .padding(8)
                    .background(Color(UIColor.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                } else {
                    Text("TJ Chat")
                        .font(.system(size: 22, weight: .bold))

                    Spacer()

                    Button(action: { isSearchActive = true }) {
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 20))
                    }

                    Button(action: onArchivedClick) {
                        Image(systemName: "archivebox")
                            .font(.system(size: 20))
                    }

                    Button(action: onProfileClick) {
                        Image(systemName: "person.circle")
                            .font(.system(size: 20))
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            // Sub Tabs Segmented Picker
            Picker("", selection: $selectedTab) {
                Text("Chats").tag(0)
                Text("Channels").tag(1)
                Text("Jobs").tag(2)
                Text("Rewards").tag(3)
            }
            .pickerStyle(SegmentedPickerStyle())
            .padding(.horizontal, 16)
            .padding(.bottom, 8)

            Divider()

            // Active Call Overlay Banner
            CallBannerOverlay()

            // Tab Content
            switch selectedTab {
            case 0:
                chatsTabContent
            case 1:
                channelsTabContent
            case 2:
                jobsTabContent
            case 3:
                rewardsTabContent
            default:
                chatsTabContent
            }
        }
    }

    private var chatsTabContent: some View {
        ZStack(alignment: .bottomTrailing) {
            if viewModel.uiState.isChatsFetching && chatsList.isEmpty {
                ProgressView("Loading chats...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if chatsList.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "bubble.left.and.bubble.right")
                        .font(.system(size: 48))
                        .foregroundColor(.gray)
                    Text("No chats yet")
                        .font(.system(size: 16))
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                List {
                    ForEach(chatsList) { chat in
                        ChatItemRow(
                            chat: chat,
                            onAvatarClick: {
                                onChatSelected(chat)
                            }
                        )
                        .onTapGesture {
                            onChatSelected(chat)
                        }
                    }
                }
                .listStyle(PlainListStyle())
            }

            // FAB New Chat Button
            Button(action: onNewChatClick) {
                ZStack {
                    Circle()
                        .fill(Color.accentColor)
                        .frame(width: 56, height: 56)
                        .shadow(radius: 4)
                    Image(systemName: "square.and.pencil")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(.white)
                }
            }
            .padding(24)
        }
    }

    private var channelsTabContent: some View {
        VStack {
            Text("Channels")
                .font(.headline)
                .padding()
            Spacer()
        }
    }

    private var jobsTabContent: some View {
        VStack {
            Text("Jobs")
                .font(.headline)
                .padding()
            Spacer()
        }
    }

    private var rewardsTabContent: some View {
        ScrollView {
            VStack(spacing: 16) {
                Text("Rewards & Points")
                    .font(.title2)
                    .bold()
                    .padding(.top)

                Text("Points: \(Int(rewardViewModel.points))")
                    .font(.system(size: 28, weight: .heavy))
                    .foregroundColor(.accentColor)

                Button(action: {
                    rewardViewModel.showRewardedAd(taskId: "daily_ad", onReward: {})
                }) {
                    Text("Watch Ad (+10 Points)")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.white)
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(Color.accentColor)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 32)

                Spacer()
            }
        }
    }
}
