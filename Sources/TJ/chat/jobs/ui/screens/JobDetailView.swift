import SwiftUI

public struct JobDetailView: View {
    public let job: Job
    public var onBack: () -> Void
    public var onApply: () -> Void
    public var onExternalApplyClick: (String) -> Void = { _ in }
    public var onNavigateToProfile: () -> Void = {}
    public var onReportJob: (String, String) -> Void = { _, _ in }

    @State private var showReportDialog = false
    @State private var selectedReason = "Fraudulent or misleading"

    private let reportReasons = [
        "Fraudulent or misleading",
        "Discriminatory content",
        "Spam or scam",
        "Inappropriate content",
        "Other"
    ]

    public init(
        job: Job,
        onBack: @escaping () -> Void,
        onApply: @escaping () -> Void,
        onExternalApplyClick: @escaping (String) -> Void = { _ in },
        onNavigateToProfile: @escaping () -> Void = {},
        onReportJob: @escaping (String, String) -> Void = { _, _ in }
    ) {
        self.job = job
        self.onBack = onBack
        self.onApply = onApply
        self.onExternalApplyClick = onExternalApplyClick
        self.onNavigateToProfile = onNavigateToProfile
        self.onReportJob = onReportJob
    }

    private var isAppliedInternal: Bool {
        job.isApplied || job.hasAppliedInternal
    }

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // Header banner
                ZStack(alignment: .bottomLeading) {
                    LinearGradient(
                        colors: [job.logoColor, Color(.systemGroupedBackground)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: 180)

                    HStack(spacing: 20) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 16)
                                .fill(Color(.systemBackground))
                                .frame(width: 72, height: 72)
                                .shadow(radius: 4)

                            Text(String(job.companyName.prefix(1)).uppercased())
                                .font(.system(size: 32, weight: .heavy))
                                .foregroundColor(job.logoColor)
                        }

                        VStack(alignment: .leading, spacing: 4) {
                            Text(job.title)
                                .font(.system(size: 22, weight: .heavy))
                                .foregroundColor(.primary)

                            Text(job.companyName)
                                .font(.system(size: 16, weight: .medium))
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(24)
                }

                // Details Content
                VStack(alignment: .leading, spacing: 20) {
                    if job.isExternal {
                        HStack(spacing: 8) {
                            Image(systemName: "globe")
                                .foregroundColor(.orange)
                            Text("Partner Listing • Listed via \(job.sourcePartner ?? "External Source")")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(.orange)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.orange.opacity(0.15))
                        .cornerRadius(8)
                    }

                    HStack(spacing: 12) {
                        JobDetailChipView(systemName: "location.fill", label: job.location)
                        JobDetailChipView(systemName: "briefcase.fill", label: job.jobType)
                    }

                    if !job.salaryRange.isEmpty {
                        JobDetailChipView(systemName: "dollarsign.circle.fill", label: job.salaryRange)
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        Text("Description")
                            .font(.system(size: 18, weight: .bold))

                        Text(job.description)
                            .font(.system(size: 15))
                            .foregroundColor(.secondary)
                            .lineSpacing(6)
                    }
                    .padding(.top, 12)

                    if !job.requirements.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Requirements")
                                .font(.system(size: 18, weight: .bold))

                            ForEach(job.requirements, id: \.self) { req in
                                HStack(alignment: .top, spacing: 8) {
                                    Text("•")
                                        .foregroundColor(.blue)
                                    Text(req)
                                        .font(.system(size: 14))
                                        .foregroundColor(.secondary)
                                }
                            }
                        }
                        .padding(.top, 12)
                    }

                    if job.isExternal {
                        HStack(alignment: .top, spacing: 8) {
                            Image(systemName: "info.circle")
                                .foregroundColor(.secondary)
                            Text("Affiliate Disclosure: Listing provided via \(job.sourcePartner ?? "External Source"). Tapping 'Apply' redirects to employer website.")
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                        }
                        .padding(12)
                        .background(Color.secondary.opacity(0.1))
                        .cornerRadius(8)
                        .padding(.top, 12)
                    }

                    // Apply Button
                    Button(action: {
                        if job.isExternal {
                            onExternalApplyClick(job.id)
                            if let url = job.trackingApplyUrl, !url.isEmpty {
                                CustomTabsHelper.launchUrl(url)
                            }
                        } else {
                            onApply()
                        }
                    }) {
                        HStack(spacing: 8) {
                            if job.isExternal {
                                Image(systemName: "arrow.up.right.square")
                                Text(job.hasClickedExternal ? "Open Again (\(job.sourcePartner ?? "Partner Site"))" : "Apply on \(job.sourcePartner ?? "Partner Site")")
                                    .fontWeight(.bold)
                            } else {
                                Text(isAppliedInternal ? "Applied" : "Apply Now")
                                    .fontWeight(.bold)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(job.isExternal ? Color.orange : (isAppliedInternal ? Color.secondary.opacity(0.3) : Color.blue))
                        .foregroundColor(.white)
                        .cornerRadius(16)
                    }
                    .disabled(!job.isExternal && isAppliedInternal)
                    .padding(.top, 16)
                }
                .padding(20)
            }
        }
        .navigationTitle("Job Details")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                }
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button(role: .destructive, action: {
                        showReportDialog = true
                    }) {
                        Label("Report Job Listing", systemImage: "exclamationmark.triangle")
                    }
                } label: {
                    Image(systemName: "ellipsis")
                }
            }
        }
        .confirmationDialog("Report Job Listing", isPresented: $showReportDialog, titleVisibility: .visible) {
            ForEach(reportReasons, id: \.self) { reason in
                Button(reason) {
                    onReportJob(job.id, reason)
                }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Why are you reporting this job listing?")
        }
    }
}

public struct JobDetailChipView: View {
    public let systemName: String
    public let label: String

    public init(systemName: String, label: String) {
        self.systemName = systemName
        self.label = label
    }

    public var body: some View {
        HStack(spacing: 8) {
            Image(systemName: systemName)
                .font(.system(size: 14))
                .foregroundColor(.blue)
            Text(label)
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(.primary)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(Color.primary.opacity(0.05))
        .cornerRadius(8)
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color.primary.opacity(0.1), lineWidth: 1)
        )
    }
}
