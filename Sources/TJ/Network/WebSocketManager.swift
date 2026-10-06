import Foundation
import Combine

struct AnyCodable: Codable {
    let value: Any

    init(_ value: Any) {
        self.value = value
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let bool = try? container.decode(Bool.self) {
            value = bool
        } else if let int = try? container.decode(Int.self) {
            value = int
        } else if let double = try? container.decode(Double.self) {
            value = double
        } else if let string = try? container.decode(String.self) {
            value = string
        } else if let dict = try? container.decode([String: AnyCodable].self) {
            value = dict.mapValues { $0.value }
        } else if let array = try? container.decode([AnyCodable].self) {
            value = array.map { $0.value }
        } else {
            value = ""
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        if let bool = value as? Bool {
            try container.encode(bool)
        } else if let int = value as? Int {
            try container.encode(int)
        } else if let double = value as? Double {
            try container.encode(double)
        } else if let string = value as? String {
            try container.encode(string)
        } else {
            try container.encode(String(describing: value))
        }
    }

    var stringValue: String? {
        if let str = value as? String { return str }
        if let intVal = value as? Int { return String(intVal) }
        if let dblVal = value as? Double { return String(dblVal) }
        return nil
    }
}

enum WebSocketConnectionState {
    case connecting
    case connected
    case disconnected
}

struct WSEnvelope: Codable {
    let type: String?
    let payload: [String: AnyCodable]?
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

        let token = UserDefaults.standard.string(forKey: "tj_auth_token") ?? "mock_token_123"
        guard let url = URL(string: "\(wsUrlString)?token=\(token)") else { return }

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

        let envelope = WSEnvelope(type: type, payload: payload?.mapValues { AnyCodable($0) })
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
