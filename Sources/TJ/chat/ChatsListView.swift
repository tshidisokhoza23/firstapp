import SwiftUI

struct ChatsListView: View {
    @EnvironmentObject var chatViewModel: ChatViewModel

    var body: some View {
        List {
            ForEach(chatViewModel.chats) { chat in
                Button(action: {
                    chatViewModel.activeChat = chat
                }) {
                    HStack(spacing: 12) {
                        ZStack(alignment: .bottomTrailing) {
                            Image(systemName: "person.crop.circle.fill")
                                .resizable()
                                .scaledToFit()
                                .frame(width: 50, height: 50)
                                .foregroundColor(AppColors.greyMedium)

                            if chat.isOnline {
                                Circle()
                                    .fill(Color.green)
                                    .frame(width: 14, height: 14)
                                    .overlay(Circle().stroke(Color.white, lineWidth: 2))
                            }
                        }

                        VStack(alignment: .leading, spacing: 4) {
                            HStack {
                                Text(chat.name)
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundColor(.primary)
                                Spacer()
                                Text(chat.timestamp)
                                    .font(.system(size: 12))
                                    .foregroundColor(AppColors.greyMedium)
                            }

                            HStack {
                                Text(chat.lastMessage)
                                    .font(.system(size: 14))
                                    .foregroundColor(AppColors.greyMedium)
                                    .lineLimit(1)
                                Spacer()
                                if chat.unreadCount > 0 {
                                    Text("\(chat.unreadCount)")
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundColor(.white)
                                        .padding(.horizontal, 6)
                                        .padding(.vertical, 2)
                                        .background(AppColors.brandPrimary)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
        .listStyle(PlainListStyle())
    }
}
