import Foundation
import Combine

// MARK: - Call Event & Delegate

public struct CallEvent: Codable, Identifiable, Sendable {
    public var id: String { sessionId }
    public let type: String?
    public let senderId: String
    public let receiverId: String
    public let sessionId: String
    public let callType: CallType
    public var senderName: String
    public let signalType: String?
    public let signalPayload: [String: String]?
    public let durationSeconds: Int
    public let endReason: String?
    public let timestamp: String?

    public init(
        type: String?,
        senderId: String,
        receiverId: String,
        sessionId: String,
        callType: CallType,
        senderName: String = "",
        signalType: String? = nil,
        signalPayload: [String: String]? = nil,
        durationSeconds: Int = 0,
        endReason: String? = nil,
        timestamp: String? = nil
    ) {
        self.type = type
        self.senderId = senderId
        self.receiverId = receiverId
        self.sessionId = sessionId
        self.callType = callType
        self.senderName = senderName
        self.signalType = signalType
        self.signalPayload = signalPayload
        self.durationSeconds = durationSeconds
        self.endReason = endReason
        self.timestamp = timestamp
    }
}

public protocol CallSocketListener: AnyObject {
    func onOpen()
    func onEvent(_ event: CallEvent)
    func onError(_ error: String)
}

private struct OutgoingCallEvent: Codable {
    let type: String
    let receiverId: String
    let sessionId: String
    let callType: String?
    let signalType: String?
    let signalPayload: [String: String]?
    let durationSeconds: Int
    let endReason: String?

    enum CodingKeys: String, CodingKey {
        case type
        case receiverId = "receiver_id"
        case sessionId = "session_id"
        case callType = "call_type"
        case signalType = "signal_type"
        case signalPayload = "signal_payload"
        case durationSeconds = "duration_seconds"
        case endReason = "end_reason"
    }
}

private struct WireCallEvent: Codable {
    let type: String?
    let senderId: String
    let receiverId: String
    let sessionId: String
    let callType: String?
    let signalType: String?
    let signalPayload: [String: String]?
    let durationSeconds: Int?
    let endReason: String?
    let timestamp: String?

    enum CodingKeys: String, CodingKey {
        case type
        case senderId = "sender_id"
        case receiverId = "receiver_id"
        case sessionId = "session_id"
        case callType = "call_type"
        case signalType = "signal_type"
        case signalPayload = "signal_payload"
        case durationSeconds = "duration_seconds"
        case endReason = "end_reason"
        case timestamp
    }
}

// MARK: - Call WebSocket Client

public final class CallWebSocketClient: NSObject, URLSessionWebSocketDelegate, @unchecked Sendable {
    public static let shared = CallWebSocketClient()

    private var webSocketTask: URLSessionWebSocketTask?
    private var urlSession: URLSession?
    private var listeners = NSHashTable<AnyObject>.weakObjects()
    private var pendingMessages: [OutgoingCallEvent] = []
    private var pendingRaw: [String] = []

    private var isConnected = false
    private var isConnecting = false
    private var manualClose = false
    private var lastBaseUrl: String?
    private var lastToken: String?
    private var reconnectWorkItem: DispatchWorkItem?
    private let lock = NSLock()

    public override init() {
        super.init()
        let configuration = URLSessionConfiguration.default
        self.urlSession = URLSession(configuration: configuration, delegate: self, delegateQueue: OperationQueue())
    }

    // MARK: - Connection Methods

    public func connect(listener: CallSocketListener, serverUrl: String, authToken: String) {
        lock.lock()
        listeners.add(listener)
        lock.unlock()

        ensureConnected(baseUrl: serverUrl, token: authToken)
    }

    public func disconnect(listener: CallSocketListener) {
        lock.lock()
        listeners.remove(listener)
        let count = listeners.allObjects.count
        lock.unlock()

        if count == 0 {
            close()
        }
    }

    private func ensureConnected(baseUrl: String, token: String) {
        lock.lock()
        if isConnected || isConnecting {
            lock.unlock()
            return
        }
        isConnecting = true
        manualClose = false
        lastBaseUrl = baseUrl
        lastToken = token
        lock.unlock()
        reconnectWorkItem?.cancel()
        reconnectWorkItem = nil

        let urlString = baseUrl.hasSuffix("/") ? "\(baseUrl)ws/calls?token=\(token)" : "\(baseUrl)/ws/calls?token=\(token)"
        guard let url = URL(string: urlString) else {
            lock.lock()
            isConnecting = false
            lock.unlock()
            notifyError("Invalid WebSocket URL")
            return
        }

        var request = URLRequest(url: url)
        request.addValue("Bearer \(token)", forHTTPHeaderField: "Authorization")

        let task = urlSession?.webSocketTask(with: request)
        self.webSocketTask = task
        task?.resume()

        listenForMessages()
    }

