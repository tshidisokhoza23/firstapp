import Foundation
import Combine

class NetworkClient: ObservableObject {
    static let shared = NetworkClient()

    @Published var baseURL: String = "https://firstappserver.fly.dev/api/v1"
    @Published var isConnectedToBackend: Bool = false
    @Published var authToken: String? = nil

    private var webSocketTask: URLSessionWebSocketTask?

    init() {
        checkBackendHealth()
    }

    func checkBackendHealth() {
        guard let url = URL(string: "\(baseURL)/health") else { return }
        URLSession.shared.dataTask(with: url) { [weak self] data, response, error in
            DispatchQueue.main.async {
                if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 {
                    self?.isConnectedToBackend = true
                } else {
                    self?.checkLocalBackend()
                }
            }
        }.resume()
    }

    private func checkLocalBackend() {
        guard let localUrl = URL(string: "http://localhost:8080/api/v1/health") else { return }
        URLSession.shared.dataTask(with: localUrl) { [weak self] data, response, error in
            DispatchQueue.main.async {
                if let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 {
                    self?.baseURL = "http://localhost:8080/api/v1"
                    self?.isConnectedToBackend = true
                } else {
                    self?.isConnectedToBackend = false
                }
            }
        }.resume()
    }

    // MARK: - Auth APIs
    func login(phoneNumber: String) async throws -> String {
        guard let url = URL(string: "\(baseURL)/auth/login") else { throw URLError(.badURL) }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        let body = ["phoneNumber": phoneNumber]
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
            return "simulated_otp_123456"
        }
        return String(data: data, encoding: .utf8) ?? "simulated_otp_123456"
    }

    // MARK: - Fetch Chats
    func fetchChats() async -> [ChatItem] {
        guard let url = URL(string: "\(baseURL)/chats") else { return ChatItem.samples }
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let chats = try JSONDecoder().decode([ChatItem].self, from: data)
            return chats.isEmpty ? ChatItem.samples : chats
        } catch {
            return ChatItem.samples
        }
    }

    // MARK: - Fetch Social Posts
    func fetchSocialPosts() async -> [SocialPost] {
        guard let url = URL(string: "\(baseURL)/social/posts") else { return SocialPost.samples }
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let posts = try JSONDecoder().decode([SocialPost].self, from: data)
            return posts.isEmpty ? SocialPost.samples : posts
        } catch {
            return SocialPost.samples
        }
    }

    // MARK: - Fetch Mingle Candidates
    func fetchMingleCandidates(radiusKm: Double) async -> [MingleProfileModel] {
        guard let url = URL(string: "\(baseURL)/mingle/candidates?radius=\(radiusKm)") else { return MingleProfileModel.samples }
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let candidates = try JSONDecoder().decode([MingleProfileModel].self, from: data)
            return candidates.isEmpty ? MingleProfileModel.samples : candidates
        } catch {
            return MingleProfileModel.samples
        }
    }

    // MARK: - WebSocket Realtime Connection
    func connectWebSocket() {
        guard let wsUrl = URL(string: baseURL.replacingOccurrences(of: "http", with: "ws") + "/ws") else { return }
        let session = URLSession(configuration: .default)
        webSocketTask = session.webSocketTask(with: wsUrl)
        webSocketTask?.resume()
        receiveWebSocketMessages()
    }

    private func receiveWebSocketMessages() {
        webSocketTask?.receive { [weak self] result in
            switch result {
            case .success(let message):
                print("WebSocket Received: \(message)")
                self?.receiveWebSocketMessages()
            case .failure(let error):
                print("WebSocket Error: \(error)")
            }
        }
    }
}
