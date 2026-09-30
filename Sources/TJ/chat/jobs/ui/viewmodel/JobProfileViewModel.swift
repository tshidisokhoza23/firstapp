import Foundation
import Combine

@MainActor
public class JobProfileViewModel: ObservableObject {
    @Published public var profile: Resource<JobProfileData> = .loading(nil)
    @Published public var saveResult: Resource<Bool>? = nil

    private let repository: JobRepository

    public init(repository: JobRepository? = nil) {
        self.repository = repository ?? JobRepositoryImpl()
        fetchProfile()
    }

    public func fetchProfile() {
        Task {
            let resource = await repository.getJobProfile()
            self.profile = resource
        }
    }

    public func updateProfile(_ profile: JobProfileData) {
        Task {
            let result = await repository.updateJobProfile(profile: profile)
            self.saveResult = result
        }
    }

    public func resetState() {
        self.saveResult = nil
    }
}
