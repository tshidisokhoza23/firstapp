import SwiftUI
import PhotosUI

public struct ChatTabView: View {
    @StateObject public var viewModel: ChatViewModel
    @StateObject public var rewardViewModel: RewardViewModel
    @StateObject private var channelsViewModel = ChannelsViewModel()
    @StateObject private var jobViewModel = JobViewModel()
    @StateObject private var storyRepository = StoryRepository.shared

    public var onChatSelected: (ChatMessage) -> Void = { _ in }
    public var onNewChatClick: () -> Void = {}
    public var onArchivedClick: () -> Void = {}
    public var onProfileClick: () -> Void = {}

    @State private var selectedTab: Int = 0 // 0: Chats, 1: Channels, 2: Jobs, 3: Rewards
    @State private var searchQuery: String = ""
    @State private var isSearchActive: Bool = false
    @State private var showTopMenu: Bool = false
    @State private var showDirectMessages: Bool = false
    @State private var activeChannelDetail: Channel? = nil
    @State private var activeJobDetail: Job? = nil
    @State private var showCreateChannel: Bool = false
    @State private var channelQuery = ""
    @State private var channelSearch: [Channel] = []
    @State private var showPostJob: Bool = false
    @State private var showStarred: Bool = false
    @State private var showApplications: Bool = false
    @State private var showJobProfile: Bool = false
    @State private var showWallet: Bool = false
    @State private var showSettings: Bool = false
    @State private var showMingleActivity: Bool = false
    @State private var showGroup: Bool = false
    @State private var activeStory: UserStory? = nil
    @State private var storyStart: AndroidStoryFlow.Start?
    @State private var storyDraft: String = ""

    private let subTabs = ["Chats", "Channels", "Jobs", "Rewards"]

    @MainActor public init(
        viewModel: ChatViewModel? = nil,
        rewardViewModel: RewardViewModel? = nil,
        onChatSelected: @escaping (ChatMessage) -> Void = { _ in },
        onNewChatClick: @escaping () -> Void = {},
        onArchivedClick: @escaping () -> Void = {},
        onProfileClick: @escaping () -> Void = {}
    ) {
        self._viewModel = StateObject(wrappedValue: viewModel ?? ChatViewModel())
        self._rewardViewModel = StateObject(wrappedValue: rewardViewModel ?? RewardViewModel())
        self.onChatSelected = onChatSelected
        self.onNewChatClick = onNewChatClick
        self.onArchivedClick = onArchivedClick
        self.onProfileClick = onProfileClick
    }

    private var regularChats: [ChatMessage] {
        (viewModel.uiState.chats.data ?? []).filter { !$0.isSecretMingleChat && $0.type != .channel }
    }

    private var chatsList: [ChatMessage] {
        if searchQuery.isEmpty {
            return regularChats.filter { !$0.isArchived }
        } else {
            return regularChats.filter {
                !$0.isArchived &&
                ($0.consistentName().localizedCaseInsensitiveContains(searchQuery) ||
                 $0.lastMessage.localizedCaseInsensitiveContains(searchQuery))
            }
        }
    }

    private var archivedCount: Int {
        regularChats.filter(\.isArchived).count
    }

    private var totalUnreadCount: Int {
        regularChats.reduce(0) { $0 + $1.unreadCount }
    }

