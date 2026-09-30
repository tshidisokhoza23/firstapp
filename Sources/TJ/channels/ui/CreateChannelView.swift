import SwiftUI

public struct CreateChannelView: View {
    @ObservedObject public var viewModel: ChannelsViewModel
    public var onBack: () -> Void
    public var onCreated: (String) -> Void

    @State private var name: String = ""
    @State private var description: String = ""
    @State private var avatarUrl: String? = nil

    public init(
        viewModel: ChannelsViewModel,
        onBack: @escaping () -> Void,
        onCreated: @escaping (String) -> Void = { _ in }
    ) {
        self.viewModel = viewModel
        self.onBack = onBack
        self.onCreated = onCreated
    }

    public var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Channel Icon Selection
                    ZStack {
                        Circle()
                            .fill(Color(uiColor: .secondarySystemBackground))
                            .frame(width: 100, height: 100)

                        VStack(spacing: 4) {
                            Image(systemName: "camera.fill")
                                .font(.system(size: 28))
                                .foregroundColor(.appPrimary)
                            Text("ADD ICON")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundColor(.appPrimary)
                        }
                    }
                    .padding(.top, 24)

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Channel Name")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.appPrimary)

                        TextField("Enter channel name", text: $name)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Description (optional)")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.appPrimary)

                        TextEditor(text: $description)
                            .frame(height: 100)
                            .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.gray.opacity(0.3), lineWidth: 1))
                    }

                    Text("Anyone can discover your channel and see the last 30 days of history.")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)

                    Button(action: {
                        if !name.trimmingCharacters(in: .whitespaces).isEmpty {
                            viewModel.createChannel(name: name, description: description, avatarUrl: avatarUrl)
                        }
                    }) {
                        Text("Create Channel")
                            .font(.system(size: 16, weight: .bold))
                            .frame(maxWidth: .infinity)
                            .frame(height: 50)
                            .background(name.isEmpty ? Color.gray : Color.appPrimary)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }
                    .disabled(name.isEmpty)
                }
                .padding(24)
            }
            .navigationTitle("New Channel")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                    }
                }
            }
            .onReceive(viewModel.$createdChannelId) { channelId in
                if let id = channelId, !id.isEmpty {
                    onCreated(id)
                }
            }
        }
    }
}