    private func listenForMessages() {
        webSocketTask?.receive { [weak self] result in
            guard let self = self else { return }

            switch result {
            case .success(let message):
                switch message {
                case .string(let text):
                    self.handleIncomingText(text)
                case .data(let data):
                    if let text = String(data: data, encoding: .utf8) {
                        self.handleIncomingText(text)
                    }
                @unknown default:
                    break
                }
                self.listenForMessages()

            case .failure(let error):
                print("[CallWebSocket] Receive error: \(error.localizedDescription)")
                self.handleConnectionFailure()
            }
        }
    }

    private func handleIncomingText(_ text: String) {
        guard let data = text.data(using: .utf8) else { return }
        if let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            let rawPayload = obj["signal_payload"] as? [String: Any] ?? [:]
            var payload: [String: String] = [:]
            for (key, value) in rawPayload {
                payload[key] = LiveJSON.string(value)
            }
            let event = CallEvent(
                type: LiveJSON.string(obj["type"]).nilIfEmpty,
                senderId: LiveJSON.string(obj["sender_id"]),
                receiverId: LiveJSON.string(obj["receiver_id"]),
                sessionId: LiveJSON.string(obj["session_id"]),
                callType: CallType.fromWire(LiveJSON.string(obj["call_type"])),
                signalType: LiveJSON.string(obj["signal_type"]).nilIfEmpty,
                signalPayload: payload,
                durationSeconds: LiveJSON.int(obj["duration_seconds"]),
                endReason: LiveJSON.string(obj["end_reason"]).nilIfEmpty,
                timestamp: LiveJSON.string(obj["timestamp"]).nilIfEmpty
            )
            if !event.sessionId.isEmpty || event.type != nil {
                notifyEvent(event)
                return
            }
        }
        do {
            let wireEvent = try JSONDecoder().decode(WireCallEvent.self, from: data)
            let event = CallEvent(
                type: wireEvent.type,
                senderId: wireEvent.senderId,
                receiverId: wireEvent.receiverId,
                sessionId: wireEvent.sessionId,
                callType: CallType.fromWire(wireEvent.callType),
                signalType: wireEvent.signalType,
                signalPayload: wireEvent.signalPayload,
                durationSeconds: wireEvent.durationSeconds ?? 0,
                endReason: wireEvent.endReason,
                timestamp: wireEvent.timestamp
            )
            notifyEvent(event)
        } catch {
            print("[CallWebSocket] Parse error: \(error)")
        }
    }

    // MARK: - Signaling Actions

    public func invite(receiverId: String, sessionId: String, callType: CallType) {
        send(OutgoingCallEvent(type: "call_invite", receiverId: receiverId, sessionId: sessionId, callType: callType.wireValue, signalType: nil, signalPayload: nil, durationSeconds: 0, endReason: nil))
    }

    public func ringing(receiverId: String, sessionId: String, callType: CallType) {
        send(OutgoingCallEvent(type: "call_ringing", receiverId: receiverId, sessionId: sessionId, callType: callType.wireValue, signalType: nil, signalPayload: nil, durationSeconds: 0, endReason: nil))
    }

    public func accept(receiverId: String, sessionId: String, callType: CallType) {
        send(OutgoingCallEvent(type: "call_accept", receiverId: receiverId, sessionId: sessionId, callType: callType.wireValue, signalType: nil, signalPayload: nil, durationSeconds: 0, endReason: nil))
    }

    public func reject(receiverId: String, sessionId: String, callType: CallType, reason: String = "rejected") {
        send(OutgoingCallEvent(type: "call_reject", receiverId: receiverId, sessionId: sessionId, callType: callType.wireValue, signalType: nil, signalPayload: nil, durationSeconds: 0, endReason: reason))
    }

    public func end(receiverId: String, sessionId: String, callType: CallType, durationSeconds: Int, reason: String = "ended") {
        send(OutgoingCallEvent(type: "call_end", receiverId: receiverId, sessionId: sessionId, callType: callType.wireValue, signalType: nil, signalPayload: nil, durationSeconds: durationSeconds, endReason: reason))
    }

    public func signal(receiverId: String, sessionId: String, callType: CallType, signalType: String, signalPayload: [String: String]) {
        signalObject(receiverId: receiverId, sessionId: sessionId, callType: callType, signalType: signalType, signalPayload: signalPayload)
    }

    public func signalObject(receiverId: String, sessionId: String, callType: CallType, signalType: String, signalPayload: [String: Any]) {
        let body: [String: Any] = [
            "type": "call_signal",
            "receiver_id": receiverId,
            "session_id": sessionId,
            "call_type": callType.wireValue,
            "signal_type": signalType,
            "signal_payload": signalPayload,
            "duration_seconds": 0
        ]
        guard JSONSerialization.isValidJSONObject(body),
              let data = try? JSONSerialization.data(withJSONObject: body),
              let text = String(data: data, encoding: .utf8) else { return }
        sendText(text)
    }

    private func send(_ event: OutgoingCallEvent) {
        guard let data = try? JSONEncoder().encode(event), let jsonString = String(data: data, encoding: .utf8) else { return }

        lock.lock()
        let activeTask = webSocketTask
        let currentlyConnected = isConnected
        if !currentlyConnected {
            pendingMessages.append(event)
        }
        lock.unlock()

        if currentlyConnected, let task = activeTask {
            task.send(.string(jsonString)) { error in
                if let error = error {
                    print("[CallWebSocket] Send failure: \(error.localizedDescription)")
                }
            }
        }
    }

    private func sendText(_ jsonString: String) {
        lock.lock()
        let activeTask = webSocketTask
        let currentlyConnected = isConnected
        if !currentlyConnected {
            pendingRaw.append(jsonString)
        }
        lock.unlock()
        guard currentlyConnected, let task = activeTask else { return }
        task.send(.string(jsonString)) { error in
            if let error = error {
                print("[CallWebSocket] Send failure: \(error.localizedDescription)")
            }
        }
    }

    public func close() {
        reconnectWorkItem?.cancel()
        reconnectWorkItem = nil
        lock.lock()
        manualClose = true
        isConnected = false
        isConnecting = false
        let task = webSocketTask
        webSocketTask = nil
        pendingMessages.removeAll()
        lock.unlock()

        task?.cancel(with: .normalClosure, reason: nil)
    }

    // MARK: - URLSessionWebSocketDelegate

    public func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didOpenWithProtocol protocol: String?) {
        lock.lock()
        isConnected = true
        isConnecting = false
        let queued = pendingMessages
        let raw = pendingRaw
        pendingMessages.removeAll()
        pendingRaw.removeAll()
        lock.unlock()

        for event in queued {
            send(event)
        }
        for text in raw {
            sendText(text)
        }
        notifyOpen()
    }

    public func urlSession(_ session: URLSession, webSocketTask: URLSessionWebSocketTask, didCloseWith closeCode: URLSessionWebSocketTask.CloseCode, reason: Data?) {
        handleConnectionFailure()
    }

    private func handleConnectionFailure() {
        lock.lock()
        isConnected = false
        isConnecting = false
        let isManual = manualClose
        let listenerCount = listeners.allObjects.count
        let baseUrl = lastBaseUrl
        let token = lastToken
        webSocketTask = nil
        lock.unlock()

        reconnectWorkItem?.cancel()
        guard !isManual, listenerCount > 0, let baseUrl, let token else { return }

        let work = DispatchWorkItem { [weak self] in
            self?.ensureConnected(baseUrl: baseUrl, token: token)
        }
        reconnectWorkItem = work
        DispatchQueue.global().asyncAfter(deadline: .now() + 5.0, execute: work)
    }

    // MARK: - Event Dispatching

    private func notifyOpen() {
        lock.lock()
        let currentListeners = listeners.allObjects.compactMap { $0 as? CallSocketListener }
        lock.unlock()

        DispatchQueue.main.async {
            currentListeners.forEach { $0.onOpen() }
        }
    }

    private func notifyEvent(_ event: CallEvent) {
        lock.lock()
        let currentListeners = listeners.allObjects.compactMap { $0 as? CallSocketListener }
        lock.unlock()

        DispatchQueue.main.async {
            currentListeners.forEach { $0.onEvent(event) }
        }
    }

    private func notifyError(_ error: String) {
        lock.lock()
        let currentListeners = listeners.allObjects.compactMap { $0 as? CallSocketListener }
        lock.unlock()

        DispatchQueue.main.async {
            currentListeners.forEach { $0.onError(error) }
        }
    }
}
