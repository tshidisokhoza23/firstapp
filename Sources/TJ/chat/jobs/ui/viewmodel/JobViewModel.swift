import Foundation
import Combine

@MainActor
public class JobViewModel: ObservableObject {
    @Published public var jobs: Resource<[Job]> = .loading(nil)
    @Published public var jobProfile: JobProfileData? = nil
    @Published public var applications: [JobApplication] = []
    @Published public var createJobResult: Resource<Job>? = nil

    private let repository: JobRepository

    public init(repository: JobRepository? = nil) {
        self.repository = repository ?? JobRepositoryImpl()
        fetchJobs()
        fetchJobApplications()
    }

    public func fetchJobs() {
        Task {
            for await result in repository.getJobs(cursor: nil, limit: 50) {
                self.jobs = result
            }
        }
    }

    public func fetchJobApplications() {
        Task {
            for await result in repository.getJobApplications() {
                self.applications = result.data ?? []
            }
        }
    }

    public func applyForJob(jobId: String) {
        Task {
            for await result in repository.applyForJob(jobId: jobId) {
                if case .success = result {
                    fetchJobs()
                    fetchJobApplications()
                }
            }
        }
    }

    public func updateJobProfile(_ profile: JobProfileData) {
        Task {
            let result = await repository.updateJobProfile(profile: profile)
            if case .success = result {
                self.jobProfile = profile
            }
        }
    }

    public func withdrawApplication(applicationId: String) {
        applications.removeAll { $0.id == applicationId }
        Task {
            _ = await repository.withdrawApplication(applicationId: applicationId)
        }
    }

    public func createJob(title: String, description: String, category: String, skills: [String], durationDays: Int) {
        Task {
            let result = await repository.createJob(title: title, description: description, category: category, skills: skills, durationDays: durationDays)
            self.createJobResult = result
        }
    }
}
