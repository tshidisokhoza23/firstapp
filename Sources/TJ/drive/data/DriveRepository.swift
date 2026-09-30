//
//  DriveRepository.swift
//  TJ
//
//  Swift 5+ Repository for Cloud file upload, download, folder tree management,
//  and TJ Drive / Rides features (ride options, destinations, driver documents, WebSocket events).
//

import Foundation
import Combine

// MARK: - Cloud Storage & Folder Tree Models

public struct CloudFileItem: Identifiable, Codable, Equatable {
    public let id: String
    public let name: String
    public let isDirectory: Bool
    public let sizeBytes: Int64
    public let mimeType: String?
    public let downloadUrl: String?
    public let parentFolderId: String?
    public let updatedAt: Int64

    public init(
        id: String,
        name: String,
        isDirectory: Bool = false,
        sizeBytes: Int64 = 0,
        mimeType: String? = nil,
        downloadUrl: String? = nil,
        parentFolderId: String? = nil,
        updatedAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) {
        self.id = id
        self.name = name
        self.isDirectory = isDirectory
        self.sizeBytes = sizeBytes
        self.mimeType = mimeType
        self.downloadUrl = downloadUrl
        self.parentFolderId = parentFolderId
        self.updatedAt = updatedAt
    }
}

// MARK: - Ride & Driver Models

public struct RideOption: Identifiable, Codable, Equatable {
    public let id: String
    public let title: String
    public let price: String
    public let arrivalTime: String
    public let description: String
    public let iconColorHex: String

    public init(id: String, title: String, price: String, arrivalTime: String, description: String, iconColorHex: String = "#4CAF50") {
        self.id = id
        self.title = title
        self.price = price
        self.arrivalTime = arrivalTime
        self.description = description
        self.iconColorHex = iconColorHex
    }
}

public struct Destination: Identifiable, Codable, Equatable {
    public let id: String
    public let name: String
    public let address: String
    public let latitude: Double
    public let longitude: Double
    public let isFavorite: Bool
    public let type: String

    public init(id: String = UUID().uuidString, name: String, address: String, latitude: Double, longitude: Double, isFavorite: Bool = false, type: String = "recent") {
        self.id = id
        self.name = name
        self.address = address
        self.latitude = latitude
        self.longitude = longitude
        self.isFavorite = isFavorite
        self.type = type
    }
}

public struct Trip: Identifiable, Codable, Equatable {
    public let id: String
    public let pickupAddress: String
    public let destinationAddress: String
    public let fare: Double
    public let date: String
    public let status: String

    public init(id: String, pickupAddress: String, destinationAddress: String, fare: Double, date: String, status: String) {
        self.id = id
        self.pickupAddress = pickupAddress
        self.destinationAddress = destinationAddress
        self.fare = fare
        self.date = date
        self.status = status
    }
}

public struct DriverDocument: Identifiable, Codable, Equatable {
    public let id: String
    public let type: String
    public let status: String
    public let url: String

    public init(id: String, type: String, status: String, url: String) {
        self.id = id
        self.type = type
        self.status = status
        self.url = url
    }
}

public struct RideRequest: Identifiable, Codable, Equatable {
    public let id: String
    public let passengerName: String
    public let pickupAddress: String
    public let destinationAddress: String
    public let fare: Double
    public let distance: String

    public init(id: String, passengerName: String, pickupAddress: String, destinationAddress: String, fare: Double, distance: String) {
        self.id = id
        self.passengerName = passengerName
        self.pickupAddress = pickupAddress
        self.destinationAddress = destinationAddress
        self.fare = fare
        self.distance = distance
    }
}

public struct VehicleInfo: Codable, Equatable {
    public let make: String
    public let model: String
    public let year: String
    public let plateNumber: String
    public let color: String

    public init(make: String, model: String, year: String, plateNumber: String, color: String) {
        self.make = make
        self.model = model
        self.year = year
        self.plateNumber = plateNumber
        self.color = color
    }
}

public struct DriverStatus: Identifiable, Codable, Equatable {
    public let id: String
    public let name: String
    public let latitude: Double
    public let longitude: Double
    public let isOnline: Bool

