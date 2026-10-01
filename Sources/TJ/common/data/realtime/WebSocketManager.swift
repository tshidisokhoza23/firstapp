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
    public static let shared = WebSocketManager(
        tokenProvider: { SessionStore.shared.token },
        wsURLProvider: { AppBackend.wsBaseURL }
    )

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
        sessionStore: SessionStore = SessionStore.shared,
        wsURLProvider: @escaping @Sendable () -> String = { AppBackend.wsBaseURL }
    ) {
        self.urlSession = urlSession
        self.tokenProvider = { sessionStore.token }
        self.wsURLProvider = wsURLProvider
        super.init()
    }

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

        let base = wsURLProvider().trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        guard var components = URLComponents(string: base) else { return }
        components.queryItems = [URLQueryItem(name: "token", value: token)]
        guard let url = components.url else { return }

        connectionStateSubject.send(.connecting)

        var request = URLRequest(url: url)
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let task = urlSession.webSocketTask(with: request)
        self.webSocketTask = task
        task.resume()

        self.isConnected = true
        self.retryCount = 0
        connectionStateSubject.send(.connected)

        startReceiveLoop()
        startHeartbeat()
    }

    public func disconnect() {
        isManualDisconnect = true
        isConnected = false
        heartbeatTask?.cancel()
        heartbeatTask = nil
        receiveTask?.cancel()
        receiveTask = nil
        webSocketTask?.cancel(with: .normalClosure, reason: nil)
        webSocketTask = nil
        connectionStateSubject.send(.disconnected)
    }

    public func send(type: String, payload: [String: WSAnyCodable] = [:]) async throws {
        guard let task = webSocketTask, isConnected else {
            throw URLError(.notConnectedToInternet)
        }
        let env = WSEnvelope(type: type, payload: payload)
        let data = try JSONEncoder().encode(env)
        let string = String(data: data, encoding: .utf8) ?? ""
        try await task.send(.string(string))
    }

    private func startReceiveLoop() {
        receiveTask?.cancel()
        receiveTask = Task {
            while isConnected {
                guard let task = webSocketTask else { break }
                do {
                    let message = try await task.receive()
                    switch message {
                    case .string(let text):
                        if let data = text.data(using: .utf8),
                           let env = try? JSONDecoder().decode(WSEnvelope.self, from: data) {
                            eventsSubject.send(env)
                        }
                    case .data(let data):
                        if let env = try? JSONDecoder().decode(WSEnvelope.self, from: data) {
                            eventsSubject.send(env)
                        }
                    @unknown default:
                        break
                    }
                } catch {
                    if !isManualDisconnect {
                        handleDisconnect()
                    }
                    break
                }
            }
        }
    }

    private func startHeartbeat() {
        heartbeatTask?.cancel()
        heartbeatTask = Task {
            while isConnected {
                try? await Task.sleep(nanoseconds: 25_000_000_000)
                guard isConnected, let task = webSocketTask else { break }
                let ping = WSEnvelope(type: "ping", payload: [:])
                if let data = try? JSONEncoder().encode(ping),
                   let text = String(data: data, encoding: .utf8) {
                    try? await task.send(.string(text))
                }
            }
        }
    }

    private func handleDisconnect() {
        isConnected = false
        connectionStateSubject.send(.disconnected)
        guard !isManualDisconnect else { return }

        retryCount += 1
        let delaySeconds = min(pow(2.0, Double(retryCount)), 30.0)
        Task {
            try? await Task.sleep(nanoseconds: UInt64(delaySeconds * 1_000_000_000))
            if !self.isConnected && !self.isManualDisconnect {
                self.connect()
            }
        }
    }
}
