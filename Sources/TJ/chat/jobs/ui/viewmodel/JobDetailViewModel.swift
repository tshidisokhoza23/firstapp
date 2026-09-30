import Foundation
import Combine

@MainActor
public class JobDetailViewModel: ObservableObject {
    public struct ApplyContext: Equatable {
        public let jobId: String
        public let jobTitle: String
        public let companyName: String

        public init(jobId: String, jobTitle: String, companyName: String) {
            self.jobId = jobId
            self.jobTitle = jobTitle
            self.companyName = companyName
        }
    }

    @Published public var job: Resource<Job> = .loading(nil)
    @Published public var applyResult: Resource<String>? = nil
    @Published public var lastApplyContext: ApplyContext? = nil

    private let repository: JobRepository

    public init(repository: JobRepository = JobRepositoryImpl()) {
        self.repository = repository
    }

    public func loadJob(jobId: String) {
        Task {
            for await resource in repository.getJobs(cursor: nil, limit: 50) {
                if case .success(let jobs) = resource {
                    if let found = jobs.first(where: { $0.id == jobId }) {
                        self.job = .success(found)
                    } else {
                        self.job = .error("Job not found", nil)
                    }
                } else if case .error(let message, _) = resource {
                    self.job = .error(message, nil)
                }
            }
        }
    }

    public func applyForJob(jobId: String, jobTitle: String, companyName: String) {
        Task {
            self.lastApplyContext = ApplyContext(jobId: jobId, jobTitle: jobTitle, companyName: companyName)
            for await result in repository.applyForJob(jobId: jobId) {
                self.applyResult = result
            }
        }
    }

    public func markClickedExternal(jobId: String) {
        Task {
            await repository.markClickedExternalLocally(jobId: jobId)
        }
    }

    public func resetApplyResult() {
        self.applyResult = nil
    }
}
