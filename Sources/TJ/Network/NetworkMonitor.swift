import Foundation
import Network
import Combine

enum NetworkInterfaceType {
    case wifi
    case cellular
    case unavailable
}

struct NetworkStatus {
    let isConnected: Bool
    let interfaceType: NetworkInterfaceType
    let isRoaming: Bool
    let isMetered: Bool

    static let unavailable = NetworkStatus(isConnected: false, interfaceType: .unavailable, isRoaming: false, isMetered: false)
}

class NetworkMonitor: ObservableObject {
    static let shared = NetworkMonitor()

    @Published var status: NetworkStatus = .unavailable

    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "com.tpsk.tj.networkmonitor")

    init() {
        monitor.pathUpdateHandler = { [weak self] path in
            DispatchQueue.main.async {
                let isConnected = (path.status == .satisfied)
                let isCellular = path.usesInterfaceType(.cellular)
                let isWifi = path.usesInterfaceType(.wifi)
                let isExpensive = path.isExpensive
                let isConstrained = path.isConstrained

                let interfaceType: NetworkInterfaceType = isWifi ? .wifi : (isCellular ? .cellular : .unavailable)

                self?.status = NetworkStatus(
                    isConnected: isConnected,
                    interfaceType: interfaceType,
                    isRoaming: isConstrained,
                    isMetered: isExpensive
                )
            }
        }
        monitor.start(queue: queue)
    }
}
