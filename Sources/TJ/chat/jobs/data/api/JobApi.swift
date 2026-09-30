import Foundation

public struct JobsResponse: Codable {
    public let jobs: [ApiJob]
    public let next_cursor: String?
}

public struct JobApplicationsResponse: Codable {
    public let applications: [ApiJobApplication]
}

public struct CreateJobRequest: Codable {
    public let title: String
    public let description: String
    public let job_category: String
    public let required_skills: [String]
    public let duration_days: Int
    public let company: String?
    public let location_city: String?
    public let category: String?
    public let skills: [String]?

    public init(
        title: String,
        description: String,
        job_category: String,
        required_skills: [String],
        duration_days: Int,
        company: String? = nil,
        location_city: String? = nil,
        category: String? = nil,
        skills: [String]? = nil
    ) {
        self.title = title
        self.description = description
        self.job_category = job_category
        self.required_skills = required_skills
        self.duration_days = duration_days
        self.company = company
        self.location_city = location_city
        self.category = category
        self.skills = skills
    }
}

public struct ApiJob: Codable {
    public let id: String
    public let title: String
    public let company_name: String
    public let company_logo_url: String?
    public let location: String
    public let salary_range: String?
    public let job_type: String?
    public let posted_time: Int64
    public let description: String?
    public let requirements: [String]?
    public let is_verified: Bool
    public let created_by: String?
    public let is_applied: Bool
    public let is_saved: Bool
    public let is_external: Bool?
    public let source_partner: String?
    public let apply_redirect_url: String?
    public let tracking_apply_url: String?
    public let apply_url: String?
    public let has_applied_internal: Bool?
    public let has_clicked_external: Bool?

    public func getTrackingUrl() -> String? {
        apply_redirect_url ?? tracking_apply_url ?? apply_url
    }

    public func toModel() -> Job {
        Job(
            id: id,
            title: title,
            companyName: company_name,
            companyLogoUrl: company_logo_url,
            location: location,
            logoColorHex: "#2196F3",
            salaryRange: salary_range ?? "",
            jobType: job_type ?? "Full-time",
            postedTime: posted_time,
            description: description ?? "",
            requirements: requirements ?? [],
            isVerified: is_verified,
            isApplied: is_applied || (has_applied_internal ?? false),
            isSaved: is_saved,
            createdBy: created_by,
            isExternal: is_external ?? false,
            sourcePartner: source_partner,
            trackingApplyUrl: getTrackingUrl(),
            hasAppliedInternal: is_applied || (has_applied_internal ?? false),
            hasClickedExternal: has_clicked_external ?? false
        )
    }
}

public struct ApiJobApplication: Codable {
    public let id: String
    public let user_id: String
    public let job_id: String
    public let applied_date: Int64
    public var status: String
    public let title: String?
    public let company_name: String?
    public let location: String?
    public let conversation_id: String?

    public func toModel() -> JobApplication {
        JobApplication(
            id: id,
            jobId: job_id,
            jobTitle: title ?? "Job",
            companyName: company_name ?? "",
            location: location ?? "",
            appliedDate: applied_date,
            status: ApplicationStatus(rawValue: status.uppercased()) ?? .pending,
            logoColorHex: "#2196F3",
            conversationId: conversation_id
        )
    }
}

public protocol JobApiProtocol {
    func getJobs(page: Int, cursor: String?, limit: Int) async throws -> JobsResponse
    func search(query: String) async throws -> JobsResponse
    func applyForJob(jobId: String) async throws -> ConversationResponse
    func saveJob(jobId: String) async throws -> StatusResponse
    func unsaveJob(jobId: String) async throws -> StatusResponse
    func getJobApplications() async throws -> JobApplicationsResponse
    func withdrawApplication(applicationId: String) async throws -> StatusResponse
    func getJobProfile() async throws -> JobProfileData
    func updateJobProfile(profile: JobProfileData) async throws -> StatusResponse
    func createJob(request: CreateJobRequest) async throws -> ApiJob
    func getSavedJobs() async throws -> JobsResponse
}

public class JobApi: JobApiProtocol {
    private let baseURL: URL
    private let session: URLSession

