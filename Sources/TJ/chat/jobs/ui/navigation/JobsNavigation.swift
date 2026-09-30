import SwiftUI

public enum JobsRoute: Hashable {
    case jobProfile
    case postJob
    case applications
    case jobDetail(jobId: String)
    case chat(chatId: String, jobId: String?, jobTitle: String?, companyName: String?)
    case editWorkExperience(experienceId: String?)
}

public struct JobsNavigationStack<Content: View>: View {
    @State private var path = NavigationPath()
    private let content: Content

    public init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    public var body: some View {
        NavigationStack(path: $path) {
            content
                .navigationDestination(for: JobsRoute.self) { route in
                    switch route {
                    case .jobProfile:
                        JobProfileView(
                            onClose: { path.removeLast() },
                            onNavigateToEditExperience: { expId in
                                path.append(JobsRoute.editWorkExperience(experienceId: expId))
                            }
                        )
                    case .postJob:
                        PostJobView(onBack: { path.removeLast() })
                    case .applications:
                        JobApplicationsView(
                            onBack: { path.removeLast() },
                            onViewJobDetails: { jobId in
                                path.append(JobsRoute.jobDetail(jobId: jobId))
                            },
                            onContactRecruiter: { chatId in
                                path.append(JobsRoute.chat(chatId: chatId, jobId: nil, jobTitle: nil, companyName: nil))
                            },
                            onSetReminder: { _ in }
                        )
                    case .jobDetail(let jobId):
                        JobDetailDestinationView(jobId: jobId, path: $path)
                    case .chat(let chatId, _, _, let jobTitle):
                        Text("Chat Screen: \(chatId) for \(jobTitle ?? "")")
                    case .editWorkExperience(let expId):
                        Text("Edit Work Experience: \(expId ?? "New")")
                    }
                }
        }
    }
}

private struct JobDetailDestinationView: View {
    let jobId: String
    @Binding var path: NavigationPath
    @StateObject private var viewModel = JobDetailViewModel()

    var body: some View {
        Group {
            if case .success(let job) = viewModel.job {
                JobDetailView(
                    job: job,
                    onBack: { path.removeLast() },
                    onApply: {
                        viewModel.applyForJob(jobId: job.id, jobTitle: job.title, companyName: job.companyName)
                    },
                    onExternalApplyClick: { id in
                        viewModel.markClickedExternal(jobId: id)
                    }
                )
            } else {
                ProgressView("Loading Job...")
            }
        }
        .onAppear {
            viewModel.loadJob(jobId: jobId)
        }
        .onChange(of: viewModel.applyResult) { result in
            if case .success(let conversationId) = result {
                let context = viewModel.lastApplyContext
                path.append(JobsRoute.chat(
                    chatId: conversationId,
                    jobId: context?.jobId,
                    jobTitle: context?.jobTitle,
                    companyName: context?.companyName
                ))
                viewModel.resetApplyResult()
            }
        }
    }
}
