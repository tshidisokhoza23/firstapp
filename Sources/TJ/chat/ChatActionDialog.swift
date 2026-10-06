import SwiftUI

struct MuteDurationSheet: View {
    @Environment(\.dismiss) var dismiss
    var onSelectDuration: (String) -> Void

    var body: some View {
        NavigationView {
            List {
                Button("For 8 hours") {
                    onSelectDuration("8 hours")
                    dismiss()
                }
                Button("For 1 week") {
                    onSelectDuration("1 week")
                    dismiss()
                }
                Button("Always") {
                    onSelectDuration("Always")
                    dismiss()
                }
            }
            .navigationTitle("Mute Notifications")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
        .frame(height: 240)
    }
}

struct ChatActionDialog: View {
    let chat: ChatItem
    @Environment(\.dismiss) var dismiss

    var onTogglePin: () -> Void
    var onOpenMutePicker: () -> Void
    var onToggleArchive: () -> Void
    var onMarkReadUnread: () -> Void
    var onClearHistory: () -> Void
    var onDeleteChat: (Bool) -> Void

    @State private var showingMuteSheet: Bool = false
    @State private var showingDeleteAlert: Bool = false
    @State private var deleteMediaFromStorage: Bool = true

    var body: some View {
        NavigationView {
            List {
                Section(header: Text(chat.name).font(.headline).fontWeight(.bold)) {
                    Button(action: {
                        onTogglePin()
                        dismiss()
                    }) {
                        Label("Pin Chat", systemImage: "pin.fill")
                            .foregroundColor(AppColors.brandPrimary)
                    }

                    Button(action: {
                        showingMuteSheet = true
                    }) {
                        Label("Mute Notifications", systemImage: "bell.slash.fill")
                            .foregroundColor(AppColors.brandPrimary)
                    }

                    Button(action: {
                        onToggleArchive()
                        dismiss()
                    }) {
                        Label("Archive Chat", systemImage: "archivebox.fill")
                            .foregroundColor(AppColors.brandPrimary)
                    }

                    Button(action: {
                        onMarkReadUnread()
                        dismiss()
                    }) {
                        Label("Mark as Read", systemImage: "envelope.open.fill")
                            .foregroundColor(AppColors.brandPrimary)
                    }

                    Button(action: {
                        onClearHistory()
                        dismiss()
                    }) {
                        Label("Clear Chat History", systemImage: "sparkles")
                            .foregroundColor(.orange)
                    }

                    Button(action: {
                        showingDeleteAlert = true
                    }) {
                        Label("Delete Chat for Me", systemImage: "trash.fill")
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle("Conversation Options")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Close") { dismiss() }
                }
            }
            .sheet(isPresented: $showingMuteSheet) {
                MuteDurationSheet { duration in
                    onOpenMutePicker()
                    dismiss()
                }
            }
            .alert("Delete Chat with \(chat.name)?", isPresented: $showingDeleteAlert) {
                Button("Delete for Me", role: .destructive) {
                    onDeleteChat(deleteMediaFromStorage)
                    dismiss()
                }
                Button("Cancel", role: .cancel) { }
            } message: {
                Text("This chat will be cleared from your conversation list. The other recipient will still keep their chat history.")
            }
        }
    }
}
