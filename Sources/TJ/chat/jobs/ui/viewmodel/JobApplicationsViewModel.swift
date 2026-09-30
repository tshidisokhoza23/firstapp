import Foundation
import Combine

@MainActor
public class JobApplicationsViewModel: ObservableObject {
    @Published public var applications: Resource<[JobApplication]> = .loading(nil)

    private let repository: JobRepository

    public init(repository: JobRepository? = nil) {
        self.repository = repository ?? JobRepositoryImpl()
        fetchApplications()
    }

    public func fetchApplications() {
        Task {
            for await resource in repository.getJobApplications() {
                self.applications = resource
            }
        }
    }

    public func withdrawApplication(applicationId: String) {
        Task {
            let result = await repository.withdrawApplication(applicationId: applicationId)
            if case .success = result {
                fetchApplications()
            }
        }
    }
}
