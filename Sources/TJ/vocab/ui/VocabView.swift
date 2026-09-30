import SwiftUI

// MARK: - Vocab Word Model

public struct VocabCardItem: Identifiable, Hashable {
    public let id: String
    public let word: String
    public let pronunciation: String
    public let partOfSpeech: String
    public let definition: String
    public let exampleSentence: String
    public let synonyms: [String]

    public init(id: String, word: String, pronunciation: String, partOfSpeech: String, definition: String, exampleSentence: String, synonyms: [String]) {
        self.id = id
        self.word = word
        self.pronunciation = pronunciation
        self.partOfSpeech = partOfSpeech
        self.definition = definition
        self.exampleSentence = exampleSentence
        self.synonyms = synonyms
    }
}

// MARK: - VocabView

public struct VocabView: View {
    @State private var selectedDeck: String = "Tech Terms"
    @State private var isFlipped: Bool = false
    @State private var currentCardIndex: Int = 0
    @State private var streakDays: Int = 14
    @State private var cardsMasteredToday: Int = 8
    @State private var isAudioPlaying: Bool = false

    private let decks = ["Tech Terms", "Business English", "Spanish Basics", "JLPT N3 Vocab"]

    @State private var cards: [VocabCardItem] = [
        VocabCardItem(id: "v1", word: "Idempotent", pronunciation: "/ˌaɪ.dɛmˈpoʊ.tənt/", partOfSpeech: "Adjective", definition: "Denoting an element or operation which, when applied multiple times, produces the same result as if applied once.", exampleSentence: "HTTP GET and PUT requests are designed to be idempotent in REST architecture.", synonyms: ["Invariant", "Consistent"]),
        VocabCardItem(id: "v2", word: "Asynchronous", pronunciation: "/eɪˈsɪŋ.krə.nəs/", partOfSpeech: "Adjective", definition: "Not occurring at the same time; executing tasks independently in the background without blocking execution.", exampleSentence: "Kotlin Coroutines enable asynchronous non-blocking network requests.", synonyms: ["Non-blocking", "Concurrent"]),
        VocabCardItem(id: "v3", word: "Polymorphism", pronunciation: "/ˌpɒl.iˈmɔː.fɪ.zəm/", partOfSpeech: "Noun", definition: "The provision of a single interface to entities of different types.", exampleSentence: "Interface inheritance allows runtime polymorphism in object-oriented programming.", synonyms: ["Multiformity", "Variation"]),
        VocabCardItem(id: "v4", word: "Heuristic", pronunciation: "/hjʊəˈrɪs.tɪk/", partOfSpeech: "Noun", definition: "A practical method or shortcut that is not guaranteed to be optimal, but is sufficient for immediate goals.", exampleSentence: "The pathfinding algorithm uses a Manhattan distance heuristic.", synonyms: ["Rule of thumb", "Trial-and-error"])
    ]

    public init() {}

    private var currentCard: VocabCardItem? {
        if currentCardIndex < cards.count {
            return cards[currentCardIndex]
        }
        return nil
    }

    public var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                // Deck Selector & Progress Summary
                deckHeaderBar

                // Daily Progress Bar
                progressStatsCard

