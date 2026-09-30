import Foundation
import Combine

public actor JobLocalStore {
    public static let shared = JobLocalStore()

    private var jobs: [String: JobEntity] = [:]
    private var applications: [String: JobApplicationEntity] = [:]

    private let jobsSubject = CurrentValueSubject<[JobEntity], Never>([])
    private let applicationsSubject = CurrentValueSubject<[JobApplicationEntity], Never>([])

    public init() {}

    // MARK: - Jobs
    public func getJobs() -> AnyPublisher<[JobEntity], Never> {
        jobsSubject.eraseToAnyPublisher()
    }

    public func getJobsList() -> [JobEntity] {
        Array(jobs.values.sorted { $0.postedTime > $1.postedTime })
    }

    public func insertJobs(_ newJobs: [JobEntity]) {
        for job in newJobs {
            jobs[job.id] = job
        }
        notifyJobsChanged()
    }

    public func updateAppliedStatus(jobId: String, isApplied: Bool) {
        if var job = jobs[jobId] {
            job.isApplied = isApplied
            job.hasAppliedInternal = isApplied
            jobs[jobId] = job
            notifyJobsChanged()
        }
    }

    public func updateSavedStatus(jobId: String, isSaved: Bool) {
        if var job = jobs[jobId] {
            job.isSaved = isSaved
            jobs[jobId] = job
            notifyJobsChanged()
        }
    }

    public func markClickedExternalLocally(jobId: String) {
        if var job = jobs[jobId] {
            job.hasClickedExternal = true
            jobs[jobId] = job
            notifyJobsChanged()
        }
    }

    public func clearJobs() {
        jobs.removeAll()
        notifyJobsChanged()
    }

    public func searchJobs(query: String) -> [JobEntity] {
        let q = query.lowercased()
        return jobs.values
            .filter {
                $0.title.lowercased().contains(q) ||
                $0.companyName.lowercased().contains(q) ||
                $0.description.lowercased().contains(q)
            }
            .sorted { $0.postedTime > $1.postedTime }
    }

    private func notifyJobsChanged() {
        let sorted = Array(jobs.values.sorted { $0.postedTime > $1.postedTime })
        jobsSubject.send(sorted)
    }

    // MARK: - Job Applications
    public func getApplications() -> AnyPublisher<[JobApplicationEntity], Never> {
        applicationsSubject.eraseToAnyPublisher()
    }

    public func getApplicationsList() -> [JobApplicationEntity] {
        Array(applications.values.sorted { $0.appliedDate > $1.appliedDate })
    }

    public func insertApplications(_ newApplications: [JobApplicationEntity]) {
        for app in newApplications {
            applications[app.id] = app
        }
        notifyApplicationsChanged()
    }

    public func clearApplications() {
        applications.removeAll()
        notifyApplicationsChanged()
    }

    public func deleteApplication(applicationId: String) {
        applications.removeValue(forKey: applicationId)
        notifyApplicationsChanged()
    }

    private func notifyApplicationsChanged() {
        let sorted = Array(applications.values.sorted { $0.appliedDate > $1.appliedDate })
        applicationsSubject.send(sorted)
    }
}
