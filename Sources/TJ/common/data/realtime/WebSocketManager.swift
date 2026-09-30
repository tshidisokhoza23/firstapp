import Foundation
import Combine

public struct WSEnvelope: Codable, Sendable {
    public let type: String?
    public let payload: [String: WSAnyCodable]?

    public init(type: String? = nil, payload: [String: WSAnyCodable]? = nil) {
        self.type = type
        self.payload = payload
    }
}

public struct WSAnyCodable: Codable, Sendable {
    public let value: Sendable

    public var stringValue: String? { value as? String }
    public var boolValue: Bool? { value as? Bool }
    public var intValue: Int? { value as? Int }
    public var int64Value: Int64? {
        if let v = value as? Int64 { return v }
        if let v = value as? Int { return Int64(v) }
        if let v = value as? Double { return Int64(v) }
        return nil
    }
    public var doubleValue: Double? { value as? Double }
    public var arrayValue: [WSAnyCodable]? { value as? [WSAnyCodable] }
    public var dictionaryValue: [String: WSAnyCodable]? { value as? [String: WSAnyCodable] }

    public init(_ value: Sendable) {
        self.value = value
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() {
            self.value = NSNull()
        } else if let boolVal = try? container.decode(Bool.self) {
            self.value = boolVal
        } else if let intVal = try? container.decode(Int64.self) {
            self.value = intVal
        } else if let doubleVal = try? container.decode(Double.self) {
            self.value = doubleVal
        } else if let strVal = try? container.decode(String.self) {
            self.value = strVal
        } else if let arrVal = try? container.decode([WSAnyCodable].self) {
            self.value = arrVal
        } else if let dictVal = try? container.decode([String: WSAnyCodable].self) {
            self.value = dictVal
        } else {
            throw DecodingError.dataCorruptedError(in: container, debugDescription: "Unsupported WSAnyCodable type")
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        if value is NSNull {
            try container.encodeNil()
        } else if let b = value as? Bool {
            try container.encode(b)
        } else if let i = value as? Int64 {
            try container.encode(i)
        } else if let i = value as? Int {
            try container.encode(i)
        } else if let d = value as? Double {
            try container.encode(d)
        } else if let s = value as? String {
            try container.encode(s)
        } else if let arr = value as? [WSAnyCodable] {
            try container.encode(arr)
        } else if let dict = value as? [String: WSAnyCodable] {
            try container.encode(dict)
        } else {
            try container.encodeNil()
        }
    }
}

public final class WebSocketManager: NSObject, @unchecked Sendable {
    public enum ConnectionState: String, Sendable {
        case connecting
        case connected
        case disconnected
    }

    public let eventsSubject = PassthroughSubject<WSEnvelope, Never>()
    public let connectionStateSubject = CurrentValueSubject<ConnectionState, Never>(.disconnected)

    private var webSocketTask: URLSessionWebSocketTask?
    private let urlSession: URLSession
    private let tokenProvider: @Sendable () -> String?
    private let wsURLProvider: @Sendable () -> String

    private var isConnected = false
    private var isManualDisconnect = false
    private var retryCount = 0
    private var heartbeatTask: Task<Void, Never>?
    private var receiveTask: Task<Void, Never>?

    public init(
        urlSession: URLSession = .shared,
        tokenProvider: @escaping @Sendable () -> String?,
        wsURLProvider: @escaping @Sendable () -> String
    ) {
        self.urlSession = urlSession
        self.tokenProvider = tokenProvider
        self.wsURLProvider = wsURLProvider
        super.init()
    }

