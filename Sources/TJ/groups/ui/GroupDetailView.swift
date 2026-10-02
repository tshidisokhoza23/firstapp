import SwiftUI

// MARK: - Group Models

public enum GroupRole: String, CaseIterable, Codable {
    case owner = "Owner"
    case admin = "Admin"
    case moderator = "Moderator"
    case member = "Member"

    public var badgeColor: Color {
        switch self {
        case .owner: return .purple
        case .admin: return .blue
        case .moderator: return .orange
        case .member: return .gray
        }
    }
}

public struct GroupMemberItem: Identifiable {
    public let id: String
    public let name: String
    public let handle: String
    public let avatarUrl: String?
    public var role: GroupRole
    public var isOnline: Bool

    public init(id: String, name: String, handle: String, avatarUrl: String? = nil, role: GroupRole = .member, isOnline: Bool = false) {
        self.id = id
        self.name = name
        self.handle = handle
        self.avatarUrl = avatarUrl
        self.role = role
        self.isOnline = isOnline
    }
}

// MARK: - GroupDetailView

public struct GroupDetailView: View {
    @State private var groupName: String = ""
    @State private var groupDescription: String = ""
    @State private var isMuted: Bool = false
    @State private var onlyAdminsCanSend: Bool = false
    @State private var onlyAdminsCanEditInfo: Bool = true

    // Sheets & Alerts
    @State private var showAddMemberSheet: Bool = false
    @State private var showLeaveGroupAlert: Bool = false
    @State private var newMemberHandle: String = ""
    @State private var alertToastMessage: String? = nil
    @State private var showAlertToast: Bool = false

    @State private var members: [GroupMemberItem] = []

    public init() {}

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Header Section
                    groupHeaderView

                    // Quick Actions Row
                    quickActionsRow

                    // Description & Invite Link
                    groupInfoCard

                    // Shared Content Summary
                    sharedContentCard

                    // Group Admin Settings
                    adminSettingsCard

