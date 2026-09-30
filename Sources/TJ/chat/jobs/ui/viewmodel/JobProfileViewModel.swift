import Foundation
import Combine

@MainActor
public class JobProfileViewModel: ObservableObject {
    @Published public var profile: Resource<JobProfileData> = .loading(nil)
    @Published public var saveResult: Resource<Bool>? = nil

    private let repository: JobRepository

    public init(repository: JobRepository = JobRepositoryImpl()) {
        self.repository = repository
        fetchProfile()
    }

    public func fetchProfile() {
        Task {
            for await resource in repository.getJobProfile() {
                self.profile = resource
            }
        }
    }

    public func updateProfile(_ profile: JobProfileData) {
        Task {
            for await result in repository.updateJobProfile(profile: profile) {
                self.saveResult = result
            }
        }
    }

    public func resetState() {
        self.saveResult = nil
    }
}
