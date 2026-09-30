//
//  VocabRepository.swift
//  TJ
//
//  Swift 5+ Repository for Vocabulary card decks across 35 units & 7 languages,
//  spaced repetition status, audio playback using AVSpeechSynthesizer, and user XP/Streak management.
//

import Foundation
import AVFoundation
import Combine

// MARK: - Enums & Models

public enum ContentType: String, Codable {
    case teach
    case example
    case question
}

public struct LessonContent: Identifiable, Codable, Equatable {
    public var id: String { audioText + (type.rawValue) }
    public let type: ContentType
    public let translations: [String: String]
    public let options: [String]
    public let correctAnswerIndex: Int
    public let explanation: String
    public let audioText: String
    public let phonetic: String
    public let partOfSpeech: String

    public init(
        type: ContentType,
        translations: [String: String],
        options: [String] = [],
        correctAnswerIndex: Int = 0,
        explanation: String = "",
        audioText: String = "",
        phonetic: String = "",
        partOfSpeech: String = "noun"
    ) {
        self.type = type
        self.translations = translations
        self.options = options
        self.correctAnswerIndex = correctAnswerIndex
        self.explanation = explanation
        self.audioText = audioText
        self.phonetic = phonetic
        self.partOfSpeech = partOfSpeech
    }
}

public struct Lesson: Identifiable, Codable, Equatable {
    public let id: String
    public let language: String
    public let titleTranslations: [String: String]
    public let categoryTranslations: [String: String]
    public var isCompleted: Bool
    public var content: [LessonContent]

    public init(
        id: String,
        language: String,
        titleTranslations: [String: String],
        categoryTranslations: [String: String],
        isCompleted: Bool = false,
        content: [LessonContent] = []
    ) {
        self.id = id
        self.language = language
        self.titleTranslations = titleTranslations
        self.categoryTranslations = categoryTranslations
        self.isCompleted = isCompleted
        self.content = content
    }
}

public struct VocabUser: Identifiable, Codable, Equatable {
    public let id: String
    public var username: String
    public var email: String
    public var displayName: String
    public var xp: Int
    public var level: Int
    public var streak: Int
    public var streakFreezes: Int
    public var isPremium: Bool
    public var nativeLanguage: String
    public var targetLanguage: String
    public var completedLessonIds: [String]

    public init(
        id: String = "me",
        username: String = "user",
        email: String = "user@example.com",
        displayName: String = "Learner",
        xp: Int = 0,
        level: Int = 1,
        streak: Int = 0,
        streakFreezes: Int = 1,
        isPremium: Bool = false,
        nativeLanguage: String = "English",
        targetLanguage: String = "Spanish",
        completedLessonIds: [String] = []
    ) {
        self.id = id
        self.username = username
        self.email = email
        self.displayName = displayName
        self.xp = xp
        self.level = level
        self.streak = streak
        self.streakFreezes = streakFreezes
        self.isPremium = isPremium
        self.nativeLanguage = nativeLanguage
        self.targetLanguage = targetLanguage
        self.completedLessonIds = completedLessonIds
    }
}

// MARK: - Audio Synthesizer Engine

@MainActor
public final class VocabAudioPlayer: NSObject, ObservableObject, AVSpeechSynthesizerDelegate {
    public static let shared = VocabAudioPlayer()

    private let synthesizer = AVSpeechSynthesizer()
    @Published public private(set) var isSpeaking = false

    override private init() {
        super.init()
        synthesizer.delegate = self
    }

    public func speak(text: String, languageCode: String = "es-ES") {
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: languageCode)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.85

        isSpeaking = true
        synthesizer.speak(utterance)
    }

    public func stop() {
        synthesizer.stopSpeaking(at: .immediate)
        isSpeaking = false
    }

    nonisolated public func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        Task { @MainActor in
            self.isSpeaking = false
        }
    }
}

// MARK: - Vocab Repository Protocol & Implementation

public protocol VocabRepositoryProtocol: AnyObject {
    func getLessons() async throws -> [Lesson]
    func getLessonById(id: String) async throws -> Lesson?
    func completeLesson(lessonId: String, score: Int) async throws
    func getMissedItems() async throws -> [String]
    func getUserProfile() async throws -> VocabUser
    func updateUserXP(delta: Int) async throws
    func useStreakFreeze() async throws -> Bool
}

@MainActor
public final class VocabRepository: ObservableObject, VocabRepositoryProtocol {
    public static let shared = VocabRepository()