                    // Members List Section
                    membersSection
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 20)
            }
            .navigationTitle("Group Info")
            .navigationBarTitleDisplayMode(.inline)
            .background(Color(.systemGroupedBackground).ignoresSafeArea())
            .alert("Group Notice", isPresented: $showAlertToast) {
                Button("OK", role: .cancel) { }
            } message: {
                Text(alertToastMessage ?? "")
            }
            .alert("Leave Group?", isPresented: $showLeaveGroupAlert) {
                Button("Cancel", role: .cancel) { }
                Button("Leave", role: .destructive) {
                    alertToastMessage = "You have left \(groupName)."
                    showAlertToast = true
                }
            } message: {
                Text("You will no longer receive messages or updates from this group.")
            }
            .sheet(isPresented: $showAddMemberSheet) {
                addMemberSheet
            }
        }
    }

    // MARK: - Subviews

    private var groupHeaderView: some View {
        VStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(LinearGradient(colors: [.indigo, .blue], startPoint: .topLeading, endPoint: .bottomTrailing))
                    .frame(width: 90, height: 90)

                Image(systemName: "person.3.fill")
                    .font(.system(size: 42))
                    .foregroundColor(.white)
            }

            VStack(spacing: 4) {
                Text(groupName)
                    .font(.title2.bold())
                    .multilineTextAlignment(.center)

                Text("\(members.count) Members • \(members.filter(\.isOnline).count) Online")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
        }
    }

    private var quickActionsRow: some View {
        HStack(spacing: 16) {
            actionIconButton(
                icon: isMuted ? "bell.slash.fill" : "bell.fill",
                label: isMuted ? "Unmute" : "Mute",
                color: isMuted ? .orange : .blue
            ) {
                isMuted.toggle()
            }

            actionIconButton(icon: "magnifyingglass", label: "Search", color: .blue) {
                alertToastMessage = "Search within group initiated."
                showAlertToast = true
            }

            actionIconButton(icon: "link", label: "Share Link", color: .blue) {
                alertToastMessage = "Invite link copied to clipboard: https://tj.app/g/core-mobile"
                showAlertToast = true
            }

            actionIconButton(icon: "rectangle.portrait.and.arrow.right", label: "Leave", color: .red) {
                showLeaveGroupAlert = true
            }
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(16)
    }

    private func actionIconButton(icon: String, label: String, color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.system(size: 20))
                    .foregroundColor(color)
                    .frame(width: 44, height: 44)
                    .background(color.opacity(0.12))
                    .clipShape(Circle())

                Text(label)
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(.primary)
            }
            .frame(maxWidth: .infinity)
        }
    }

    private var groupInfoCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("ABOUT GROUP")
                .font(.caption.bold())
                .foregroundColor(.secondary)

            Text(groupDescription)
                .font(.body)
                .foregroundColor(.primary)

            Divider()

            HStack {
                Label("Invite Link", systemImage: "link.circle.fill")
                    .font(.subheadline.bold())
                    .foregroundColor(.blue)
                Spacer()
                Text("tj.app/g/core-mobile")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(16)
    }

    private var sharedContentCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("SHARED MEDIA & FILES")
                .font(.caption.bold())
                .foregroundColor(.secondary)

            HStack(spacing: 12) {
                sharedItemBadge(icon: "photo.stack.fill", count: "142 Photos", color: .purple)
                sharedItemBadge(icon: "doc.fill", count: "28 Docs", color: .blue)
                sharedItemBadge(icon: "link", count: "56 Links", color: .teal)
            }
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(16)
    }

    private func sharedItemBadge(icon: String, count: String, color: Color) -> some View {
        HStack(spacing: 6) {
            Image(systemName: icon)
                .foregroundColor(color)
            Text(count)
                .font(.caption.bold())
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(color.opacity(0.12))
        .cornerRadius(10)
    }

    private var adminSettingsCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("GROUP PERMISSIONS & SETTINGS")
                .font(.caption.bold())
                .foregroundColor(.secondary)

            Toggle(isOn: $onlyAdminsCanSend) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Only Admins Can Send Messages")
                        .font(.body)
                    Text("Restrict members from posting")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }

            Divider()

            Toggle(isOn: $onlyAdminsCanEditInfo) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Only Admins Edit Group Info")
                        .font(.body)
                    Text("Protect group name, icon & link")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(16)
    }

    private var membersSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("MEMBERS (\(members.count))")
                    .font(.caption.bold())
                    .foregroundColor(.secondary)

                Spacer()

                Button(action: { showAddMemberSheet = true }) {
                    Label("Add Member", systemImage: "person.badge.plus")
                        .font(.caption.bold())
                        .foregroundColor(.blue)
                }
            }

            VStack(spacing: 0) {
                ForEach(members) { member in
                    HStack(spacing: 12) {
                        ZStack(alignment: .bottomTrailing) {
                            Circle()
                                .fill(Color.blue.opacity(0.2))
                                .frame(width: 42, height: 42)
                                .overlay(
                                    Text(String(member.name.prefix(1)))
                                        .font(.headline.bold())
                                        .foregroundColor(.blue)
                                )

                            if member.isOnline {
                                Circle()
                                    .fill(Color.green)
                                    .frame(width: 10, height: 10)
                                    .overlay(Circle().stroke(Color.white, lineWidth: 1.5))
                            }
                        }

                        VStack(alignment: .leading, spacing: 2) {
                            Text(member.name)
                                .font(.subheadline.weight(.semibold))

                            Text(member.handle)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }

                        Spacer()

                        // Role Badge
                        Text(member.role.rawValue)
                            .font(.caption2.bold())
                            .foregroundColor(member.role.badgeColor)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(member.role.badgeColor.opacity(0.12))
                            .cornerRadius(8)

                        // Action Menu
                        Menu {
                            Button(action: {
                                if let idx = members.firstIndex(where: { $0.id == member.id }) {
                                    members[idx].role = .admin
                                }
                            }) { Label("Make Admin", systemImage: "shield.fill") }

                            Button(role: .destructive, action: {
                                members.removeAll { $0.id == member.id }
                            }) { Label("Remove from Group", systemImage: "person.badge.minus") }
                        } label: {
                            Image(systemName: "ellipsis")
                                .foregroundColor(.gray)
                                .padding(.leading, 4)
                        }
                    }
                    .padding()

                    if member.id != members.last?.id {
                        Divider().padding(.leading, 16)
                    }
                }
            }
            .background(Color(.secondarySystemGroupedBackground))
            .cornerRadius(16)
        }
    }

    private var addMemberSheet: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Text("Add Member to Group")
                    .font(.title2.bold())
                    .padding(.top)

                TextField("Enter handle (e.g. @username)", text: $newMemberHandle)
                    .padding()
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)
                    .padding(.horizontal)

                Button(action: {
                    if !newMemberHandle.isEmpty {
                        members.append(GroupMemberItem(
                            id: UUID().uuidString,
                            name: newMemberHandle.replacingOccurrences(of: "@", with: "").capitalized,
                            handle: newMemberHandle.hasPrefix("@") ? newMemberHandle : "@\(newMemberHandle)",
                            role: .member,
                            isOnline: true
                        ))
                        newMemberHandle = ""
                        showAddMemberSheet = false
                    }
                }) {
                    Text("Add to Group")
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .cornerRadius(12)
                        .padding(.horizontal)
                }

                Spacer()
            }
            .navigationBarItems(trailing: Button("Cancel") { showAddMemberSheet = false })
        }
        .presentationDetents([.height(280)])
    }
}

#Preview {
    GroupDetailView()
}
