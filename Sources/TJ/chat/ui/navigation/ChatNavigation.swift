import SwiftUI

public enum ChatRoute: Hashable {
    case chatTab
    case chatDetail(chat: ChatMessage)
    case chatProfile(chat: ChatMessage)
    case messageInfo(messageId: String)
    case archivedChats
    case contactPicker(isGroupMode: Bool)
    case starredMessages
}

public struct ChatNavigationView: View {
    @StateObject public var chatViewModel = ChatViewModel()
    @StateObject public var contactPickerViewModel = ContactPickerViewModel()
    @StateObject public var starredMessagesViewModel = StarredMessagesViewModel()
    @StateObject public var rewardViewModel = RewardViewModel()

    @State private var navigationPath = NavigationPath()

    public init() {}

    public var body: some View {
        NavigationStack(path: $navigationPath) {
            ChatTabView(
                viewModel: chatViewModel,
                rewardViewModel: rewardViewModel,
                onChatSelected: { chat in
                    navigationPath.append(ChatRoute.chatDetail(chat: chat))
                },
                onNewChatClick: {
                    navigationPath.append(ChatRoute.contactPicker(isGroupMode: false))
                },
                onArchivedClick: {
                    navigationPath.append(ChatRoute.archivedChats)
                },
                onProfileClick: {
                    // Profile route
                }
            )
            .navigationDestination(for: ChatRoute.self) { route in
                switch route {
                case .chatTab:
                    ChatTabView(
                        viewModel: chatViewModel,
                        rewardViewModel: rewardViewModel,
                        onChatSelected: { chat in
                            navigationPath.append(ChatRoute.chatDetail(chat: chat))
                        },
                        onNewChatClick: {
                            navigationPath.append(ChatRoute.contactPicker(isGroupMode: false))
                        },
                        onArchivedClick: {
                            navigationPath.append(ChatRoute.archivedChats)
                        }
                    )
                case .chatDetail(let chat):
                    ChatView(
                        chat: chat,
                        viewModel: chatViewModel,
                        onBack: { navigationPath.removeLast() },
                        onShowProfile: { _, _ in
                            navigationPath.append(ChatRoute.chatProfile(chat: chat))
                        }
                    )
                    .navigationBarHidden(true)
                case .chatProfile(let chat):
                    ChatProfileView(
                        chat: chat,
                        viewModel: chatViewModel,
                        onBack: { navigationPath.removeLast() }
                    )
                    .navigationBarHidden(true)
                case .messageInfo(let messageId):
                    MessageInfoView(
                        messageId: messageId,
                        viewModel: chatViewModel,
                        onBack: { navigationPath.removeLast() }
                    )
                    .navigationBarHidden(true)
                case .archivedChats:
                    ArchivedChatsView(
                        viewModel: chatViewModel,
                        onBack: { navigationPath.removeLast() },
                        onChatClick: { chat in
                            navigationPath.append(ChatRoute.chatDetail(chat: chat))
                        }
                    )
                    .navigationBarHidden(true)
                case .contactPicker(let isGroupMode):
                    ContactPickerView(
                        viewModel: contactPickerViewModel,
                        initialIsGroupMode: isGroupMode,
                        onBack: { navigationPath.removeLast() },
                        onContactSelected: { contact in
                            let chat = ChatMessage(id: contact.id, senderName: contact.displayName, phoneNumber: contact.phoneNumber)
                            navigationPath.append(ChatRoute.chatDetail(chat: chat))
                        },
                        onInviteClicked: {},
                        onGroupCreate: { name, contacts in
                            chatViewModel.createGroup(name: name, contactIds: contacts.map(\.id)) { chat in
                                navigationPath.append(ChatRoute.chatDetail(chat: chat))
                            }
                        }
                    )
                    .navigationBarHidden(true)
                case .starredMessages:
                    StarredMessagesView(
                        viewModel: starredMessagesViewModel,
                        onBack: { navigationPath.removeLast() },
                        onMessageClick: { msg in }
                    )
                    .navigationBarHidden(true)
                }
            }
        }
    }
}
