import Foundation
import Combine

public protocol JobRepository {
    func getJobs(page: Int) -> AsyncStream<Resource<[Job]>>
    func applyForJob(jobId: String) -> AsyncStream<Resource<String>>
    func markClickedExternalLocally(jobId: String) async
    func getJobApplications() -> AsyncStream<Resource<[JobApplication]>>
    func getJobProfile() -> AsyncStream<Resource<JobProfileData>>
    func withdrawApplication(applicationId: String) -> AsyncStream<Resource<Bool>>
    func updateJobProfile(profile: JobProfileData) -> AsyncStream<Resource<Bool>>
    func createJob(title: String, description: String, category: String, skills: [String], durationDays: Int) -> AsyncStream<Resource<Job>>
    func searchJobs(query: String) -> AsyncStream<Resource<[Job]>>
}

public class JobRepositoryImpl: JobRepository {
    private let jobApi: JobApiProtocol
    private let localStore: JobLocalStore

    public init(jobApi: JobApiProtocol = JobApi(), localStore: JobLocalStore = JobLocalStore.shared) {
        self.jobApi = jobApi
        self.localStore = localStore
    }

    public func getJobs(page: Int = 1) -> AsyncStream<Resource<[Job]>> {
        AsyncStream { continuation in
            Task {
                let cachedEntities = await localStore.getJobsSnapshot()
                let cached = cachedEntities.map { $0.toModel() }
                if !cached.isEmpty {
                    continuation.yield(.loading(cached))
                } else {
                    continuation.yield(.loading(nil))
                }

                do {
                    let response = try await jobApi.getJobs(page: page, cursor: nil, limit: 50)
                    let apiJobs = response.jobs
                    let jobs = apiJobs.map { $0.toModel() }
                    if page == 1 {
                        await localStore.clearJobs()
                    }
                    await localStore.insertJobs(jobs.map { $0.toEntity() })
                } catch {
                    print("JobRepo: getJobs failed with error: \(error)")
                }

                let updatedEntities = await localStore.getJobsSnapshot()
                let models = updatedEntities.map { $0.toModel() }
                continuation.yield(.success(models))
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
                    continuation.yield(.success(response.conversation_id))
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

    public func getJobApplications() -> AsyncStream<Resource<[JobApplication]>> {
        AsyncStream { continuation in
            Task {
                let cachedEntities = await localStore.getApplicationsSnapshot()
                let cached = cachedEntities.map { $0.toModel() }
                if !cached.isEmpty {
                    continuation.yield(.loading(cached))
                } else {
                    continuation.yield(.loading(nil))
                }

                do {
                    let response = try await jobApi.getJobApplications()
                    let applications = response.applications.map { $0.toModel() }
                    await localStore.clearApplications()
                    await localStore.insertApplications(applications.map { $0.toEntity() })
                } catch {
                    print("JobRepo: getJobApplications failed: \(error)")
                }

                let updated = await localStore.getApplicationsSnapshot()
                continuation.yield(.success(updated.map { $0.toModel() }))
                continuation.finish()
            }
        }
    }

    public func getJobProfile() -> AsyncStream<Resource<JobProfileData>> {
        AsyncStream { continuation in
            Task {
                continuation.yield(.loading(nil))
                do {
                    let profile = try await jobApi.getJobProfile()
                    continuation.yield(.success(profile))
                } catch {
                    continuation.yield(.error("Failed to fetch profile: \(error.localizedDescription)", nil))
                }
                continuation.finish()
            }
        }
    }

    public func withdrawApplication(applicationId: String) -> AsyncStream<Resource<Bool>> {
        AsyncStream { continuation in
            Task {
                continuation.yield(.loading(nil))
                do {
                    _ = try await jobApi.withdrawApplication(applicationId: applicationId)
                    await localStore.deleteApplication(applicationId: applicationId)
                    continuation.yield(.success(true))
                } catch {
                    continuation.yield(.error("Withdrawal failed", nil))
                }
                continuation.finish()
            }
        }
    }

    public func updateJobProfile(profile: JobProfileData) -> AsyncStream<Resource<Bool>> {
        AsyncStream { continuation in
            Task {
                continuation.yield(.loading(nil))
                do {
                    _ = try await jobApi.updateJobProfile(profile: profile)
                    continuation.yield(.success(true))
                } catch {
                    continuation.yield(.error("Could not save profile (\(error.localizedDescription))", nil))
                }
                continuation.finish()
            }
        }
    }

    public func createJob(title: String, description: String, category: String, skills: [String], durationDays: Int) -> AsyncStream<Resource<Job>> {
        AsyncStream { continuation in
            Task {
                continuation.yield(.loading(nil))
                do {
                    let req = CreateJobRequest(
                        title: title,
                        description: description,
                        job_category: category.isEmpty ? "General" : category,
                        required_skills: skills,
                        duration_days: durationDays,
                        category: category.isEmpty ? "General" : category,
                        skills: skills
                    )
                    let apiJob = try await jobApi.createJob(request: req)
                    let job = apiJob.toModel()
                    await localStore.insertJobs([job.toEntity()])
                    continuation.yield(.success(job))
                } catch {
                    continuation.yield(.error("Network error: \(error.localizedDescription)", nil))
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
                    let response = try await jobApi.getJobs(page: 1, cursor: nil, limit: 50)
                    let filtered = response.jobs.filter {
                        $0.title.localizedCaseInsensitiveContains(query) ||
                        $0.company_name.localizedCaseInsensitiveContains(query)
                    }.map { $0.toModel() }
                    continuation.yield(.success(filtered))
                } catch {
                    continuation.yield(.error(error.localizedDescription, nil))
                }
                continuation.finish()
            }
        }
    }
}
