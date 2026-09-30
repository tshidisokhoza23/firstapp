import Foundation
import Combine

public protocol JobRepository {
    func getJobs(cursor: String?, limit: Int) -> AsyncStream<Resource<[Job]>>
    func searchJobs(query: String) -> AsyncStream<Resource<[Job]>>
    func applyForJob(jobId: String) -> AsyncStream<Resource<String>>
    func saveJob(jobId: String) async -> Resource<Bool>
    func unsaveJob(jobId: String) async -> Resource<Bool>
    func getJobApplications() -> AsyncStream<Resource<[JobApplication]>>
    func withdrawApplication(applicationId: String) async -> Resource<Bool>
    func getJobProfile() async -> Resource<JobProfileData>
    func updateJobProfile(profile: JobProfileData) async -> Resource<Bool>
    func createJob(title: String, description: String, category: String, skills: [String], durationDays: Int) async -> Resource<Job>
    func markClickedExternalLocally(jobId: String) async
}

public final class JobRepositoryImpl: JobRepository, @unchecked Sendable {
    private let jobApi: JobApiProtocol
    private let localStore: JobLocalStore

    public init(jobApi: JobApiProtocol = JobApi(), localStore: JobLocalStore = JobLocalStore()) {
        self.jobApi = jobApi
        self.localStore = localStore
    }

    public func getJobs(cursor: String? = nil, limit: Int = 50) -> AsyncStream<Resource<[Job]>> {
        AsyncStream { continuation in
            Task {
                continuation.yield(.loading(nil))
                do {
                    let response = try await jobApi.getJobs(page: 1, cursor: cursor, limit: limit)
                    let models = response.jobs.map { $0.toModel() }
                    await localStore.insertJobs(models.map { $0.toEntity() })
                    continuation.yield(.success(models))
                } catch {
                    let cached = await localStore.getJobsList()
                    continuation.yield(.error("Network error: \(error.localizedDescription)", cached.map { $0.toModel() }))
                }
                continuation.finish()
            }
        }
    }

    public func searchJobs(query: String) -> AsyncStream<Resource<[Job]>> {
        AsyncStream { continuation in
            Task {
                continuation.yield(.loading(nil))
                do {
                    let response = try await jobApi.search(query: query)
                    let models = response.jobs.map { $0.toModel() }
                    continuation.yield(.success(models))
                } catch {
                    let cached = await localStore.searchJobs(query: query)
                    continuation.yield(.error("Search failed: \(error.localizedDescription)", cached.map { $0.toModel() }))
                }
                continuation.finish()
            }
        }
    }

    public func applyForJob(jobId: String) -> AsyncStream<Resource<String>> {
        AsyncStream { continuation in
            Task {
                continuation.yield(.loading(nil))
                do {
                    let response = try await jobApi.applyForJob(jobId: jobId)
                    await localStore.updateAppliedStatus(jobId: jobId, isApplied: true)
                    continuation.yield(.success(response.conversationId))
                } catch {
                    continuation.yield(.error("Application failed: \(error.localizedDescription)", nil))
                }
                continuation.finish()
            }
        }
    }

    public func markClickedExternalLocally(jobId: String) async {
        await localStore.markClickedExternalLocally(jobId: jobId)
    }

    public func saveJob(jobId: String) async -> Resource<Bool> {
        do {
            _ = try await jobApi.saveJob(jobId: jobId)
            await localStore.updateSavedStatus(jobId: jobId, isSaved: true)
            return .success(true)
        } catch {
            return .error(error.localizedDescription, nil)
        }
    }

    public func unsaveJob(jobId: String) async -> Resource<Bool> {
        do {
            _ = try await jobApi.unsaveJob(jobId: jobId)
            await localStore.updateSavedStatus(jobId: jobId, isSaved: false)
            return .success(true)
        } catch {
            return .error(error.localizedDescription, nil)
        }
    }

    public func getJobApplications() -> AsyncStream<Resource<[JobApplication]>> {
        AsyncStream { continuation in
            Task {
                continuation.yield(.loading(nil))
                do {
                    let response = try await jobApi.getJobApplications()
                    let models = response.applications.map { $0.toModel() }
                    await localStore.insertApplications(models.map { $0.toEntity() })
                    continuation.yield(.success(models))
                } catch {
                    let cached = await localStore.getApplicationsList()
                    continuation.yield(.error("Failed to load applications: \(error.localizedDescription)", cached.map { $0.toModel() }))
                }
                continuation.finish()
            }
        }
    }

    public func withdrawApplication(applicationId: String) async -> Resource<Bool> {
        do {
            _ = try await jobApi.withdrawApplication(applicationId: applicationId)
            await localStore.deleteApplication(applicationId: applicationId)
            return .success(true)
        } catch {
            return .error(error.localizedDescription, nil)
        }
    }

    public func getJobProfile() async -> Resource<JobProfileData> {
        do {
            let profile = try await jobApi.getJobProfile()
            return .success(profile)
        } catch {
            return .error(error.localizedDescription, nil)
        }
    }

    public func updateJobProfile(profile: JobProfileData) async -> Resource<Bool> {
        do {
            _ = try await jobApi.updateJobProfile(profile: profile)
            return .success(true)
        } catch {
            return .error(error.localizedDescription, nil)
        }
    }

    public func createJob(title: String, description: String, category: String, skills: [String], durationDays: Int) async -> Resource<Job> {
        do {
            let req = CreateJobRequest(
                title: title,
                description: description,
                job_category: category,
                required_skills: skills,
                duration_days: durationDays
            )
            let apiJob = try await jobApi.createJob(request: req)
            let model = apiJob.toModel()
            await localStore.insertJobs([model.toEntity()])
            return .success(model)
        } catch {
            return .error(error.localizedDescription, nil)
        }
    }
}
