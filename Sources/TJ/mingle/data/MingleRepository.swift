//
//  MingleRepository.swift
//  TJ
//
//  Swift 5+ Repository for Discovery card fetching, swipes, likes,
//  profile management, photo uploads, and match notifications.
//

import Foundation
import Combine

// MARK: - Models

public struct MinglePhotoDto: Identifiable, Codable, Equatable {
    public let id: String
    public let url: String
    public let isPrimary: Bool

    public init(id: String, url: String, isPrimary: Bool = false) {
        self.id = id
        self.url = url
        self.isPrimary = isPrimary
    }
}

public struct MingleProfileDto: Identifiable, Codable, Equatable {
    public let id: String
    public var name: String?
    public var bio: String?
    public var age: Int?
    public var gender: String?
    public var lookingFor: String?
    public var targetAgeRange: String?
    public var locationName: String?
    public var latitude: Double?
    public var longitude: Double?
    public var radiusKm: Int?
    public var photos: [MinglePhotoDto]?

    public init(
        id: String,
        name: String? = nil,
        bio: String? = nil,
        age: Int? = nil,
        gender: String? = nil,
        lookingFor: String? = nil,
        targetAgeRange: String? = nil,
        locationName: String? = nil,
        latitude: Double? = nil,
        longitude: Double? = nil,
        radiusKm: Int? = nil,
        photos: [MinglePhotoDto]? = nil
    ) {
        self.id = id
        self.name = name
        self.bio = bio
        self.age = age
        self.gender = gender
        self.lookingFor = lookingFor
        self.targetAgeRange = targetAgeRange
        self.locationName = locationName
        self.latitude = latitude
        self.longitude = longitude
        self.radiusKm = radiusKm
        self.photos = photos
    }
}

public struct MingleCandidateDto: Identifiable, Codable, Equatable {
    public let id: String
    public let name: String
    public let age: Int
    public let bio: String?
    public let distanceKm: Double?
    public let photos: [MinglePhotoDto]?

    public init(id: String, name: String, age: Int, bio: String? = nil, distanceKm: Double? = nil, photos: [MinglePhotoDto]? = nil) {
        self.id = id
        self.name = name
        self.age = age
        self.bio = bio
        self.distanceKm = distanceKm
        self.photos = photos
    }
}

public struct MingleSwipeResultDto: Codable, Equatable {
    public let matched: Bool
    public let conversation_id: String?
    public let error: String?
}

public struct MingleSwipeSyncItemDto: Codable, Equatable {
    public let target_user_id: String
    public let action: String
    public let timestamp: Int64

    public init(target_user_id: String, action: String, timestamp: Int64 = Int64(Date().timeIntervalSince1970 * 1000)) {
        self.target_user_id = target_user_id
        self.action = action
        self.timestamp = timestamp
    }
}

public struct MingleIntroDto: Identifiable, Codable, Equatable {
    public let id: String
    public let sender_id: String?
    public let recipient_id: String?
    public let message: String?
    public let conversation_id: String?
    public let status: String?

    public init(id: String, sender_id: String? = nil, recipient_id: String? = nil, message: String? = nil, conversation_id: String? = nil, status: String? = nil) {
        self.id = id
        self.sender_id = sender_id
        self.recipient_id = recipient_id
        self.message = message
        self.conversation_id = conversation_id
        self.status = status
    }
}

public struct MingleActivityDto: Identifiable, Codable, Equatable {
    public let id: String
    public let title: String
    public let description: String
    public let timestamp: Int64

    public init(id: String, title: String, description: String, timestamp: Int64) {
        self.id = id
        self.title = title
        self.description = description
        self.timestamp = timestamp
    }
}

// MARK: - Mingle Repository Protocol & Implementation

public protocol MingleRepositoryProtocol: AnyObject {
    func getMe() async throws -> MingleProfileDto
    func updateProfile(name: String?, bio: String?, age: Int?, gender: String?, lookingFor: String?, targetAgeRange: String?) async throws -> MingleProfileDto
    func updateLocation(lat: Double, lng: Double, radiusKm: Int) async throws -> MingleProfileDto
    func getFeed(lat: Double, lng: Double, radiusKm: Int, offset: Int) async throws -> [MingleCandidateDto]
    func swipe(targetUserId: String, action: String, candidateName: String?) async throws -> MingleSwipeResultDto
    func rewind() async throws -> Bool
    func uploadPhoto(imageData: Data) async throws -> MinglePhotoDto
    func deletePhoto(photoId: String) async throws -> Bool
    func getActivity() async throws -> [MingleActivityDto]
    func sendIntro(recipientId: String, message: String) async throws -> MingleIntroDto
    func listIntros() async throws -> [MingleIntroDto]
    func respondIntro(introId: String, action: String, senderName: String?) async throws -> MingleIntroDto
}

@MainActor
public final class MingleRepository: ObservableObject, MingleRepositoryProtocol {
    public static let shared = MingleRepository()

    @Published public private(set) var myProfile: MingleProfileDto?
    @Published public private(set) var candidates: [MingleCandidateDto] = []

    private let baseURL: URL

    public init(baseURL: URL = URL(string: "https://api.tj.app/mingle")!) {
        self.baseURL = baseURL
    }

    // MARK: - Profile Management

