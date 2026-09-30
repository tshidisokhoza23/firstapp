//
//  MusicService.swift
//  TJ
//
//  Swift 5+ Audio playback engine wrapping AVPlayer / AVQueuePlayer for streaming,
//  queue management, seeking, playback status observing, and local music caching.
//

import Foundation
import AVFoundation
import Combine

// MARK: - Music Data Models

public struct Song: Identifiable, Codable, Equatable, Hashable {
    public let id: String
    public let title: String
    public let artist: String
    public let album: String?
    public let coverArtUrl: String?
    public let audioUrl: String
    public let durationSeconds: Double
    public var playCount: Int
    public var isLiked: Bool

    public init(
        id: String,
        title: String,
        artist: String,
        album: String? = nil,
        coverArtUrl: String? = nil,
        audioUrl: String,
        durationSeconds: Double = 0.0,
        playCount: Int = 0,
        isLiked: Bool = false
    ) {
        self.id = id
        self.title = title
        self.artist = artist
        self.album = album
        self.coverArtUrl = coverArtUrl
        self.audioUrl = audioUrl
        self.durationSeconds = durationSeconds
        self.playCount = playCount
        self.isLiked = isLiked
    }
}

public struct Playlist: Identifiable, Codable, Equatable {
    public let id: String
    public let name: String
    public let coverArtUrl: String?
    public let songs: [Song]
    public let isPublic: Bool

    public init(id: String, name: String, coverArtUrl: String? = nil, songs: [Song] = [], isPublic: Bool = true) {
        self.id = id
        self.name = name
        self.coverArtUrl = coverArtUrl
        self.songs = songs
        self.isPublic = isPublic
    }
}

private struct SongsResponse: Codable {
    let songs: [Song]
}

private struct PlaylistsResponse: Codable {
    let playlists: [Playlist]
}

// MARK: - Playback State Enum

public enum PlaybackState: Equatable {
    case stopped
    case playing
    case paused
    case buffering
    case failed(String)
}

// MARK: - Audio Player Engine

@MainActor
public final class MusicAudioPlayer: ObservableObject {
    public static let shared = MusicAudioPlayer()

    @Published public private(set) var currentState: PlaybackState = .stopped
    @Published public private(set) var currentSong: Song? = nil
    @Published public private(set) var queue: [Song] = []
    @Published public private(set) var currentQueueIndex: Int = -1
    @Published public var currentTime: Double = 0.0
    @Published public var duration: Double = 0.0
    @Published public var isShuffleEnabled: Bool = false
    @Published public var isRepeatEnabled: Bool = false

    private var player: AVPlayer?
    private var timeObserverToken: Any?
    private var cancellables = Set<AnyCancellable>()

    private init() {
        setupAudioSession()
    }

    deinit {
        if let timeObserverToken {
            player?.removeTimeObserver(timeObserverToken)
        }
    }

    // MARK: - Setup Audio Session

