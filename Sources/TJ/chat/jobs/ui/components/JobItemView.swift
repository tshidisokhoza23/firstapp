import SwiftUI

public struct JobItemView: View {
    public let job: Job
    public var onClick: () -> Void = {}

    public init(job: Job, onClick: @escaping () -> Void = {}) {
        self.job = job
        self.onClick = onClick
    }

    public var body: some View {
        Button(action: onClick) {
            HStack(alignment: .top, spacing: 16) {
                // Company Logo
                ZStack {
                    RoundedRectangle(cornerRadius: 12)
                        .fill(job.logoColor)
                        .frame(width: 56, height: 56)

                    if let logoUrl = job.companyLogoUrl, let url = URL(string: logoUrl) {
                        AsyncImage(url: url) { image in
                            image
                                .resizable()
                                .scaledToFit()
                        } placeholder: {
                            ProgressView()
                        }
                        .frame(width: 56, height: 56)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    } else {
                        Text(String(job.companyName.prefix(1)).uppercased())
                            .foregroundColor(.white)
                            .font(.system(size: 22, weight: .heavy))
                    }
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(job.title)
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(.primary)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)

                    HStack(spacing: 4) {
                        Text(job.companyName)
                            .font(.system(size: 15))
                            .foregroundColor(.primary.opacity(0.9))

                        if job.isVerified {
                            Image(systemName: "checkmark.seal.fill")
                                .font(.system(size: 14))
                                .foregroundColor(.blue)
                        }

                        if job.isExternal {
                            Text(job.sourcePartner ?? "Partner")
                                .font(.system(size: 10, weight: .bold))
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.orange.opacity(0.2))
                                .foregroundColor(.orange)
                                .cornerRadius(4)
                        }
                    }

                    Text(job.location)
                        .font(.system(size: 14))
                        .foregroundColor(.secondary)

                    HStack(spacing: 8) {
                        Text(job.jobType)
                            .font(.system(size: 12, weight: .medium))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(Color.primary.opacity(0.05))
                            .foregroundColor(.secondary)
                            .cornerRadius(4)

                        if job.isExternal {
                            Image(systemName: "arrow.up.right.square")
                                .font(.system(size: 14))
                                .foregroundColor(.orange)
                        }
                    }
                    .padding(.top, 2)

                    if job.postedTime > 0 {
                        Text(formatPostedTime(job.postedTime))
                            .font(.system(size: 12))
                            .foregroundColor(.secondary.opacity(0.8))
                            .padding(.top, 2)
                    }
                }
                Spacer(minLength: 0)
            }
            .padding(.vertical, 12)
            .padding(.horizontal, 16)
        }
        .buttonStyle(.plain)
    }

    private func formatPostedTime(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000.0)
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .full
        return formatter.localizedString(for: date, relativeTo: Date())
    }
}
