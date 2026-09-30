import SwiftUI

public struct JobApplicationsView: View {
    public var onBack: () -> Void
    public var onViewJobDetails: (String) -> Void
    public var onContactRecruiter: (String) -> Void
    public var onSetReminder: (JobApplication) -> Void

    @StateObject private var viewModel: JobApplicationsViewModel

    @MainActor public init(
        onBack: @escaping () -> Void,
        onViewJobDetails: @escaping (String) -> Void,
        onContactRecruiter: @escaping (String) -> Void,
        onSetReminder: @escaping (JobApplication) -> Void,
        viewModel: JobApplicationsViewModel = JobApplicationsViewModel()
    ) {
        self.onBack = onBack
        self.onViewJobDetails = onViewJobDetails
        self.onContactRecruiter = onContactRecruiter
        self.onSetReminder = onSetReminder
        self._viewModel = StateObject(wrappedValue: viewModel)
    }

    public var body: some View {
        NavigationStack {
            Group {
                let apps = viewModel.applications.data ?? []
                if apps.isEmpty {
                    VStack {
                        Text("No applications yet")
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List {
                        ForEach(apps) { application in
                            ApplicationItemView(
                                application: application,
                                onViewJobDetails: { onViewJobDetails(application.jobId) },
                                onWithdrawApplication: { viewModel.withdrawApplication(applicationId: application.id) },
                                onContactRecruiter: { onContactRecruiter(application.conversationId ?? application.jobId) },
                                onSetReminder: { onSetReminder(application) }
                            )
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Application History")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                    }
                }
            }
        }
    }
}

public struct ApplicationItemView: View {
    public let application: JobApplication
    public var onViewJobDetails: () -> Void
    public var onWithdrawApplication: () -> Void
    public var onContactRecruiter: () -> Void
    public var onSetReminder: () -> Void

    public var body: some View {
        HStack(alignment: .center, spacing: 12) {
            ZStack {
                Circle()
                    .fill(application.logoColor)
                    .frame(width: 48, height: 48)

                Image(systemName: "briefcase.fill")
                    .foregroundColor(.white)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(application.jobTitle)
                    .font(.system(size: 16, weight: .bold))
                Text("\(application.companyName) • \(application.location)")
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
                Text("Applied on \(formatAppliedDate(application.appliedDate))")
                    .font(.system(size: 12))
                    .foregroundColor(.secondary.opacity(0.8))
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 8) {
                StatusBadgeView(label: application.status.label, color: application.status.color)

                Menu {
                    Button(action: onViewJobDetails) {
                        Label("View Job Details", systemImage: "info.circle")
                    }
                    Button(role: .destructive, action: onWithdrawApplication) {
                        Label("Withdraw Application", systemImage: "xmark.circle")
                    }
                    Button(action: onContactRecruiter) {
                        Label("Contact Recruiter", systemImage: "bubble.left.and.bubble.right")
                    }
                    Button(action: onSetReminder) {
                        Label("Set Reminder", systemImage: "bell")
                    }
                } label: {
                    Image(systemName: "ellipsis")
                        .foregroundColor(.secondary)
                        .padding(8)
                }
            }
        }
        .padding(.vertical, 8)
    }

    private func formatAppliedDate(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000.0)
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        return formatter.string(from: date)
    }
}

public struct StatusBadgeView: View {
    public let label: String
    public let color: Color

    public var body: some View {
        Text(label)
            .font(.system(size: 10, weight: .bold))
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(color.opacity(0.15))
            .foregroundColor(color)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(color.opacity(0.5), lineWidth: 1)
            )
    }
}