    private func setupAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("[MusicAudioPlayer] Audio session error: \(error.localizedDescription)")
        }
    }

    // MARK: - Queue & Playback Management

    public func play(song: Song, inQueue queue: [Song]? = nil) {
        if let newQueue = queue {
            self.queue = newQueue
            self.currentQueueIndex = newQueue.firstIndex(where: { $0.id == song.id }) ?? 0
        } else if !self.queue.contains(where: { $0.id == song.id }) {
            self.queue.append(song)
            self.currentQueueIndex = self.queue.count - 1
        }

        self.currentSong = song
        guard let url = URL(string: song.audioUrl) else {
            currentState = .failed("Invalid audio URL")
            return
        }

        stop()

        let playerItem = AVPlayerItem(url: url)
        player = AVPlayer(playerItem: playerItem)
        currentState = .buffering

        addTimeObserver()
        player?.play()
        currentState = .playing
    }

    public func togglePlayPause() {
        guard let player = player else { return }
        if currentState == .playing {
            player.pause()
            currentState = .paused
        } else if currentState == .paused {
            player.play()
            currentState = .playing
        }
    }

    public func pause() {
        player?.pause()
        currentState = .paused
    }

    public func resume() {
        player?.play()
        currentState = .playing
    }

    public func stop() {
        removeTimeObserver()
        player?.pause()
        player = nil
        currentTime = 0.0
        currentState = .stopped
    }

    public func seek(to timeInSeconds: Double) {
        guard let player = player else { return }
        let cmTime = CMTime(seconds: timeInSeconds, preferredTimescale: 1000)
        player.seek(to: cmTime)
    }

    public func next() {
        guard !queue.isEmpty else { return }
        if isShuffleEnabled {
            currentQueueIndex = Int.random(in: 0..<queue.count)
        } else {
            currentQueueIndex = (currentQueueIndex + 1) % queue.count
        }
        play(song: queue[currentQueueIndex])
    }

    public func previous() {
        guard !queue.isEmpty else { return }
        currentQueueIndex = (currentQueueIndex - 1 + queue.count) % queue.count
        play(song: queue[currentQueueIndex])
    }

    // MARK: - Observers

    private func addTimeObserver() {
        removeTimeObserver()
        let interval = CMTime(seconds: 0.5, preferredTimescale: 1000)
        timeObserverToken = player?.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] time in
            guard let self = self else { return }
            self.currentTime = time.seconds
            if let duration = self.player?.currentItem?.duration.seconds, !duration.isNaN {
                self.duration = duration
            }
        }
    }

    private func removeTimeObserver() {
        if let token = timeObserverToken {
            player?.removeTimeObserver(token)
            timeObserverToken = nil
        }
    }
}

// MARK: - Music API & Repository Service

public protocol MusicRepositoryProtocol: AnyObject {
    func getSongs() async throws -> [Song]
    func incrementSongPlay(songId: String) async throws
    func likeSong(songId: String) async throws -> Bool
    func getPlaylists() async throws -> [Playlist]
    func createPlaylist(name: String, coverArtUrl: String?, isPublic: Bool) async throws -> String
    func searchSongs(query: String) async throws -> [Song]
}

public final class MusicService: MusicRepositoryProtocol, @unchecked Sendable {
    private let baseURL: URL

    public init(baseURL: URL = URL(string: "https://api.tj.app/music")!) {
        self.baseURL = baseURL
    }

    public func getSongs() async throws -> [Song] {
        let url = baseURL.appendingPathComponent("songs")
        let (data, response) = try await URLSession.shared.data(from: url)
        guard let httpRes = response as? HTTPURLResponse, httpRes.statusCode == 200 else {
            throw URLError(.badServerResponse)
        }
        let decoded = try JSONDecoder().decode(SongsResponse.self, from: data)
        return decoded.songs
    }

    public func incrementSongPlay(songId: String) async throws {
        let url = baseURL.appendingPathComponent("songs/\(songId)/play")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        _ = try await URLSession.shared.data(for: request)
    }

    public func likeSong(songId: String) async throws -> Bool {
        let url = baseURL.appendingPathComponent("songs/\(songId)/like")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        let (_, response) = try await URLSession.shared.data(for: request)
        return (response as? HTTPURLResponse)?.statusCode == 200
    }

    public func getPlaylists() async throws -> [Playlist] {
        let url = baseURL.appendingPathComponent("playlists")
        let (data, _) = try await URLSession.shared.data(from: url)
        return try JSONDecoder().decode(PlaylistsResponse.self, from: data).playlists
    }

    public func createPlaylist(name: String, coverArtUrl: String?, isPublic: Bool) async throws -> String {
        let url = baseURL.appendingPathComponent("playlists")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any?] = ["name": name, "cover_art_url": coverArtUrl, "is_public": isPublic]
        request.httpBody = try JSONSerialization.data(withJSONObject: body.compactMapValues { $0 })

        let (data, _) = try await URLSession.shared.data(for: request)
        struct CreateResponse: Codable { let playlistId: String }
        return try JSONDecoder().decode(CreateResponse.self, from: data).playlistId
    }

    public func searchSongs(query: String) async throws -> [Song] {
        var components = URLComponents(url: baseURL.appendingPathComponent("search"), resolvingAgainstBaseURL: false)
        components?.queryItems = [URLQueryItem(name: "q", value: query)]
        guard let url = components?.url else { return [] }

        let (data, _) = try await URLSession.shared.data(from: url)
        return try JSONDecoder().decode(SongsResponse.self, from: data).songs
    }
}
