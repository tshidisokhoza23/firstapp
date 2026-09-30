import SwiftUI

public struct JobListView: View {
    public let jobs: [Job]
    public var topBarHeight: CGFloat = 0
    public var onJobClick: (Job) -> Void

    public init(jobs: [Job], topBarHeight: CGFloat = 0, onJobClick: @escaping (Job) -> Void) {
        self.jobs = jobs
        self.topBarHeight = topBarHeight
        self.onJobClick = onJobClick
    }

    public var body: some View {
        if jobs.isEmpty {
            VStack(spacing: 8) {
                Text("No jobs yet")
                    .font(.headline)
                    .foregroundColor(.primary)
                Text("Open roles show up here. Use the button to post one.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding(.horizontal, 32)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            ScrollView {
                VStack(spacing: 0) {
                    Spacer().frame(height: topBarHeight)
                    LazyVStack(spacing: 0) {
                        ForEach(jobs) { job in
                            JobItemView(job: job, onClick: {
                                onJobClick(job)
                            })
                            Divider()
                        }
                    }
                }
                .padding(.bottom, 160)
            }
        }
    }
}
