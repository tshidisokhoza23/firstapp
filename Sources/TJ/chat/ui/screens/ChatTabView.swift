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
    @State private var activeChannelDetail: Channel? = nil
    @State private var activeJobDetail: Job? = nil
    @State private var showCreateChannel: Bool = false
    @State private var showPostJob: Bool = false
    @State private var activeStory: UserStory? = nil
    @State private var showAddStory: Bool = false
    @State private var storyDraft: String = ""

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

    private var archivedCount: Int {
        (viewModel.uiState.chats.data ?? []).filter { $0.isArchived }.count
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

            // Sub Tabs Segmented Picker matching Android (Chats, Channels, Jobs, Rewards)
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
        .onAppear {
            viewModel.fetchChats()
            channelsViewModel.fetchChannels()
            jobViewModel.fetchJobs()
            Task { _ = await storyRepository.syncStories() }
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
            StoryViewer(story: story, onClose: { activeStory = nil })
        }
        .sheet(isPresented: $showAddStory) {
            StoryComposerSheet(draft: $storyDraft) { text, file in
                showAddStory = false
                storyDraft = ""
                Task {
                    do {
                        var contentURL: String?
                        var type = "text"
                        if let file {
                            let data = try Data(contentsOf: file.url)
                            let isVideo = ["mp4", "mov", "m4v"].contains(file.url.pathExtension.lowercased())
                            type = isVideo ? "video" : "image"
                            contentURL = try await MediaActions.upload(
                                data: data,
                                filename: file.url.lastPathComponent,
                                mime: isVideo ? "video/mp4" : "image/jpeg",
                                folder: "stories"
                            )
                        }
                        _ = try await storyRepository.createStory(content: text, contentUrl: contentURL, type: type)
                    } catch {
                        print("[Story] \(error)")
                    }
                }
            }
        }
    }

    // MARK: - Chats Tab Content

    private var chatsTabContent: some View {
        ZStack(alignment: .bottomTrailing) {
            ScrollView {
                VStack(spacing: 0) {
                    // Stories Horizontal Row
                    StoriesHeaderRow(
                        stories: storyRepository.userStories,
                        onStoryClick: { activeStory = $0 },
                        onMediaStoryClick: { showAddStory = true }
                    )
                    .padding(.vertical, 8)

                    Divider()

                    // Archived Chats Header Button
                    if archivedCount > 0 {
                        Button(action: onArchivedClick) {
                            HStack {
                                Image(systemName: "archivebox.fill")
                                    .foregroundColor(.accentColor)
                                Text("Archived")
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.primary)
                                Spacer()
                                Text("\(archivedCount)")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(.accentColor)
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
                                .font(.system(size: 16))
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

    // MARK: - Channels Tab Content

    private var channelsTabContent: some View {
        ZStack(alignment: .bottomTrailing) {
            let channelList = channelsViewModel.channels.data ?? []
            if channelList.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "megaphone")
                        .font(.system(size: 48))
                        .foregroundColor(.gray)
                    Text("No channels available")
                        .font(.system(size: 16))
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    LazyVStack(spacing: 0) {
                        ForEach(channelList) { ch in
                            ChannelItemView(
                                channel: ch,
                                onClick: {
                                    activeChannelDetail = ch
                                },
                                onSubscribe: { id in
                                    channelsViewModel.subscribeToChannel(channelId: id)
                                }
                            )
                            Divider()
                        }
                    }
                }
            }

            // FAB Create Channel Button
            Button(action: { showCreateChannel = true }) {
                ZStack {
                    Circle()
                        .fill(Color.accentColor)
                        .frame(width: 56, height: 56)
                        .shadow(radius: 4)
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

            // FAB Post Job Button
            Button(action: { showPostJob = true }) {
                ZStack {
                    Circle()
                        .fill(Color.accentColor)
                        .frame(width: 56, height: 56)
                        .shadow(radius: 4)
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

    // MARK: - Rewards Tab Content

    private var rewardsTabContent: some View {
        ScrollView {
            VStack(spacing: 20) {
                Text("Rewards & Points")
                    .font(.title2.bold())
                    .padding(.top)

                VStack(spacing: 8) {
                    Text("\(Int(rewardViewModel.points))")
                        .font(.system(size: 36, weight: .black))
                        .foregroundColor(.accentColor)
                    Text("Total Reward Points")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding()
                .frame(maxWidth: .infinity)
                .background(Color(UIColor.secondarySystemBackground))
                .cornerRadius(16)

                if case .success(let dash) = rewardViewModel.uiState {
                    Text("Ads today \(dash.adsWatchedToday)/\(max(dash.maxDailyAds, 1))")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    ForEach(dash.tasks) { task in
                        HStack {
                            Text(task.title)
                            Spacer()
                            Text("\(Int(task.points))")
                                .foregroundColor(task.isCompleted ? .secondary : .accentColor)
                        }
                        .padding(.vertical, 6)
                    }
                }

                Button(action: {
                    rewardViewModel.showRewardedAd(taskId: "daily_ad", onReward: {})
                }) {
                    HStack {
                        Image(systemName: "play.circle.fill")
                            .font(.system(size: 22))
                        Text("Watch Ad (+10 Points)")
                            .font(.system(size: 16, weight: .bold))
                    }
                    .foregroundColor(.white)
                    .padding()
                    .frame(maxWidth: .infinity)
                    .background(Color.accentColor)
                    .cornerRadius(12)
                }

                Spacer()
            }
            .padding(.horizontal, 24)
        }
    }
}

private struct StoryComposerSheet: View {
    @Binding var draft: String
    var onPost: (String, PickedFile?) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var photoItem: PhotosPickerItem?
    @State private var file: PickedFile?

    var body: some View {
        NavigationStack {
            Form {
                TextField("What's happening?", text: $draft, axis: .vertical)
                PhotosPicker(selection: $photoItem, matching: .any(of: [.images, .videos])) {
                    Label(file == nil ? "Add photo or video" : "Media attached", systemImage: "photo")
                }
            }
            .navigationTitle("New story")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Post") {
                        onPost(draft, file)
                    }
                    .disabled(draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && file == nil)
                }
            }
            .onChange(of: photoItem) { item in
                guard let item else { return }
                Task {
                    file = try? await item.loadTransferable(type: PickedFile.self)
                }
            }
        }
    }
}