    @Published public private(set) var currentUser = VocabUser()
    @Published public private(set) var lessons: [Lesson] = []

    public let languages = ["English", "Spanish", "French", "German", "Italian", "Portuguese", "Afrikaans"]

    private let sampleWords: [String: [String]] = [
        "Spanish": ["hola", "adiós", "gracias", "por favor", "agua", "comida", "amigo", "casa", "escuela", "sol"],
        "French": ["bonjour", "au revoir", "merci", "s'il vous plaît", "eau", "nourriture", "ami", "maison", "école", "soleil"],
        "German": ["hallo", "auf wiedersehen", "danke", "bitte", "wasser", "essen", "freund", "haus", "schule", "sonne"],
        "Italian": ["ciao", "arrivederci", "grazie", "per favore", "acqua", "cibo", "amico", "casa", "scuola", "sole"],
        "Portuguese": ["olá", "adeus", "obrigado", "por favor", "água", "comida", "amigo", "casa", "escola", "sol"],
        "Afrikaans": ["hallo", "totsiens", "dankie", "asseblief", "water", "kos", "vriend", "huis", "skool", "son"]
    ]

    public init() {
        self.lessons = generateDefaultLessons()
    }

    // MARK: - Lesson Operations

    public func getLessons() async throws -> [Lesson] {
        return lessons
    }

    public func getLessonById(id: String) async throws -> Lesson? {
        if let existing = lessons.first(where: { $0.id == id }) {
            var fullLesson = existing
            if fullLesson.content.isEmpty {
                fullLesson.content = generateContentForLesson(id: id, language: existing.language)
            }
            return fullLesson
        }
        return nil
    }

    public func completeLesson(lessonId: String, score: Int) async throws {
        if let index = lessons.firstIndex(where: { $0.id == lessonId }) {
            lessons[index].isCompleted = true
        }
        if !currentUser.completedLessonIds.contains(lessonId) {
            currentUser.completedLessonIds.append(lessonId)
        }
        try await updateUserXP(delta: 20)
    }

    public func getMissedItems() async throws -> [String] {
        return ["comida", "escuela", "eau", "haus"]
    }

    // MARK: - User Profile & Progress

    public func getUserProfile() async throws -> VocabUser {
        return currentUser
    }

    public func updateUserXP(delta: Int) async throws {
        currentUser.xp += delta
        currentUser.level = (currentUser.xp / 100) + 1
    }

    public func useStreakFreeze() async throws -> Bool {
        guard currentUser.streakFreezes > 0 else { return false }
        currentUser.streakFreezes -= 1
        return true
    }

    // MARK: - Generators

    private func generateDefaultLessons() -> [Lesson] {
        var list: [Lesson] = []
        let langCodes = ["EN": "English", "ES": "Spanish", "FR": "French", "DE": "German", "IT": "Italian", "PT": "Portuguese", "AF": "Afrikaans"]

        for (code, lang) in langCodes {
            for unit in 1...35 {
                for lessonNum in 1...5 {
                    let idx = (unit - 1) * 5 + (lessonNum - 1)
                    let lessonId = "lesson_\(idx)_\(code)"
                    list.append(
                        Lesson(
                            id: lessonId,
                            language: lang,
                            titleTranslations: ["English": "Lesson \(lessonNum)", "Spanish": "Lección \(lessonNum)"],
                            categoryTranslations: ["English": "Unit \(unit): Vocabulary", "Spanish": "Unidad \(unit): Vocabulario"],
                            isCompleted: false
                        )
                    )
                }
            }
        }
        return list
    }

    private func generateContentForLesson(id: String, language: String) -> [LessonContent] {
        let words = sampleWords[language] ?? ["word1", "word2", "word3"]
        return words.flatMap { targetWord -> [LessonContent] in
            let teach = LessonContent(
                type: .teach,
                translations: ["English": "Translation of \(targetWord)", language: targetWord],
                audioText: targetWord,
                phonetic: "/\(targetWord)/",
                partOfSpeech: "noun"
            )
            let question = LessonContent(
                type: .question,
                translations: ["English": "What is '\(targetWord)'?", language: targetWord],
                options: [targetWord, "other1", "other2", "other3"],
                correctAnswerIndex: 0,
                explanation: "Correct! '\(targetWord)' is the answer.",
                audioText: targetWord,
                phonetic: "/\(targetWord)/"
            )
            return [teach, question]
        }
    }
}