    public var body: some View {
        VStack(spacing: 0) {
            // Top App Bar matching Android Screenshot 1
            HStack {
                if isSearchActive {
                    HStack(spacing: 12) {
                        Button(action: {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Image(systemName: "arrow.left")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundColor(.primary)
                        }

                        TextField("Search chats, contacts, messages...", text: $searchQuery)
                            .textFieldStyle(PlainTextFieldStyle())

                        if !searchQuery.isEmpty {
                            Button(action: { searchQuery = "" }) {
                                Image(systemName: "xmark.circle.fill")
                                    .foregroundColor(.gray)
                            }
                        }
                    }
                    .padding(10)
                    .background(Color(UIColor.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                } else {
                    // Left Menu Button with Unread Badge
                    Button(action: { showTopMenu = true }) {
                        ZStack(alignment: .topTrailing) {
                            Image(systemName: "line.3.horizontal")
                                .font(.system(size: 20, weight: .bold))
                                .foregroundColor(.primary)

                            if totalUnreadCount > 0 {
                                Circle()
                                    .fill(Color.red)
                                    .frame(width: 8, height: 8)
                                    .offset(x: 2, y: -2)
                            }
                        }
                    }

                    Spacer()

                    // Center TJ Logo
                    AppLogo(size: 36)

                    Spacer()

                    // Right Search Button
                    Button(action: { isSearchActive = true }) {
                        Image(systemName: "magnifyingglass")
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(.primary)
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            // Horizontal Scrollable Sub-Tab Pills matching Android Screenshot 1
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(0..<subTabs.count, id: \.self) { index in
                        let isSelected = selectedTab == index
                        Button(action: {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                selectedTab = index
                                viewModel.setActiveSubTab(index)
                            }
                        }) {
                            Text(subTabs[index])
                                .font(.system(size: 13, weight: .black))
                                .padding(.horizontal, 18)
                                .padding(.vertical, 8)
                                .background(isSelected ? Color.appPrimary : Color(UIColor.secondarySystemBackground))
                                .foregroundColor(isSelected ? .white : .primary)
                                .clipShape(Capsule())
                                .overlay(
                                    Capsule()
                                        .stroke(isSelected ? Color.clear : Color.gray.opacity(0.15), lineWidth: 1)
                                )
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 6)
            }

            Divider()

            // Sub Tab Content
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
            LiveBannerAd()
        }
        .onAppear {
            viewModel.fetchChats()
            channelsViewModel.fetchChannels()
            jobViewModel.fetchJobs()
        }
        .sheet(isPresented: $showTopMenu) {
            TopMoreMenuSheetView(
                selectedTab: selectedTab,
                onContacts: { showTopMenu = false; onNewChatClick() },
                onNewGroup: { showTopMenu = false; showGroup = true },
                onStarred: { showTopMenu = false; showStarred = true },
                onArchived: { showTopMenu = false; onArchivedClick() },
                onMingle: { showTopMenu = false; showMingleActivity = true },
                onCreateChannel: { showTopMenu = false; showCreateChannel = true },
                onPostJob: { showTopMenu = false; showPostJob = true },
                onApplications: { showTopMenu = false; showApplications = true },
                onJobProfile: { showTopMenu = false; showJobProfile = true },
                onWallet: { showTopMenu = false; showWallet = true },
                onSettings: { showTopMenu = false; showSettings = true },
                onSignOut: {
                    showTopMenu = false
                    Task { await SessionActions.logout() }
                }
            )
            .presentationDetents([.medium])
        }
        .sheet(isPresented: $showStarred) { StarredMessagesView() }
        .sheet(isPresented: $showApplications) {
            JobApplicationsView(onBack: { showApplications = false }, onViewJobDetails: { _ in }, onContactRecruiter: { _ in }, onSetReminder: { _ in })
        }
        .sheet(isPresented: $showJobProfile) { JobProfileView(onClose: { showJobProfile = false }) }
        .sheet(isPresented: $showWallet) { WalletView() }
        .sheet(isPresented: $showSettings) { SettingsView(router: NavigationRouter()) }
        .sheet(isPresented: $showMingleActivity) {
            MingleActivitySheet { conversationId, name in
                showMingleActivity = false
                onChatSelected(ChatMessage(id: conversationId, type: .mingle, senderName: name, isMingle: true))
            }
        }
        .sheet(isPresented: $showGroup) {
            ContactPickerView(
                initialIsGroupMode: true,
                onBack: { showGroup = false },
                onGroupCreate: { name, contacts in
                    showGroup = false
                    viewModel.createGroup(name: name, contactIds: contacts.map(\.id)) { chat in
                        onChatSelected(chat)
                    }
                }
            )
        }
        .sheet(isPresented: $showDirectMessages) {
            DirectMessagesView(
                onConversationClick: { item in
                    showDirectMessages = false
                    onChatSelected(ChatMessage(id: item.id, senderName: item.contactName))
                },
                onNewChatClick: {
                    showDirectMessages = false
                    onNewChatClick()
                }
            )
        }
        .sheet(item: $activeChannelDetail) { ch in
            ChannelDetailView(
                channel: ch,
                onBack: { activeChannelDetail = nil },
                onSubscribe: { id in
                    channelsViewModel.subscribeToChannel(channelId: id)
                }
            )
        }
        .sheet(item: $activeJobDetail) { job in
            JobDetailView(
                job: job,
                onBack: { activeJobDetail = nil },
                onApply: {
                    jobViewModel.applyForJob(jobId: job.id)
                }
            )
        }
        .fullScreenCover(item: $activeStory) { story in
            StoryRowPlayer(startUserId: story.userId.isEmpty ? story.id : story.userId, onClose: { activeStory = nil })
        }
        .fullScreenCover(item: $storyStart) { start in
            AndroidStoryFlow(
                start: start,
                stories: storyRepository.userStories,
                currentUserId: SessionStore.shared.userId,
                onClose: { storyStart = nil },
                onPosted: {
                    storyStart = nil
                    Task { await storyRepository.syncStories() }
                }
            )
        }
        .task { await storyRepository.syncStories() }
    }

    // MARK: - Chats Tab Content matching Screenshot 1

    private var chatsTabContent: some View {
        ZStack(alignment: .bottomTrailing) {
            ScrollView {
                VStack(spacing: 0) {
                    // Horizontal Stories Header Row matching Android Screenshot 1
                    StoriesHeaderRow(
                        stories: storyRepository.userStories,
                        currentUserId: SessionStore.shared.userId,
                        onStoryClick: { activeStory = $0 },
                        onMediaStoryClick: { storyStart = .media },
                        onTextStoryClick: { storyStart = .text },
                        onPostedStoriesClick: { storyStart = .posted }
                    )
                    .padding(.vertical, 8)

                    Divider()

                    // Archived Header Item
                    if archivedCount > 0 {
                        Button(action: onArchivedClick) {
                            HStack {
                                Image(systemName: "archivebox.fill")
                                    .font(.system(size: 20))
                                    .foregroundColor(.appPrimary)
                                Text("Archived")
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.primary)
                                Spacer()
                                Text("\(archivedCount)")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(.appPrimary)
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 12)
                        }
                        Divider()
                    }

                    if viewModel.uiState.isChatsFetching && chatsList.isEmpty {
                        ProgressView("Loading chats...")
                            .padding(.top, 40)
                    } else if chatsList.isEmpty {
                        VStack(spacing: 12) {
                            Image(systemName: "bubble.left.and.bubble.right")
                                .font(.system(size: 48))
                                .foregroundColor(.gray)
                            Text("No chats yet")
                                .font(.system(size: 16, weight: .medium))
                                .foregroundColor(.secondary)
                            Button("Start New Chat", action: onNewChatClick)
                                .buttonStyle(.borderedProminent)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.top, 60)
                    } else {
                        LazyVStack(spacing: 0) {
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

                                Divider()
                                    .padding(.leading, 72)
                            }
                        }
                    }
                }
            }

            // Double Floating Action Buttons matching Android Screenshot 1
            VStack(alignment: .trailing, spacing: 12) {
                // Upper Small Call FAB
                Button(action: { showDirectMessages = true }) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 14)
                            .fill(Color.appBottomNavPill) // #D0E1FB
                            .frame(width: 44, height: 44)
                            .shadow(color: Color.black.opacity(0.12), radius: 4, x: 0, y: 2)

                        Image(systemName: "phone.fill")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(Color.appPrimary)
                    }
                }

                // Lower Main Contacts FAB
                Button(action: onNewChatClick) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 18)
                            .fill(Color.appPrimary) // #0045BC
                            .frame(width: 56, height: 56)
                            .shadow(color: Color.appPrimary.opacity(0.35), radius: 6, x: 0, y: 3)

                        Image(systemName: "person.crop.square.fill")
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.white)
                    }
                }
            }
            .padding(.trailing, 20)
            .padding(.bottom, 16)
        }
    }

    private func searchChannels() async {
        let q = channelQuery.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { channelSearch = []; return }
        guard let data = try? await API.data("chats/channels/search", query: ["query": q]) else { return }
        channelSearch = ChannelJSON.list(from: data)
    }

    // MARK: - Channels Tab Content

    private var channelsTabContent: some View {
        ZStack(alignment: .bottomTrailing) {
            let channelList = channelQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                ? (channelsViewModel.channels.data ?? [])
                : channelSearch
            VStack(spacing: 0) {
                TextField("Search channels", text: $channelQuery)
                    .textFieldStyle(.roundedBorder)
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    .onSubmit { Task { await searchChannels() } }
                    .onChange(of: channelQuery) { value in
                        let q = value.trimmingCharacters(in: .whitespacesAndNewlines)
                        if q.isEmpty { channelSearch = [] } else { Task { await searchChannels() } }
                    }
                if channelList.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "megaphone")
                            .font(.system(size: 48))
                            .foregroundColor(.gray)
                        Text(channelsViewModel.channels.isLoading ? "Loading channels..." : (channelsViewModel.channels.message ?? (channelQuery.isEmpty ? "No channels available" : "No channels match that search")))
                            .font(.system(size: 16, weight: .medium))
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ChannelListView(
                        channels: channelList,
                        currentUserId: SessionStore.shared.userId,
                        onChannelClick: { activeChannelDetail = $0 },
                        onSubscribe: { channelsViewModel.subscribeToChannel(channelId: $0) }
                    )
                }
            }

            // Floating Create Channel Action Button (FAB)
            Button(action: { showCreateChannel = true }) {
                ZStack {
                    RoundedRectangle(cornerRadius: 18)
                        .fill(Color.appPrimary)
                        .frame(width: 56, height: 56)
                        .shadow(color: Color.appPrimary.opacity(0.35), radius: 6, x: 0, y: 3)
                    Image(systemName: "plus")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(.white)
                }
            }
            .padding(24)
        }
        .sheet(isPresented: $showCreateChannel) {
            CreateChannelView(
                viewModel: channelsViewModel,
                onBack: { showCreateChannel = false }
            )
        }
    }

    // MARK: - Jobs Tab Content

    private var jobsTabContent: some View {
        ZStack(alignment: .bottomTrailing) {
            let jobList = jobViewModel.jobs.data ?? []
            JobListView(
                jobs: jobList,
                onJobClick: { job in
                    activeJobDetail = job
                }
            )

            // Floating Post Job Action Button (FAB)
            Button(action: { showPostJob = true }) {
                ZStack {
                    RoundedRectangle(cornerRadius: 18)
                        .fill(Color.appPrimary)
                        .frame(width: 56, height: 56)
                        .shadow(color: Color.appPrimary.opacity(0.35), radius: 6, x: 0, y: 3)
                    Image(systemName: "briefcase.fill")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(.white)
                }
            }
            .padding(24)
        }
        .sheet(isPresented: $showPostJob) {
            PostJobView(onBack: { showPostJob = false })
        }
    }

    // MARK: - Rewards Tab Content matching Screenshot 3

    private var rewardsTabContent: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Total Points Header Card matching Screenshot 3
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        HStack(spacing: 4) {
                            Text("Total Points")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(.secondary)
                            Image(systemName: "info.circle")
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                        }

                        Spacer()

                        VStack(alignment: .trailing, spacing: 2) {
                            Text("Today")
                                .font(.system(size: 13, weight: .medium))
                                .foregroundColor(.secondary)
                            Text("0")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(.primary)
                        }
                    }

                    HStack(spacing: 8) {
                        Image(systemName: "star.circle.fill")
                            .font(.system(size: 32))
                            .foregroundColor(.orange)

                        Text("\(Int(rewardViewModel.points))")
                            .font(.system(size: 36, weight: .black))
                            .foregroundColor(.primary)
                    }
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(UIColor.secondarySystemBackground))
                .cornerRadius(20)

                // Daily Tasks Title
                Text("Daily Tasks")
                    .font(.system(size: 18, weight: .bold))
                    .frame(maxWidth: .infinity, alignment: .leading)

                let dailyTasks: [RewardTask] = {
                    if case .success(let dash) = rewardViewModel.uiState { return dash.tasks }
                    return []
                }()

                if dailyTasks.isEmpty {
                    Text("No tasks yet")
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                ForEach(dailyTasks) { task in
                    HStack(spacing: 16) {
                        ZStack {
                            Circle()
                                .fill(Color.orange.opacity(0.12))
                                .frame(width: 48, height: 48)

                            Image(systemName: "play.circle.fill")
                                .font(.system(size: 24))
                                .foregroundColor(.orange)
                        }

                        VStack(alignment: .leading, spacing: 4) {
                            Text(task.title)
                                .font(.system(size: 15, weight: .bold))
                                .foregroundColor(.primary)

                            Text("+\(Int(task.points)) points")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(.orange)
                        }

                        Spacer()

                        Button(action: {
                            rewardViewModel.showRewardedAd(taskId: task.id, onReward: {})
                        }) {
                            Text("Watch")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 20)
                                .padding(.vertical, 10)
                                .background(Color.appPrimary)
                                .cornerRadius(12)
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
        }
    }
}