    public init(baseURL: URL = URL(string: "https://api.example.com/v1/")!, session: URLSession = .shared) {
        self.baseURL = baseURL
        self.session = session
    }

    public func getJobs(page: Int = 1, cursor: String? = nil, limit: Int = 50) async throws -> JobsResponse {
        var components = URLComponents(url: baseURL.appendingPathComponent("jobs"), resolvingAgainstBaseURL: true)!
        var queryItems = [
            URLQueryItem(name: "page", value: "\(page)"),
            URLQueryItem(name: "limit", value: "\(limit)")
        ]
        if let cursor = cursor { queryItems.append(URLQueryItem(name: "cursor", value: cursor)) }
        components.queryItems = queryItems

        let (data, response) = try await session.data(from: components.url!)
        try validateResponse(response)
        return try JSONDecoder().decode(JobsResponse.self, from: data)
    }

    public func search(query: String) async throws -> JobsResponse {
        var components = URLComponents(url: baseURL.appendingPathComponent("jobs/search"), resolvingAgainstBaseURL: true)!
        components.queryItems = [URLQueryItem(name: "query", value: query)]

        let (data, response) = try await session.data(from: components.url!)
        try validateResponse(response)
        return try JSONDecoder().decode(JobsResponse.self, from: data)
    }

    public func applyForJob(jobId: String) async throws -> ConversationResponse {
        let url = baseURL.appendingPathComponent("jobs/\(jobId)/apply")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"

        let (data, response) = try await session.data(for: request)
        try validateResponse(response)
        return try JSONDecoder().decode(ConversationResponse.self, from: data)
    }

    public func saveJob(jobId: String) async throws -> StatusResponse {
        let url = baseURL.appendingPathComponent("jobs/\(jobId)/save")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"

        let (data, response) = try await session.data(for: request)
        try validateResponse(response)
        return try JSONDecoder().decode(StatusResponse.self, from: data)
    }

    public func unsaveJob(jobId: String) async throws -> StatusResponse {
        let url = baseURL.appendingPathComponent("jobs/\(jobId)/save")
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"

        let (data, response) = try await session.data(for: request)
        try validateResponse(response)
        return try JSONDecoder().decode(StatusResponse.self, from: data)
    }

    public func getJobApplications() async throws -> JobApplicationsResponse {
        let url = baseURL.appendingPathComponent("users/me/applications")
        let (data, response) = try await session.data(from: url)
        try validateResponse(response)
        return try JSONDecoder().decode(JobApplicationsResponse.self, from: data)
    }

    public func withdrawApplication(applicationId: String) async throws -> StatusResponse {
        let url = baseURL.appendingPathComponent("job-applications/\(applicationId)")
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"

        let (data, response) = try await session.data(for: request)
        try validateResponse(response)
        return try JSONDecoder().decode(StatusResponse.self, from: data)
    }

    public func getJobProfile() async throws -> JobProfileData {
        let url = baseURL.appendingPathComponent("users/me/job-profile")
        let (data, response) = try await session.data(from: url)
        try validateResponse(response)
        return try JSONDecoder().decode(JobProfileData.self, from: data)
    }

    public func updateJobProfile(profile: JobProfileData) async throws -> StatusResponse {
        let url = baseURL.appendingPathComponent("users/me/job-profile")
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(profile)

        let (data, response) = try await session.data(for: request)
        try validateResponse(response)
        return try JSONDecoder().decode(StatusResponse.self, from: data)
    }

    public func createJob(request: CreateJobRequest) async throws -> ApiJob {
        let url = baseURL.appendingPathComponent("jobs")
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = try JSONEncoder().encode(request)

        let (data, response) = try await session.data(for: req)
        try validateResponse(response)
        return try JSONDecoder().decode(ApiJob.self, from: data)
    }

    public func getSavedJobs() async throws -> JobsResponse {
        let url = baseURL.appendingPathComponent("users/me/saved-jobs")
        let (data, response) = try await session.data(from: url)
        try validateResponse(response)
        return try JSONDecoder().decode(JobsResponse.self, from: data)
    }

    private func validateResponse(_ response: URLResponse) throws {
        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }
        guard (200...299).contains(httpResponse.statusCode) else {
            throw URLError(.init(rawValue: httpResponse.statusCode))
        }
    }
}