    public func connect() {
        guard !isConnected else { return }
        isManualDisconnect = false

        guard let token = tokenProvider(), !token.isEmpty else {
            connectionStateSubject.send(.disconnected)
            Task {
                try? await Task.sleep(nanoseconds: 2_000_000_000)
                if !self.isConnected && self.tokenProvider() != nil {
                    self.connect()
                }
            }
            return
        }

        connectionStateSubject.send(.connecting)

        var base = wsURLProvider().trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        if base.hasPrefix("http://") {
            base = "ws://" + base.dropFirst(7)
        } else if base.hasPrefix("https://") {
            base = "wss://" + base.dropFirst(8)
        } else if !base.hasPrefix("ws://") && !base.hasPrefix("wss://") {
            base = "wss://" + base
        }

        let fullUrlString = base.hasSuffix("/ws/chat") ? "\(base)?token=\(token)" : "\(base)/ws/chat?token=\(token)"
        guard let url = URL(string: fullUrlString) else {
            connectionStateSubject.send(.disconnected)
            return
        }

        var request = URLRequest(url: url)
        request.timeoutInterval = 10

        let task = urlSession.webSocketTask(with: request)
        self.webSocketTask = task
        task.resume()

        self.isConnected = true
        self.retryCount = 0
        self.connectionStateSubject.send(.connected)

        startReceiving()
        startHeartbeat()
    }

    private func startReceiving() {
        receiveTask?.cancel()
        receiveTask = Task { [weak self] in
            while let self = self, self.isConnected, !Task.isCancelled {
                guard let task = self.webSocketTask else { break }
                do {
                    let message = try await task.receive()
                    switch message {
                    case .string(let text):
                        if let data = text.data(using: .utf8) {
                            let decoder = JSONDecoder()
                            if let envelope = try? decoder.decode(WSEnvelope.self, from: data) {
                                self.eventsSubject.send(envelope)
                            }
                        }
                    case .data(let data):
                        let decoder = JSONDecoder()
                        if let envelope = try? decoder.decode(WSEnvelope.self, from: data) {
                            self.eventsSubject.send(envelope)
                        }
                    @unknown default:
                        break
                    }
                } catch {
                    self.handleDisconnect()
                    break
                }
            }
        }
    }

    private func startHeartbeat() {
        heartbeatTask?.cancel()
        heartbeatTask = Task { [weak self] in
            while let self = self, self.isConnected, !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 15_000_000_000)
                guard self.isConnected else { break }
                let payload: [String: WSAnyCodable] = [
                    "status": WSAnyCodable("online"),
                    "timestamp": WSAnyCodable(Int64(Date().timeIntervalSince1970 * 1000))
                ]
                _ = self.send(type: "heartbeat", payload: payload)
            }
        }
    }

    @discardableResult
    public func send(type: String, payload: [String: WSAnyCodable]? = nil) -> Bool {
        guard let task = webSocketTask, isConnected else {
            if !isConnected && !isManualDisconnect {
                connect()
            }
            return false
        }
        let envelope = WSEnvelope(type: type, payload: payload)
        let encoder = JSONEncoder()
        guard let data = try? encoder.encode(envelope),
              let jsonString = String(data: data, encoding: .utf8) else {
            return false
        }

        task.send(.string(jsonString)) { error in
            if error != nil {
                // Sent error handling
            }
        }
        return true
    }

    private func handleDisconnect() {
        guard isConnected else { return }
        isConnected = false
        webSocketTask = nil
        heartbeatTask?.cancel()
        receiveTask?.cancel()
        connectionStateSubject.send(.disconnected)

        if isManualDisconnect { return }

        retryCount += 1
        let delaySeconds = calculateExponentialBackoff(retries: retryCount)
        Task {
            try? await Task.sleep(nanoseconds: UInt64(delaySeconds * 1_000_000_000))
            if !self.isConnected && !self.isManualDisconnect {
                self.connect()
            }
        }
    }

    private func calculateExponentialBackoff(retries: Int) -> Double {
        let baseDelay = 1.0
        let maxDelay = 15.0
        let exponent = min(Double(retries), 5.0)
        let delay = min(maxDelay, baseDelay * pow(2.0, exponent))
        let jitter = Double.random(in: 0...0.5)
        return delay + jitter
    }

    public func disconnect() {
        isManualDisconnect = true
        isConnected = false
        heartbeatTask?.cancel()
        receiveTask?.cancel()
        webSocketTask?.cancel(with: .normalClosure, reason: nil)
        webSocketTask = nil
        connectionStateSubject.send(.disconnected)
    }
}
