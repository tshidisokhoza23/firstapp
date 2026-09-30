import Foundation
import Combine

enum WebSocketConnectionState {
    case connecting
    case connected
    case disconnected
}

struct WSEnvelope: Codable {
    let type: String?
    let payload: [String: String]?
}

class WebSocketManager: ObservableObject {
    static let shared = WebSocketManager()

    @Published var connectionState: WebSocketConnectionState = .disconnected
    @Published var isConnected: Bool = false
    @Published var incomingMessagePublisher = PassthroughSubject<WSEnvelope, Never>()

    private var webSocketTask: URLSessionWebSocketTask?
    private var heartbeatTimer: Timer?
    private var retryCount: Int = 0
    private var isManualDisconnect: Bool = false

    private let wsUrlString = "wss://firstappserver.fly.dev/api/v1/ws/chat"

    init() {
        connect()
    }

    func connect() {
        guard !isConnected else { return }
        isManualDisconnect = false
        connectionState = .connecting

        guard let url = URL(string: "\(wsUrlString)?token=mock_token_123") else { return }
        let session = URLSession(configuration: .default)
        webSocketTask = session.webSocketTask(with: url)
        webSocketTask?.resume()

        connectionState = .connected
        isConnected = true
        retryCount = 0

        startHeartbeat()
        receiveMessages()
    }

    private func receiveMessages() {
        webSocketTask?.receive { [weak self] result in
            guard let self = self else { return }
            switch result {
            case .success(let message):
                switch message {
                case .string(let text):
                    self.parseAndEmit(text: text)
                case .data(let data):
                    if let text = String(data: data, encoding: .utf8) {
                        self.parseAndEmit(text: text)
                    }
                @unknown default:
                    break
                }
                self.receiveMessages()

            case .failure(let error):
                print("WebSocket Receive Error: \(error.localizedDescription)")
                self.handleDisconnect()
            }
        }
    }

    private func parseAndEmit(text: String) {
        guard let data = text.data(using: .utf8) else { return }
        let decoder = JSONDecoder()
        if let envelope = try? decoder.decode(WSEnvelope.self, from: data) {
            DispatchQueue.main.async {
                self.incomingMessagePublisher.send(envelope)
            }
        }
    }

    func send(type: String, payload: [String: String]? = nil) {
        guard isConnected, let webSocketTask = webSocketTask else {
            connect()
            return
        }

        let envelope = WSEnvelope(type: type, payload: payload)
        if let encoder = try? JSONEncoder().encode(envelope),
           let jsonString = String(data: encoder, encoding: .utf8) {
            let message = URLSessionWebSocketTask.Message.string(jsonString)
            webSocketTask.send(message) { error in
                if let error = error {
                    print("WebSocket Send Error: \(error.localizedDescription)")
                }
            }
        }
    }

    private func startHeartbeat() {
        heartbeatTimer?.invalidate()
        heartbeatTimer = Timer.scheduledTimer(withTimeInterval: 15.0, repeats: true) { [weak self] _ in
            self?.send(type: "heartbeat", payload: ["status": "online", "timestamp": "\(Date().timeIntervalSince1970)"])
        }
    }

    private func handleDisconnect() {
        DispatchQueue.main.async {
            self.isConnected = false
            self.connectionState = .disconnected
        }
        heartbeatTimer?.invalidate()
        webSocketTask = nil

        if !isManualDisconnect {
            retryCount += 1
            let delay = min(15.0, Double(retryCount * 2))
            DispatchQueue.main.asyncAfter(deadline: .now() + delay) { [weak self] in
                self?.connect()
            }
        }
    }

    func disconnect() {
        isManualDisconnect = true
        heartbeatTimer?.invalidate()
        webSocketTask?.cancel(with: .normalClosure, reason: nil)
        webSocketTask = nil
        isConnected = false
        connectionState = .disconnected
    }
}
