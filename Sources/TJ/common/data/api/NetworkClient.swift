import Foundation

public final class NetworkClient: @unchecked Sendable {
    public static let shared = NetworkClient()

    private let session: URLSession
    private let baseURL: URL

    public init(
        baseURL: URL = URL(string: "https://api.tj.app")!,
        session: URLSession = .shared
    ) {
        self.baseURL = baseURL
        self.session = session
    }

    public func request<T: Decodable>(
        endpoint: String,
        method: String = "GET",
        headers: [String: String] = [:],
        body: Data? = nil
    ) async throws -> T {
        var url = baseURL.appendingPathComponent(endpoint)
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.httpBody = body

        for (key, value) in headers {
            request.setValue(value, forHTTPHeaderField: key)
        }

        if request.value(forHTTPHeaderField: "Content-Type") == nil {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        let (data, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }

        guard (200...299).contains(httpResponse.statusCode) else {
            let message = String(data: data, encoding: .utf8) ?? "HTTP Error \(httpResponse.statusCode)"
            throw NSError(domain: "NetworkClient", code: httpResponse.statusCode, userInfo: [NSLocalizedDescriptionKey: message])
        }

        let decoder = JSONDecoder()
        return try decoder.decode(T.self, from: data)
    }
}

public struct LoginRequest: Codable, Sendable {
    public let username: String
    public let password: String
    public init(username: String, password: String) {
        self.username = username
        self.password = password
    }
}

public struct AuthResponse: Codable, Equatable, Sendable {
    public let token: String
    public let refreshToken: String?
    public let user: User?

    enum CodingKeys: String, CodingKey {
        case token, user
        case refreshToken = "refresh_token"
    }

    public init(token: String, refreshToken: String? = nil, user: User? = nil) {
        self.token = token
        self.refreshToken = refreshToken
        self.user = user
    }
}

public struct TokenRefreshRequest: Codable, Sendable {
    public let refreshToken: String
    enum CodingKeys: String, CodingKey { case refreshToken = "refresh_token" }
    public init(refreshToken: String) { self.refreshToken = refreshToken }
}