    public init(id: String, name: String, latitude: Double, longitude: Double, isOnline: Bool) {
        self.id = id
        self.name = name
        self.latitude = latitude
        self.longitude = longitude
        self.isOnline = isOnline
    }
}

public struct DriveActiveSession: Codable, Equatable {
    public let sessionId: String
    public let isDriver: Bool
    public let currentRideId: String?

    public init(sessionId: String, isDriver: Bool, currentRideId: String? = nil) {
        self.sessionId = sessionId
        self.isDriver = isDriver
        self.currentRideId = currentRideId
    }
}

public enum RideEvent: Equatable {
    case requested(RideRequest)
    case accepted(rideId: String)
    case arrived(rideId: String)
    case completed(rideId: String)
    case cancelled(rideId: String, reason: String)
}

// MARK: - Drive Repository Protocol & Implementation

public protocol DriveRepositoryProtocol: AnyObject {
    // Cloud Files
    func listCloudFiles(folderId: String?) async throws -> [CloudFileItem]
    func createFolder(name: String, parentFolderId: String?) async throws -> CloudFileItem
    func uploadCloudFile(fileData: Data, fileName: String, parentFolderId: String?) async throws -> CloudFileItem
    func deleteCloudItem(id: String) async throws -> Bool

    // Ride Features
    func getRideOptions() async throws -> [RideOption]
    func getDestinations() async throws -> [Destination]
    func getTrips() async throws -> [Trip]
    func saveDestination(name: String, address: String, latitude: Double, longitude: Double, isFavorite: Bool, type: String) async throws -> Bool
    func deleteDestination(id: String) async throws -> Bool
    func getDriverDocuments() async throws -> [DriverDocument]
    func uploadDocument(type: String, url: String) async throws -> DriverDocument
    func getAvailableRequests(lat: Double, lng: Double) async throws -> [RideRequest]
    func updateOnlineStatus(online: Bool, latitude: Double, longitude: Double) async throws -> Bool
    func createRideRequest(pickup: String, dest: String, pLat: Double, pLng: Double, dLat: Double, dLng: Double, fare: Double, distance: String, type: String) async throws -> String
    func updateRideStatus(rideId: String, status: String) async throws -> Bool
    func cancelRide(rideId: String, reason: String, by: String) async throws -> Bool
    func getVehicleInfo() async throws -> VehicleInfo
    func updateVehicleInfo(vehicle: VehicleInfo) async throws -> VehicleInfo
    func getNearbyDrivers(lat: Double, lng: Double) async throws -> [DriverStatus]
    func getActiveSession() async throws -> DriveActiveSession
}

@MainActor
public final class DriveRepository: ObservableObject, DriveRepositoryProtocol {
    public static let shared = DriveRepository()

    @Published public private(set) var cloudItems: [CloudFileItem] = []
    @Published public private(set) var rideOptions: [RideOption] = []
    @Published public private(set) var destinations: [Destination] = []
    @Published public private(set) var trips: [Trip] = []

    private let rideEventsSubject = PassthroughSubject<RideEvent, Never>()
    public var rideEvents: AnyPublisher<RideEvent, Never> { rideEventsSubject.eraseToAnyPublisher() }

    private let baseURL: URL

    public init(baseURL: URL = URL(string: "https://api.tj.app/drive")!) {
        self.baseURL = baseURL
    }

    // MARK: - Cloud File & Folder Management

    public func listCloudFiles(folderId: String? = nil) async throws -> [CloudFileItem] {
        var components = URLComponents(url: baseURL.appendingPathComponent("files"), resolvingAgainstBaseURL: false)
        if let folderId = folderId {
            components?.queryItems = [URLQueryItem(name: "folder_id", value: folderId)]
        }
        guard let url = components?.url else { return [] }

        let (data, response) = try await URLSession.shared.data(from: url)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }

        struct FilesResponse: Codable { let files: [CloudFileItem] }
        let items = try JSONDecoder().decode(FilesResponse.self, from: data).files
        self.cloudItems = items
        return items
    }

    public func createFolder(name: String, parentFolderId: String? = nil) async throws -> CloudFileItem {
        let url = baseURL.appendingPathComponent("folders")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any?] = ["name": name, "parent_id": parentFolderId]
        request.httpBody = try JSONSerialization.data(withJSONObject: body.compactMapValues { $0 })

        let (data, response) = try await URLSession.shared.data(for: request)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }

        let folder = try JSONDecoder().decode(CloudFileItem.self, from: data)
        cloudItems.append(folder)
        return folder
    }

    public func uploadCloudFile(fileData: Data, fileName: String, parentFolderId: String? = nil) async throws -> CloudFileItem {
        let url = baseURL.appendingPathComponent("upload")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"

        let boundary = "Boundary-\(UUID().uuidString)"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        var body = Data()
        if let parentId = parentFolderId {
            body.append("--\(boundary)\r\nContent-Disposition: form-data; name=\"folder_id\"\r\n\r\n\(parentId)\r\n".data(using: .utf8)!)
        }
        body.append("--\(boundary)\r\nContent-Disposition: form-data; name=\"file\"; filename=\"\(fileName)\"\r\nContent-Type: application/octet-stream\r\n\r\n".data(using: .utf8)!)
        body.append(fileData)
        body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)

        request.httpBody = body

        let (data, response) = try await URLSession.shared.data(for: request)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }

        let fileItem = try JSONDecoder().decode(CloudFileItem.self, from: data)
        cloudItems.append(fileItem)
        return fileItem
    }

    public func deleteCloudItem(id: String) async throws -> Bool {
        let url = baseURL.appendingPathComponent("files/\(id)")
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"

        let (_, response) = try await URLSession.shared.data(for: request)
        let success = (response as? HTTPURLResponse)?.statusCode == 200
        if success {
            cloudItems.removeAll { $0.id == id }
        }
        return success
    }

    // MARK: - Ride & Driver Features

    public func getRideOptions() async throws -> [RideOption] {
        let defaults = [
            RideOption(id: "1", title: "TJ Go", price: "0.00", arrivalTime: "3 min", description: "Standard, compact rides", iconColorHex: "#4CAF50"),
            RideOption(id: "2", title: "TJ Comfort", price: "0.00", arrivalTime: "5 min", description: "Newer cars, extra legroom", iconColorHex: "#2196F3"),
            RideOption(id: "3", title: "TJ Premium", price: "0.00", arrivalTime: "4 min", description: "Luxury & top drivers", iconColorHex: "#9C27B0"),
            RideOption(id: "4", title: "TJ XL", price: "0.00", arrivalTime: "7 min", description: "SUVs for groups up to 6", iconColorHex: "#FF9800")
        ]

        do {
            let url = baseURL.appendingPathComponent("ride-options")
            let (data, response) = try await URLSession.shared.data(from: url)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { return defaults }

            struct OptionsResponse: Codable { let ride_options: [RideOption] }
            let options = try JSONDecoder().decode(OptionsResponse.self, from: data).ride_options
            self.rideOptions = options.isEmpty ? defaults : options
            return self.rideOptions
        } catch {
            self.rideOptions = defaults
            return defaults
        }
    }

    public func getDestinations() async throws -> [Destination] {
        let url = baseURL.appendingPathComponent("destinations")
        let (data, response) = try await URLSession.shared.data(from: url)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else { return destinations }
        struct DestinationsResponse: Codable { let destinations: [Destination] }
        let list = try JSONDecoder().decode(DestinationsResponse.self, from: data).destinations
        self.destinations = list
        return list
    }

    public func getTrips() async throws -> [Trip] {
        let url = baseURL.appendingPathComponent("trips")
        let (data, response) = try await URLSession.shared.data(from: url)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw URLError(.badServerResponse) }
        struct TripsResponse: Codable { let trips: [Trip] }
        let list = try JSONDecoder().decode(TripsResponse.self, from: data).trips
        self.trips = list
        return list
    }

    public func saveDestination(name: String, address: String, latitude: Double, longitude: Double, isFavorite: Bool, type: String) async throws -> Bool {
        let newDest = Destination(name: name, address: address, latitude: latitude, longitude: longitude, isFavorite: isFavorite, type: type)
        destinations.append(newDest)

        let url = baseURL.appendingPathComponent("destinations")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(newDest)

        let (_, response) = try await URLSession.shared.data(for: request)
        return (response as? HTTPURLResponse)?.statusCode == 200
    }

    public func deleteDestination(id: String) async throws -> Bool {
        destinations.removeAll { $0.id == id }
        return true
    }

    public func getDriverDocuments() async throws -> [DriverDocument] {
        let url = baseURL.appendingPathComponent("driver/documents")
        let (data, _) = try await URLSession.shared.data(from: url)
        struct DocsResponse: Codable { let documents: [DriverDocument] }
        return try JSONDecoder().decode(DocsResponse.self, from: data).documents
    }

    public func uploadDocument(type: String, url: String) async throws -> DriverDocument {
        let endpoint = baseURL.appendingPathComponent("driver/documents")
        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: ["type": type, "url": url])

        let (data, _) = try await URLSession.shared.data(for: request)
        struct DocResponse: Codable { let document: DriverDocument }
        return try JSONDecoder().decode(DocResponse.self, from: data).document
    }

    public func getAvailableRequests(lat: Double, lng: Double) async throws -> [RideRequest] {
        let url = baseURL.appendingPathComponent("driver/requests?lat=\(lat)&lng=\(lng)")
        let (data, _) = try await URLSession.shared.data(from: url)
        struct ReqResponse: Codable { let requests: [RideRequest] }
        return try JSONDecoder().decode(ReqResponse.self, from: data).requests
    }

    public func updateOnlineStatus(online: Bool, latitude: Double, longitude: Double) async throws -> Bool {
        let url = baseURL.appendingPathComponent("driver/status")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        let body: [String: Any] = ["online": online, "latitude": latitude, "longitude": longitude]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (_, response) = try await URLSession.shared.data(for: request)
        return (response as? HTTPURLResponse)?.statusCode == 200
    }

    public func createRideRequest(pickup: String, dest: String, pLat: Double, pLng: Double, dLat: Double, dLng: Double, fare: Double, distance: String, type: String) async throws -> String {
        let url = baseURL.appendingPathComponent("ride/request")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        let body: [String: Any] = [
            "pickup_address": pickup, "destination_address": dest,
            "pickup_lat": pLat, "pickup_lng": pLng, "dest_lat": dLat, "dest_lng": dLng,
            "fare": fare, "distance": distance, "ride_type": type
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, _) = try await URLSession.shared.data(for: request)
        struct RideResponse: Codable { let ride_id: String }
        return try JSONDecoder().decode(RideResponse.self, from: data).ride_id
    }

    public func updateRideStatus(rideId: String, status: String) async throws -> Bool {
        let url = baseURL.appendingPathComponent("driver/ride/status")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: ["ride_id": rideId, "status": status])

        let (_, response) = try await URLSession.shared.data(for: request)
        return (response as? HTTPURLResponse)?.statusCode == 200
    }

    public func cancelRide(rideId: String, reason: String, by: String) async throws -> Bool {
        let url = baseURL.appendingPathComponent("ride/cancel")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: ["ride_id": rideId, "reason": reason, "by": by])

        let (_, response) = try await URLSession.shared.data(for: request)
        return (response as? HTTPURLResponse)?.statusCode == 200
    }

    public func getVehicleInfo() async throws -> VehicleInfo {
        let url = baseURL.appendingPathComponent("driver/vehicle")
        let (data, _) = try await URLSession.shared.data(from: url)
        return try JSONDecoder().decode(VehicleInfo.self, from: data)
    }

    public func updateVehicleInfo(vehicle: VehicleInfo) async throws -> VehicleInfo {
        let url = baseURL.appendingPathComponent("driver/vehicle")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONEncoder().encode(vehicle)

        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(VehicleInfo.self, from: data)
    }

    public func getNearbyDrivers(lat: Double, lng: Double) async throws -> [DriverStatus] {
        let url = baseURL.appendingPathComponent("ride/nearby-drivers?lat=\(lat)&lng=\(lng)")
        let (data, _) = try await URLSession.shared.data(from: url)
        struct NearbyResponse: Codable { let drivers: [DriverStatus] }
        return try JSONDecoder().decode(NearbyResponse.self, from: data).drivers
    }

    public func getActiveSession() async throws -> DriveActiveSession {
        let url = baseURL.appendingPathComponent("user/active-session")
        let (data, _) = try await URLSession.shared.data(from: url)
        return try JSONDecoder().decode(DriveActiveSession.self, from: data)
    }
}