    public func getMe() async throws -> MingleProfileDto {
        let url = baseURL.appendingPathComponent("me")
        let (data, response) = try await URLSession.shared.data(from: url)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }
        struct MeResponse: Codable { let profile: MingleProfileDto }
        let profile = try JSONDecoder().decode(MeResponse.self, from: data).profile
        self.myProfile = profile
        return profile
    }

    public func updateProfile(
        name: String? = nil,
        bio: String? = nil,
        age: Int? = nil,
        gender: String? = nil,
        lookingFor: String? = nil,
        targetAgeRange: String? = nil
    ) async throws -> MingleProfileDto {
        let url = baseURL.appendingPathComponent("profile")
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any?] = [
            "name": name,
            "bio": bio,
            "age": age,
            "gender": gender,
            "looking_for": lookingFor,
            "target_age_range": targetAgeRange
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body.compactMapValues { $0 })

        let (data, response) = try await URLSession.shared.data(for: request)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }

        struct ProfileResponse: Codable { let profile: MingleProfileDto }
        let profile = try JSONDecoder().decode(ProfileResponse.self, from: data).profile
        self.myProfile = profile
        return profile
    }

    public func updateLocation(lat: Double, lng: Double, radiusKm: Int) async throws -> MingleProfileDto {
        let url = baseURL.appendingPathComponent("location")
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: ["latitude": lat, "longitude": lng, "radius_km": radiusKm])

        let (data, response) = try await URLSession.shared.data(for: request)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }

        struct LocationResponse: Codable { let profile: MingleProfileDto }
        let profile = try JSONDecoder().decode(LocationResponse.self, from: data).profile
        self.myProfile = profile
        return profile
    }

    // MARK: - Discovery & Swipes

    public func getFeed(lat: Double, lng: Double, radiusKm: Int = 50, offset: Int = 0) async throws -> [MingleCandidateDto] {
        let url = baseURL.appendingPathComponent("feed?lat=\(lat)&lng=\(lng)&radius=\(radiusKm)&offset=\(offset)")
        let (data, response) = try await URLSession.shared.data(from: url)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }

        struct FeedResponse: Codable { let candidates: [MingleCandidateDto] }
        let list = try JSONDecoder().decode(FeedResponse.self, from: data).candidates
        self.candidates = list
        return list
    }

    public func swipe(targetUserId: String, action: String, candidateName: String? = nil) async throws -> MingleSwipeResultDto {
        let url = baseURL.appendingPathComponent("swipe")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: ["target_user_id": targetUserId, "action": action])

        let (data, response) = try await URLSession.shared.data(for: request)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }

        let result = try JSONDecoder().decode(MingleSwipeResultDto.self, from: data)
        candidates.removeAll { $0.id == targetUserId }
        return result
    }

    public func rewind() async throws -> Bool {
        let url = baseURL.appendingPathComponent("rewind")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"

        let (_, response) = try await URLSession.shared.data(for: request)
        return (response as? HTTPURLResponse)?.statusCode == 200
    }

    // MARK: - Photos & Activity

    public func uploadPhoto(imageData: Data) async throws -> MinglePhotoDto {
        let url = baseURL.appendingPathComponent("photos")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"

        let boundary = "Boundary-\(UUID().uuidString)"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        var body = Data()
        body.append("--\(boundary)\r\nContent-Disposition: form-data; name=\"photo\"; filename=\"photo.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n".data(using: .utf8)!)
        body.append(imageData)
        body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)
        request.httpBody = body

        let (data, response) = try await URLSession.shared.data(for: request)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }

        struct PhotoResponse: Codable { let photo: MinglePhotoDto }
        return try JSONDecoder().decode(PhotoResponse.self, from: data).photo
    }

    public func deletePhoto(photoId: String) async throws -> Bool {
        let url = baseURL.appendingPathComponent("photos/\(photoId)")
        var request = URLRequest(url: url)
        request.httpMethod = "DELETE"

        let (_, response) = try await URLSession.shared.data(for: request)
        return (response as? HTTPURLResponse)?.statusCode == 200
    }

    public func getActivity() async throws -> [MingleActivityDto] {
        let url = baseURL.appendingPathComponent("activity")
        let (data, _) = try await URLSession.shared.data(from: url)
        struct ActivityResponse: Codable { let items: [MingleActivityDto] }
        return try JSONDecoder().decode(ActivityResponse.self, from: data).items
    }

    // MARK: - Intros & Requests

    public func sendIntro(recipientId: String, message: String) async throws -> MingleIntroDto {
        let url = baseURL.appendingPathComponent("intros")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: ["recipient_id": recipientId, "message": message])

        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(MingleIntroDto.self, from: data)
    }

    public func listIntros() async throws -> [MingleIntroDto] {
        let url = baseURL.appendingPathComponent("intros")
        let (data, _) = try await URLSession.shared.data(from: url)
        struct IntrosResponse: Codable { let items: [MingleIntroDto] }
        return try JSONDecoder().decode(IntrosResponse.self, from: data).items
    }

    public func respondIntro(introId: String, action: String, senderName: String? = nil) async throws -> MingleIntroDto {
        let url = baseURL.appendingPathComponent("intros/\(introId)/respond")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: ["action": action])

        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(MingleIntroDto.self, from: data)
    }
}
