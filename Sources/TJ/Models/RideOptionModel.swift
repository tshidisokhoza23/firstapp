import Foundation

struct RideOptionModel: Identifiable, Codable, Hashable {
    let id: String
    let name: String
    let price: String
    let etaMinutes: Int
    let capacity: Int
    let iconName: String

    static let samples = [
        RideOptionModel(id: "r1", name: "TJ Economy", price: "$14.50", etaMinutes: 3, capacity: 4, iconName: "car.fill"),
        RideOptionModel(id: "r2", name: "TJ Comfort", price: "$21.00", etaMinutes: 5, capacity: 4, iconName: "car.side.fill"),
        RideOptionModel(id: "r3", name: "TJ Executive", price: "$35.00", etaMinutes: 8, capacity: 4, iconName: "car.top.door.front.left.open.fill")
    ]
}