                // Card Area
                if let card = currentCard {
                    Spacer()

                    flashcardView(card)
                        .padding(.horizontal, 24)

                    Spacer()

                    // Rating Buttons (Spacing Repetition)
                    ratingButtonsRow
                } else {
                    Spacer()
                    deckCompletedView
                    Spacer()
                }
            }
            .padding(.vertical, 12)
            .navigationTitle("Vocab Practice")
            .background(Color(.systemGroupedBackground).ignoresSafeArea())
        }
    }

    // MARK: - Subviews

    private var deckHeaderBar: some View {
        HStack {
            Menu {
                ForEach(decks, id: \.self) { deck in
                    Button(action: {
                        selectedDeck = deck
                        currentCardIndex = 0
                        isFlipped = false
                    }) {
                        HStack {
                            Text(deck)
                            if selectedDeck == deck { Image(systemName: "checkmark") }
                        }
                    }
                }
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "book.fill")
                        .foregroundColor(.blue)
                    Text(selectedDeck)
                        .font(.headline.bold())
                        .foregroundColor(.primary)
                    Image(systemName: "chevron.down")
                        .font(.caption.bold())
                        .foregroundColor(.secondary)
                }
            }

            Spacer()

            if let card = currentCard {
                Text("\(currentCardIndex + 1) / \(cards.count)")
                    .font(.subheadline.bold())
                    .foregroundColor(.secondary)
            }
        }
        .padding(.horizontal)
    }

    private var progressStatsCard: some View {
        HStack(spacing: 16) {
            HStack(spacing: 8) {
                Image(systemName: "flame.fill")
                    .foregroundColor(.orange)
                    .font(.title3)
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(streakDays) Days")
                        .font(.subheadline.bold())
                    Text("Streak")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
            .frame(maxWidth: .infinity)

            Divider().frame(height: 30)

            HStack(spacing: 8) {
                Image(systemName: "checkmark.seal.fill")
                    .foregroundColor(.green)
                    .font(.title3)
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(cardsMasteredToday) Words")
                        .font(.subheadline.bold())
                    Text("Mastered Today")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
            .frame(maxWidth: .infinity)
        }
        .padding(.vertical, 12)
        .background(Color(.secondarySystemGroupedBackground))
        .cornerRadius(16)
        .padding(.horizontal)
    }

    private func flashcardView(_ card: VocabCardItem) -> some View {
        ZStack {
            if !isFlipped {
                // Front Side
                VStack(spacing: 20) {
                    HStack {
                        Text(card.partOfSpeech.uppercased())
                            .font(.caption2.bold())
                            .foregroundColor(.purple)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(Color.purple.opacity(0.12))
                            .cornerRadius(8)

                        Spacer()

                        Button(action: {
                            isAudioPlaying = true
                            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                                isAudioPlaying = false
                            }
                        }) {
                            Image(systemName: isAudioPlaying ? "speaker.wave.3.fill" : "speaker.wave.2.fill")
                                .font(.title3)
                                .foregroundColor(.blue)
                        }
                    }

                    Spacer()

                    Text(card.word)
                        .font(.system(size: 38, weight: .bold))
                        .foregroundColor(.primary)

                    Text(card.pronunciation)
                        .font(.title3)
                        .foregroundColor(.secondary)

                    Spacer()

                    Label("Tap card to reveal definition", systemImage: "hand.tap.fill")
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                .padding(24)
                .frame(maxWidth: .infinity)
                .frame(height: 360)
                .background(Color(.secondarySystemGroupedBackground))
                .cornerRadius(24)
                .shadow(radius: 6)
            } else {
                // Back Side
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Text("DEFINITION")
                            .font(.caption2.bold())
                            .foregroundColor(.blue)

                        Spacer()

                        Text(card.word)
                            .font(.subheadline.bold())
                            .foregroundColor(.secondary)
                    }

                    Text(card.definition)
                        .font(.title3.weight(.medium))
                        .foregroundColor(.primary)

                    Divider()

                    Text("EXAMPLE")
                        .font(.caption2.bold())
                        .foregroundColor(.orange)

                    Text("\"\(card.exampleSentence)\"")
                        .font(.subheadline)
                        .italic()
                        .foregroundColor(.secondary)

                    Spacer()

                    HStack {
                        Text("Synonyms:")
                            .font(.caption.bold())
                        ForEach(card.synonyms, id: \.self) { syn in
                            Text(syn)
                                .font(.caption)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 3)
                                .background(Color.blue.opacity(0.1))
                                .cornerRadius(6)
                        }
                    }
                }
                .padding(24)
                .frame(maxWidth: .infinity)
                .frame(height: 360)
                .background(Color(.secondarySystemGroupedBackground))
                .cornerRadius(24)
                .shadow(radius: 6)
            }
        }
        .rotation3DEffect(
            .degrees(isFlipped ? 180 : 0),
            axis: (x: 0.0, y: 1.0, z: 0.0)
        )
        .onTapGesture {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.7)) {
                isFlipped.toggle()
            }
        }
    }

    private var ratingButtonsRow: some View {
        HStack(spacing: 16) {
            Button(action: { processRating(difficulty: .again) }) {
                VStack(spacing: 4) {
                    Text("Again")
                        .font(.subheadline.bold())
                    Text("< 1 min")
                        .font(.caption2)
                }
                .foregroundColor(.red)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(Color.red.opacity(0.12))
                .cornerRadius(16)
            }

            Button(action: { processRating(difficulty: .good) }) {
                VStack(spacing: 4) {
                    Text("Good")
                        .font(.subheadline.bold())
                    Text("1 day")
                        .font(.caption2)
                }
                .foregroundColor(.orange)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(Color.orange.opacity(0.12))
                .cornerRadius(16)
            }

            Button(action: { processRating(difficulty: .easy) }) {
                VStack(spacing: 4) {
                    Text("Easy")
                        .font(.subheadline.bold())
                    Text("4 days")
                        .font(.caption2)
                }
                .foregroundColor(.green)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(Color.green.opacity(0.12))
                .cornerRadius(16)
            }
        }
        .padding(.horizontal, 24)
    }

    private var deckCompletedView: some View {
        VStack(spacing: 16) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 72))
                .foregroundColor(.green)

            Text("Deck Practice Complete! 🎉")
                .font(.title2.bold())

            Text("You've reviewed all cards in \(selectedDeck). Great job maintaining your streak!")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Button("Restart Deck") {
                currentCardIndex = 0
                isFlipped = false
            }
            .fontWeight(.bold)
            .foregroundColor(.white)
            .padding()
            .frame(maxWidth: .infinity)
            .background(Color.blue)
            .cornerRadius(16)
            .padding(.horizontal, 40)
        }
    }

    private enum RatingDifficulty {
        case again, good, easy
    }

    private func processRating(difficulty: RatingDifficulty) {
        withAnimation {
            if difficulty == .easy {
                cardsMasteredToday += 1
            }
            isFlipped = false
            currentCardIndex += 1
        }
    }
}

#Preview {
    VocabView()
}