public struct TopMoreMenuSheetView: View {
    public let selectedTab: Int
    public var onContacts: () -> Void
    public var onNewGroup: () -> Void
    public var onStarred: () -> Void
    public var onArchived: () -> Void
    public var onMingle: () -> Void
    public var onCreateChannel: () -> Void
    public var onPostJob: () -> Void
    public var onApplications: () -> Void
    public var onJobProfile: () -> Void
    public var onWallet: () -> Void
    public var onSettings: () -> Void
    public var onSignOut: () -> Void

    public var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.headline)
                .padding(.top, 16)
                .padding(.bottom, 8)
            ForEach(rows, id: \.0) { row in
                Button(action: row.1) {
                    HStack {
                        Text(row.0).foregroundColor(.primary)
                        Spacer()
                    }
                    .padding(.vertical, 12)
                }
            }
            Divider()
            Button(action: onSettings) {
                Text("Settings").foregroundColor(.primary).frame(maxWidth: .infinity, alignment: .leading).padding(.vertical, 12)
            }
            Button(action: onSignOut) {
                Text("Sign Out").foregroundColor(.red).frame(maxWidth: .infinity, alignment: .leading).padding(.vertical, 12)
            }
            Spacer()
        }
        .padding(.horizontal, 24)
    }

    private var title: String {
        switch selectedTab {
        case 1: return "Channels"
        case 2: return "Jobs"
        case 3: return "Rewards"
        default: return "Chats"
        }
    }

    private var rows: [(String, () -> Void)] {
        switch selectedTab {
        case 1: return [("Create Channel", onCreateChannel)]
        case 2: return [("Post Job", onPostJob), ("Applications", onApplications), ("Job Profile", onJobProfile)]
        case 3: return [("Wallet", onWallet)]
        default: return [("Contacts", onContacts), ("New Group", onNewGroup), ("Starred Messages", onStarred), ("Archived Chats", onArchived), ("Mingle Activity", onMingle)]
        }
    }
}
