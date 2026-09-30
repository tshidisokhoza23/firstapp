//
//  MusicService.swift
//  TJ
//
//  Swift 5+ Audio Playback Engine wrapping AVPlayer / AVQueuePlayer
//  for background streaming and local caching.
//

import Foundation
import AVFoundation
import Combine

// MARK: - Models

public struct Song: Identifiable, Codable, Equatable, Hashable {
    public let id: String
    public let title: String
    public let artist: String
    public let album: String?
    public let duration: TimeInterval
    public let audioUrl: String
    public let coverArtUrl: String?
    public let playsCount: Int
    public let isLiked: Bool

    public init(
        id: String,
        title: String,
        artist: String,
        album: String? = nil,
        duration: TimeInterval = 0.0,
        audioUrl: String,
        coverArtUrl: String? = nil,
        playsCount: Int = 0,
        isLiked: Bool = false
    ) {
        self.id = id
        self.title = title
        self.artist = artist
        self.album = album
        self.duration = duration
        self.audioUrl = audioUrl
        self.coverArtUrl = coverArtUrl
        self.playsCount = playsCount
        self.isLiked = isLiked
    }
}

public struct Playlist: Identifiable, Codable, Equatable {
    public let id: String
    public let name: String
    public let description: String?
    public let coverArtUrl: String?
    public let isPublic: Bool
    public let createdAt: Int64

    public init(
        id: String,
        name: String,
        description: String? = nil,
        coverArtUrl: String? = nil,
        isPublic: Bool = true,
        createdAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    ) {
        self.id = id
        self.name = name
        self.description = description
        self.coverArtUrl = coverArtUrl
        self.isPublic = isPublic
        self.createdAt = createdAt
    }
}

public enum PlaybackState: Equatable {
    case stopped
    case playing
    case paused
    case buffering
    case failed(String)
}

// MARK: - Music Cache Manager

public actor MusicCacheManager {
    public static let shared = MusicCacheManager()

    private let fileManager = FileManager.default
    private let cacheDirectory: URL

    private init() {
        let urls = fileManager.urls(for: .cachesDirectory, in: .userDomainMask)
        self.cacheDirectory = urls[0].appendingPathComponent("MusicCache", isDirectory: true)
        try? fileManager.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
    }

    public func cachedURL(for audioURLString: String) -> URL? {
        guard let url = URL(string: audioURLString) else { return nil }
        let filename = url.lastPathComponent
        let localURL = cacheDirectory.appendingPathComponent(filename)
        return fileManager.fileExists(atPath: localURL.path) ? localURL : nil
    }

    public func cacheAudio(from remoteURL: URL) async throws -> URL {
        let filename = remoteURL.lastPathComponent
        let localURL = cacheDirectory.appendingPathComponent(filename)

        if fileManager.fileExists(atPath: localURL.path) {
            return localURL
        }

        let (tempURL, _) = try await URLSession.shared.download(from: remoteURL)
        try fileManager.moveItem(at: tempURL, to: localURL)
        return localURL
    }

    public func clearCache() {
        try? fileManager.removeItem(at: cacheDirectory)
        try? fileManager.createDirectory(at: cacheDirectory, withIntermediateDirectories: true)
    }
}

// MARK: - Music Service & Playback Engine

@MainActor
public final class MusicService: ObservableObject {
    public static let shared = MusicService()

    @Published public private(set) var currentSong: Song?
    @Published public private(set) var queue: [Song] = []
    @Published public private(set) var currentIndex: Int = -1
    @Published public private(set) var playbackState: PlaybackState = .stopped
    @Published public private(set) var currentTime: TimeInterval = 0.0
    @Published public private(set) var duration: TimeInterval = 0.0
    @Published public private(set) var volume: Float = 1.0 {
        didSet { player?.volume = volume }
    }

    private var player: AVQueuePlayer?
    private var timeObserverToken: Any?
    private var cancellables = Set<AnyCancellable>()

    public init() {
        setupAudioSession()
    }

    deinit {
        removeTimeObserver()
    }

    private func setupAudioSession() {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .default, options: [.allowAirPlay, .allowBluetooth])
            try session.setActive(true)
        } catch {
            print("[MusicService] Audio session setup error: \(error)")
        }
    }

    // MARK: - Playback Controls

    public func setQueue(_ songs: [Song], startAt index: Int = 0) async {
        self.queue = songs
        guard index >= 0 && index < songs.count else { return }
        self.currentIndex = index
        await playSong(songs[index])
    }

    public func playSong(_ song: Song) async {
        self.currentSong = song
        self.playbackState = .buffering

        var audioURL: URL? = URL(string: song.audioUrl)
        if let localURL = await MusicCacheManager.shared.cachedURL(for: song.audioUrl) {
            audioURL = localURL
        } else if let remoteURL = audioURL {
            Task.detached {
                _ = try? await MusicCacheManager.shared.cacheAudio(from: remoteURL)
            }
        }

        guard let url = audioURL else {
            self.playbackState = .failed("Invalid audio URL")
            return
        }

        let playerItem = AVPlayerItem(url: url)
        if player == nil {
            player = AVQueuePlayer(playerItem: playerItem)
            setupPlayerObservers()
        } else {
            player?.removeAllItems()
            player?.insert(playerItem, after: nil)
        }

        player?.play()
        self.playbackState = .playing
    }

    public func play() {
        guard let player = player else { return }
        player.play()
        playbackState = .playing
    }

    public func pause() {
        guard let player = player else { return }
        player.pause()
        playbackState = .paused
    }

    public func togglePlayPause() {
        if playbackState == .playing {
            pause()
        } else {
            play()
        }
    }

    public func next() async {
        guard !queue.isEmpty, currentIndex + 1 < queue.count else { return }
        currentIndex += 1
        await playSong(queue[currentIndex])
    }

    public func previous() async {
        guard !queue.isEmpty, currentIndex - 1 >= 0 else { return }
        currentIndex -= 1
        await playSong(queue[currentIndex])
    }

    public func seek(to time: TimeInterval) {
        guard let player = player else { return }
        let cmTime = CMTime(seconds: time, preferredTimescale: 600)
        player.seek(to: cmTime) { [weak self] completed in
            if completed {
                self?.currentTime = time
            }
        }
    }

    // MARK: - Private Player Observers

    private func setupPlayerObservers() {
        removeTimeObserver()
        let interval = CMTime(seconds: 0.5, preferredTimescale: 600)
        timeObserverToken = player?.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] time in
            guard let self = self else { return }
            self.currentTime = time.seconds
            if let currentItem = self.player?.currentItem {
                let itemDuration = currentItem.duration.seconds
                if !itemDuration.isNaN {
                    self.duration = itemDuration
                }
            }
        }

        NotificationCenter.default.publisher(for: .AVPlayerItemDidPlayToEndTime)
            .sink { [weak self] _ in
                Task { @MainActor [weak self] in
                    await self?.next()
                }
            }
            .store(in: &cancellables)
    }

    private func removeTimeObserver() {
        if let token = timeObserverToken {
            player?.removeTimeObserver(token)
            timeObserverToken = nil
        }
    }
}

// MARK: - Music Repository API Client

public actor MusicRepository {
    public static let shared = MusicRepository()
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
        struct SongsResponse: Codable { let songs: [Song] }
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
        struct PlaylistsResponse: Codable { let playlists: [Playlist] }
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
        struct SongsResponse: Codable { let songs: [Song] }
        return try JSONDecoder().decode(SongsResponse.self, from: data).songs
    }
}
