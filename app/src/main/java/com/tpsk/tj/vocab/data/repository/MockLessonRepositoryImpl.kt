package com.tpsk.tj.vocab.data.repository

import com.tpsk.tj.vocab.data.local.dao.LessonProgressDao
import com.tpsk.tj.vocab.model.ContentType
import com.tpsk.tj.vocab.model.Lesson
import com.tpsk.tj.vocab.model.LessonContent
import com.tpsk.tj.vocab.domain.repository.LessonRepository
import com.tpsk.tj.vocab.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import com.tpsk.tj.common.util.Resource

import javax.inject.Inject

class MockLessonRepositoryImpl @Inject constructor(
    private val lessonProgressDao: LessonProgressDao
) : LessonRepository {

    private val nativeLanguages = listOf("English", "Spanish", "French", "German", "Italian", "Portuguese", "Afrikaans")

    private val veryBasicVocab = listOf(
        "hello", "goodbye", "please", "thanks", "yes", "no", "i", "you", "he", "she", "it", "we", "they",
        "who", "what", "where", "when", "why", "how", "name", "friend", "family", "man", "woman", "child",
        "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "day", "week", "month", "year"
    )

    private val basicNouns = listOf(
        "house", "dog", "cat", "car", "water", "food", "bread", "milk", "coffee", "tea", "book", "pen", "phone",
        "city", "school", "office", "street", "store", "money", "price", "room", "door", "window", "bed", "kitchen",
        "apple", "banana", "orange", "meat", "fish", "chicken", "salt", "sugar", "egg", "fruit", "vegetable",
        "shirt", "shoes", "pants", "hat", "bag", "clock", "sun", "moon", "star", "sky", "rain", "snow", "fire"
    )

    private val intermediateNouns = listOf(
        "company", "project", "meeting", "career", "success", "problem", "solution", "result", "fact", "idea",
        "culture", "history", "art", "music", "movie", "sport", "game", "nature", "tree", "flower", "bird", "forest",
        "health", "doctor", "hospital", "medicine", "pain", "travel", "trip", "plane", "train", "hotel", "beach",
        "passport", "ticket", "economy", "growth", "law", "rule", "government", "vote", "leader", "society",
        "keyboard", "mouse", "screen", "internet", "website", "app", "message", "video", "photo", "camera"
    )

    private val advancedNouns = listOf(
        "concept", "thought", "mind", "soul", "power", "freedom", "justice", "truth", "beauty", "honor",
        "research", "evidence", "theory", "discovery", "environment", "pollution", "climate", "energy", "resource",
        "relationship", "conflict", "agreement", "debate", "opinion", "reason", "purpose", "memory", "experience",
        "childhood", "philosophy", "psychology", "strategy", "innovation", "structure", "perspective", "challenge",
        "opportunity", "threat", "benefit", "consequence", "influence", "impact", "reaction", "emotion", "passion"
    )

    private val basicAdjectives = listOf(
        "good", "bad", "big", "small", "hot", "cold", "happy", "sad", "fast", "slow", "new", "old", "young",
        "high", "low", "long", "short", "easy", "hard", "rich", "poor", "clean", "dirty", "strong", "weak"
    )

    private val intermediateAdjectives = listOf(
        "beautiful", "ugly", "funny", "serious", "smart", "brave", "quiet", "loud", "bright", "dark",
        "cheap", "expensive", "modern", "classic", "safe", "dangerous", "important", "difficult", "perfect",
        "active", "lazy", "lucky", "famous", "busy", "free"
    )

    private val advancedAdjectives = listOf(
        "complex", "simple", "vivid", "subtle", "intense", "gentle", "cruel", "loyal", "selfish", "generous",
        "frequent", "rare", "massive", "tiny", "ancient", "future", "global", "local", "legal", "illegal",
        "sustainable", "artificial", "natural", "efficient", "creative", "logical", "emotional", "spiritual"
    )

    private val basicVerbs = listOf(
        "go", "come", "eat", "drink", "sleep", "wake", "work", "play", "read", "write", "speak", "learn", "study",
        "buy", "sell", "pay", "see", "hear", "feel", "think", "know", "want", "need", "give", "take"
    )

    private val intermediateVerbs = listOf(
        "travel", "visit", "stay", "live", "start", "stop", "finish", "build", "change", "create", "design",
        "find", "lose", "win", "fail", "understand", "explain", "remember", "forget", "believe", "hope", "wish",
        "become", "try", "help"
    )

    private val advancedVerbs = listOf(
        "discuss", "debate", "analyze", "evaluate", "discover", "invent", "improve", "increase", "decrease",
        "provide", "require", "develop", "manage", "organize", "negotiate", "persuade", "influence", "affect",
        "challenge", "succeed", "prepare", "decide", "achieve", "maintain", "resolve"
    )

    private val basicAdverbs = listOf(
        "always", "never", "now", "today", "here", "there", "fast", "slowly", "very", "too"
    )

    private val intermediateAdverbs = listOf(
        "often", "sometimes", "usually", "well", "badly", "clearly", "quietly", "loudly", "easily", "hard"
    )

    private val advancedAdverbs = listOf(
        "completely", "partially", "extremely", "barely", "frequently", "rarely", "locally", "globally", "perfectly", "roughly"
    )

    internal val baseVocab = (basicNouns + intermediateNouns + advancedNouns).distinct()
    private val colorVocab = listOf("red", "blue", "green", "yellow", "black", "white", "brown", "gray", "orange", "purple", "pink", "silver", "gold")
    internal val adjectiveVocab = (basicAdjectives + intermediateAdjectives + advancedAdjectives + colorVocab).distinct()
    private val verbVocab = (basicVerbs + intermediateVerbs + advancedVerbs).distinct()
    private val adverbVocab = (basicAdverbs + intermediateAdverbs + advancedAdverbs).distinct()


    internal val vocabularyList: List<String> by lazy {
        val fullList = mutableListOf<String>()
        for (i in 1..35) {
            fullList.addAll(getWordsForUnit(i))
        }
        
        val distinctList = fullList.distinct()
        if (distinctList.size < 5250) {
            val combined = mutableListOf<String>()
            combined.addAll(distinctList)
            
            val adjs = adjectiveVocab.shuffled()
            val nouns = baseVocab.shuffled()
            var aIdx = 0
            var nIdx = 0
            
            while (combined.size < 5250) {
                val combo = "${adjs[aIdx % adjs.size]} ${nouns[nIdx % nouns.size]}"
                if (!combined.contains(combo)) {
                    combined.add(combo)
                }
                nIdx++
                if (nIdx >= nouns.size) {
                    nIdx = 0
                    aIdx++
                }
                if (aIdx >= adjs.size) break
            }
            
            while (combined.size < 5250) {
                combined.add("item ${combined.size + 1}")
            }
            combined
        } else {
            distinctList.take(5250)
        }
    }

    internal fun getWordsForUnit(unitNum: Int): List<String> {
        val base = when (unitNum) {
            1 -> listOf("hello", "goodbye", "please", "thanks", "yes", "no", "i", "you", "he", "she", "it", "we", "they", "name", "where is the bathroom", "i am lost", "can you help me") 
            2 -> listOf("mother", "father", "son", "daughter", "brother", "sister", "parent", "family", "child", "kid", "wife", "relationship", "how much does it cost", "i dont understand")
            3 -> listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "time", "hour", "minute", "day", "week", "month", "year", "morning", "night", "afternoon", "evening", "today", "tomorrow", "yesterday")
            4 -> listOf("water", "food", "bread", "milk", "apple", "eat", "drink", "coffee", "tea", "sugar", "salt", "meat", "fish", "chicken", "egg", "vegetable", "fruit", "banana", "orange", "hungry", "thirsty")
            5 -> listOf("go", "come", "sleep", "wake", "work", "home", "school", "study", "learn", "live", "stay", "walk", "run", "house", "apartment", "room", "bed", "door", "window", "kitchen")
            6 -> listOf("red", "blue", "green", "yellow", "black", "white", "brown", "gray", "orange", "purple", "pink", "shirt", "shoes", "socks", "hat", "pants", "dress", "bag", "wear")
            7 -> listOf("city", "town", "street", "road", "place", "area", "country", "world", "state", "here", "there", "where", "near", "far", "left", "right", "straight")
            8 -> listOf("body", "head", "face", "eye", "hand", "foot", "arm", "heart", "health", "doctor", "hospital", "sick", "pain", "medicine", "feel", "touch", "see", "hear")
            9 -> listOf("sun", "moon", "star", "sky", "cloud", "rain", "snow", "wind", "storm", "weather", "hot", "cold", "winter", "summer", "spring", "autumn", "fire", "ice")
            10 -> listOf("money", "buy", "sell", "pay", "price", "cheap", "expensive", "cost", "wallet", "bank", "card", "market", "shop", "store", "rich", "poor", "please speak slowly", "what is this")
            11 -> listOf("job", "work", "office", "business", "company", "boss", "worker", "salary", "team", "meeting", "desk", "computer", "email", "phone", "service", "project")
            12 -> listOf("play", "game", "music", "sport", "hobby", "read", "book", "watch", "movie", "dance", "sing", "art", "paint", "photo", "camera", "fun")
            13 -> listOf("travel", "trip", "plane", "train", "car", "bus", "ticket", "hotel", "beach", "mountain", "river", "ocean", "visit", "passport", "bag", "map")
            14 -> listOf("phone", "internet", "website", "app", "message", "call", "video", "screen", "keyboard", "mouse", "online", "game", "share", "send", "receive")
            15 -> listOf("nature", "tree", "flower", "animal", "bird", "fish", "dog", "cat", "horse", "forest", "garden", "earth", "world", "environment", "clean")
            16 -> listOf("happy", "sad", "angry", " scared", "tired", "excited", "brave", "smart", "funny", "serious", "kind", "love", "hate", "feel", "think", "mind")
            17 -> listOf("study", "learn", "teach", "teacher", "student", "class", "school", "college", "university", "test", "exam", "book", "paper", "pencil", "write")
            18 -> listOf("news", "paper", "radio", "tv", "movie", "film", "actor", "music", "song", "singer", "art", "museum", "culture", "history", "story", "book")
            19 -> listOf("people", "society", "person", "group", "community", "public", "law", "rule", "government", "vote", "leader", "peace", "war", "freedom", "justice")
            20 -> listOf("law", "police", "court", "legal", "crime", "safe", "security", "right", "duty", "truth", "honor", "state", "nation", "army", "military")
            21 -> listOf("history", "past", "ancient", "old", "new", "change", "future", "time", "world", "event", "king", "queen", "castle", "war", "peace", "culture")
            22 -> listOf("science", "space", "planet", "star", "moon", "sun", "energy", "nature", "fact", "idea", "test", "result", "think", "research", "lab")
            23 -> listOf("sport", "game", "play", "win", "lose", "run", "jump", "team", "ball", "match", "gym", "fitness", "health", "strong", "fast", "coach")
            24 -> listOf("style", "fashion", "clothes", "shirt", "dress", "shoes", "hat", "color", "look", "beautiful", "ugly", "new", "classic", "modern", "design")
            25 -> listOf("business", "money", "market", "trade", "buy", "sell", "price", "bank", "card", "cost", "value", "rich", "poor", "economy", "growth")
            26 -> listOf("cook", "recipe", "kitchen", "food", "eat", "sugar", "salt", "pepper", "meat", "fruit", "vegetable", "bread", "milk", "egg", "oil", "taste")
            27 -> listOf("house", "home", "building", "room", "door", "window", "wall", "roof", "floor", "garden", "street", "city", "town", "village", "area")
            28 -> listOf("holiday", "party", "celebrate", "gift", "tradition", "culture", "family", "friend", "fun", "dance", "song", "food", "drink", "special")
            29 -> listOf("music", "song", "band", "play", "listen", "sound", "voice", "loud", "quiet", "concert", "radio", "instrument", "guitar", "piano", "drum")
            30 -> listOf("write", "word", "page", "book", "story", "poem", "letter", "email", "paper", "read", "pen", "pencil", "language", "speak", "meaning")
            31 -> listOf("future", "hope", "dream", "plan", "goal", "success", "work", "life", "wish", "want", "need", "aim", "career", "become", "try")
            32 -> listOf("past", "memory", "remember", "forget", "old", "young", "before", "history", "event", "childhood", "life", "experience", "once", "ago")
            33 -> listOf("think", "idea", "opinion", "agree", "disagree", "discuss", "debate", "reason", "fact", "truth", "true", "false", "maybe", "sure", "point")
            34 -> listOf("concept", "thought", "mind", "soul", "power", "nature", "space", "time", "life", "death", "freedom", "justice", "truth", "beauty", "honor")
            35 -> listOf("where is the hotel", "i need a doctor", "call the police", "i dont understand", "please speak slowly", "how much does it cost", "i am lost", "can you help me")
            else -> emptyList()
        }
        
        val pool = when {
            unitNum <= 10 -> (veryBasicVocab + basicNouns + basicVerbs + basicAdjectives + basicAdverbs).shuffled()
            unitNum <= 25 -> (intermediateNouns + intermediateVerbs + intermediateAdjectives + intermediateAdverbs).shuffled()
            else -> (advancedNouns + advancedVerbs + advancedAdjectives + advancedAdverbs).shuffled()
        }
        
        return (base + pool).distinct().take(150)
    }

    internal val allLanguageVocab: Map<String, Map<String, String>> by lazy {
        val romanceLanguages = listOf("Spanish", "French", "Italian", "Portuguese")
        nativeLanguages.associateWith { lang ->
            vocabularyList.associateWith { word ->
                if (word.contains(" ")) {
                    val parts = word.split(" ")
                    if (parts.size == 2 && lang in romanceLanguages) {
                        val first = parts[0]
                        val second = parts[1]
                        val firstIsAdj = adjectiveVocab.contains(first)
                        val secondIsNoun = baseVocab.contains(second)
                        
                        if (firstIsAdj && secondIsNoun) {
                            "${translateWord(second, lang)} ${translateWord(first, lang)}"
                        } else {
                            parts.joinToString(" ") { translateWord(it, lang) }
                        }
                    } else {
                        parts.joinToString(" ") { translateWord(it, lang) }
                    }
                } else {
                    translateWord(word, lang)
                }
            }
        }
    }

    private fun translateWord(word: String, language: String = "Spanish"): String {
        if (language == "English") return word
        val lowerWord = word.lowercase()
        return when (language) {
            "French" -> translateToFrench(lowerWord)
            "German" -> translateToGerman(lowerWord)
            "Italian" -> translateToItalian(lowerWord)
            "Portuguese" -> translateToPortuguese(lowerWord)
            "Afrikaans" -> translateToAfrikaans(lowerWord)
            "Spanish" -> translateToSpanish(lowerWord)
            else -> lowerWord
        }
    }

    private fun translateToFrench(word: String): String {
        return when (word) {
            "hello" -> "bonjour"
            "goodbye" -> "au revoir"
            "please" -> "s\u0027il vous plaît"
            "thanks" -> "merci"
            "yes" -> "oui"
            "no" -> "non"
            "i" -> "je"
            "you" -> "tu"
            "he" -> "il"
            "she" -> "elle"
            "it" -> "ce"
            "we" -> "nous"
            "they" -> "ils"
            "who" -> "qui"
            "what" -> "quoi"
            "where" -> "où"
            "when" -> "quand"
            "why" -> "pourquoi"
            "how" -> "comment"
            "name" -> "nom"
            "friend" -> "ami"
            "family" -> "famille"
            "man" -> "homme"
            "woman" -> "femme"
            "child" -> "enfant"
            "one" -> "un"
            "two" -> "deux"
            "three" -> "trois"
            "four" -> "quatre"
            "five" -> "cinq"
            "six" -> "six"
            "seven" -> "sept"
            "eight" -> "huit"
            "nine" -> "neuf"
            "ten" -> "dix"
            "day" -> "jour"
            "week" -> "semaine"
            "month" -> "mois"
            "year" -> "an"
            "house" -> "maison"
            "dog" -> "chien"
            "cat" -> "chat"
            "car" -> "voiture"
            "water" -> "eau"
            "food" -> "nourriture"
            "bread" -> "pain"
            "milk" -> "lait"
            "coffee" -> "café"
            "tea" -> "thé"
            "book" -> "livre"
            "pen" -> "stylo"
            "phone" -> "téléphone"
            "city" -> "ville"
            "school" -> "école"
            "office" -> "bureau"
            "street" -> "rue"
            "store" -> "magasin"
            "money" -> "argent"
            "price" -> "prix"
            "room" -> "chambre"
            "door" -> "porte"
            "window" -> "fenêtre"
            "bed" -> "lit"
            "kitchen" -> "cuisine"
            "apple" -> "pomme"
            "banana" -> "banane"
            "orange" -> "orange"
            "meat" -> "viande"
            "fish" -> "poisson"
            "chicken" -> "poulet"
            "salt" -> "sel"
            "sugar" -> "sucre"
            "egg" -> "œuf"
            "fruit" -> "fruit"
            "vegetable" -> "légume"
            "shirt" -> "chemise"
            "shoes" -> "chaussures"
            "pants" -> "pantalon"
            "hat" -> "chapeau"
            "bag" -> "sac"
            "clock" -> "horloge"
            "sun" -> "soleil"
            "moon" -> "lune"
            "star" -> "étoile"
            "sky" -> "ciel"
            "rain" -> "pluie"
            "snow" -> "neige"
            "fire" -> "feu"
            "good" -> "bon"
            "bad" -> "mauvais"
            "big" -> "grand"
            "small" -> "petit"
            "hot" -> "chaud"
            "cold" -> "froid"
            "happy" -> "heureux"
            "sad" -> "triste"
            "fast" -> "rapide"
            "slow" -> "lent"
            "new" -> "nouveau"
            "old" -> "vieux"
            "young" -> "jeune"
            "high" -> "haut"
            "low" -> "bas"
            "long" -> "long"
            "short" -> "court"
            "easy" -> "facile"
            "hard" -> "difficile"
            "rich" -> "riche"
            "poor" -> "pauvre"
            "clean" -> "propre"
            "dirty" -> "sale"
            "strong" -> "fort"
            "weak" -> "faible"
            "go" -> "aller"
            "come" -> "venir"
            "eat" -> "manger"
            "drink" -> "boire"
            "sleep" -> "dormir"
            "wake" -> "réveiller"
            "work" -> "travailler"
            "play" -> "jouer"
            "read" -> "lire"
            "write" -> "écrire"
            "speak" -> "parler"
            "learn" -> "apprendre"
            "study" -> "étudier"
            "buy" -> "acheter"
            "sell" -> "vendre"
            "pay" -> "payer"
            "see" -> "voir"
            "hear" -> "entendre"
            "feel" -> "sentir"
            "think" -> "penser"
            "know" -> "savoir"
            "want" -> "vouloir"
            "need" -> "avoir besoin de"
            "give" -> "donner"
            "take" -> "prendre"
            "always" -> "toujours"
            "never" -> "jamais"
            "now" -> "maintenant"
            "today" -> "aujourd\u0027hui"
            "here" -> "ici"
            "there" -> "là"
            "where is the bathroom" -> "Où sont les toilettes?"
            "how much does it cost" -> "Combien ça coûte?"
            "i am lost" -> "Je suis perdu."
            "can you help me" -> "Pouvez-vous m\u0027aider?"
            "i dont understand" -> "Je ne comprends pas."
            "please speak slowly" -> "Parlez plus lentement, s\u0027il vous plaît."
            "what is this" -> "Qu\u0027est-ce que c\u0027est?"
            "where is the hotel" -> "Où est l\u0027hôtel?"
            "i need a doctor" -> "J\u0027ai besoin d\u0027un médecin."
            "call the police" -> "Appelez la police!"
            else -> word
        }
    }


    private fun translateToGerman(word: String): String {
        return when (word) {
            "hello" -> "hallo"
            "goodbye" -> "auf wiedersehen"
            "please" -> "bitte"
            "thanks" -> "danke"
            "yes" -> "ja"
            "no" -> "nein"
            "i" -> "ich"
            "you" -> "du"
            "he" -> "er"
            "she" -> "sie"
            "it" -> "es"
            "we" -> "wir"
            "they" -> "sie"
            "who" -> "wer"
            "what" -> "was"
            "where" -> "wo"
            "when" -> "wann"
            "why" -> "warum"
            "how" -> "wie"
            "name" -> "name"
            "friend" -> "freund"
            "family" -> "familie"
            "man" -> "mann"
            "woman" -> "frau"
            "child" -> "kind"
            "one" -> "eins"
            "two" -> "zwei"
            "three" -> "drei"
            "four" -> "vier"
            "five" -> "fünf"
            "six" -> "sechs"
            "seven" -> "sieben"
            "eight" -> "acht"
            "nine" -> "neun"
            "ten" -> "zehn"
            "day" -> "tag"
            "week" -> "woche"
            "month" -> "monat"
            "year" -> "jahr"
            "house" -> "haus"
            "dog" -> "hund"
            "cat" -> "katze"
            "car" -> "auto"
            "water" -> "wasser"
            "food" -> "essen"
            "bread" -> "brot"
            "milk" -> "milch"
            "coffee" -> "kaffee"
            "tea" -> "tee"
            "book" -> "buch"
            "pen" -> "stift"
            "phone" -> "telefon"
            "city" -> "stadt"
            "school" -> "schule"
            "office" -> "büro"
            "street" -> "straße"
            "store" -> "geschäft"
            "money" -> "geld"
            "price" -> "preis"
            "room" -> "zimmer"
            "door" -> "tür"
            "window" -> "fenster"
            "bed" -> "bett"
            "kitchen" -> "küche"
            "apple" -> "apfel"
            "banana" -> "banane"
            "orange" -> "orange"
            "meat" -> "fleisch"
            "fish" -> "fisch"
            "chicken" -> "hähnchen"
            "salt" -> "salz"
            "sugar" -> "zucker"
            "egg" -> "ei"
            "fruit" -> "frucht"
            "vegetable" -> "gemüse"
            "shirt" -> "hemd"
            "shoes" -> "schuhe"
            "pants" -> "hose"
            "hat" -> "hut"
            "bag" -> "tasche"
            "clock" -> "uhr"
            "sun" -> "sonne"
            "moon" -> "mond"
            "star" -> "stern"
            "sky" -> "himmel"
            "rain" -> "regen"
            "snow" -> "schnee"
            "fire" -> "feuer"
            "good" -> "gut"
            "bad" -> "schlecht"
            "big" -> "groß"
            "small" -> "klein"
            "hot" -> "heiß"
            "cold" -> "kalt"
            "happy" -> "glücklich"
            "sad" -> "traurig"
            "fast" -> "schnell"
            "slow" -> "langsam"
            "new" -> "neu"
            "old" -> "alt"
            "young" -> "jung"
            "high" -> "hoch"
            "low" -> "niedrig"
            "long" -> "lang"
            "short" -> "kurz"
            "easy" -> "einfach"
            "hard" -> "schwer"
            "rich" -> "reich"
            "poor" -> "arm"
            "clean" -> "sauber"
            "dirty" -> "schmutzig"
            "strong" -> "stark"
            "weak" -> "schwach"
            "go" -> "gehen"
            "come" -> "kommen"
            "eat" -> "essen"
            "drink" -> "trinken"
            "sleep" -> "schlafen"
            "wake" -> "aufwachen"
            "work" -> "arbeiten"
            "play" -> "spielen"
            "read" -> "lesen"
            "write" -> "schreiben"
            "speak" -> "sprechen"
            "learn" -> "lernen"
            "study" -> "studieren"
            "buy" -> "kaufen"
            "sell" -> "verkaufen"
            "pay" -> "bezahlen"
            "see" -> "sehen"
            "hear" -> "hören"
            "feel" -> "fühlen"
            "think" -> "denken"
            "know" -> "wissen"
            "want" -> "wollen"
            "need" -> "brauchen"
            "give" -> "geben"
            "take" -> "nehmen"
            "always" -> "immer"
            "never" -> "nie"
            "now" -> "jetzt"
            "today" -> "heute"
            "here" -> "hier"
            "there" -> "dort"
            "where is the bathroom" -> "Wo ist die Toilette?"
            "how much does it cost" -> "Wie viel kostet das?"
            "i am lost" -> "Ich habe mich verlaufen."
            "can you help me" -> "Können Sie mir helfen?"
            "i dont understand" -> "Ich verstehe das nicht."
            "please speak slowly" -> "Bitte sprechen Sie langsam."
            "what is this" -> "Was ist das?"
            "where is the hotel" -> "Wo ist das Hotel?"
            "i need a doctor" -> "Ich brauche einen Arzt."
            "call the police" -> "Rufen Sie die Polizei!"
            else -> word
        }
    }

    private fun translateToItalian(word: String): String {
        return when (word) {
            "hello" -> "ciao"
            "goodbye" -> "arrivederci"
            "please" -> "per favore"
            "thanks" -> "grazie"
            "yes" -> "sì"
            "no" -> "no"
            "i" -> "io"
            "you" -> "tu"
            "he" -> "lui"
            "she" -> "lei"
            "it" -> "esso"
            "we" -> "noi"
            "they" -> "loro"
            "who" -> "chi"
            "what" -> "cosa"
            "where" -> "dove"
            "when" -> "quando"
            "why" -> "perché"
            "how" -> "come"
            "name" -> "nome"
            "friend" -> "amico"
            "family" -> "famiglia"
            "man" -> "uomo"
            "woman" -> "donna"
            "child" -> "bambino"
            "one" -> "uno"
            "two" -> "due"
            "three" -> "tre"
            "four" -> "quattro"
            "five" -> "cinque"
            "six" -> "sei"
            "seven" -> "sette"
            "eight" -> "otto"
            "nine" -> "nove"
            "ten" -> "dieci"
            "day" -> "giorno"
            "week" -> "settimana"
            "month" -> "mese"
            "year" -> "anno"
            "house" -> "casa"
            "dog" -> "cane"
            "cat" -> "gatto"
            "car" -> "auto"
            "water" -> "acqua"
            "food" -> "cibo"
            "bread" -> "pane"
            "milk" -> "latte"
            "coffee" -> "caffè"
            "tea" -> "tè"
            "book" -> "libro"
            "pen" -> "penna"
            "phone" -> "telefono"
            "city" -> "città"
            "school" -> "scuola"
            "office" -> "ufficio"
            "street" -> "strada"
            "store" -> "negozio"
            "money" -> "soldi"
            "price" -> "prezzo"
            "room" -> "stanza"
            "door" -> "porta"
            "window" -> "finestra"
            "bed" -> "letto"
            "kitchen" -> "cucina"
            "apple" -> "mela"
            "banana" -> "banana"
            "orange" -> "arancia"
            "meat" -> "carne"
            "fish" -> "pesce"
            "chicken" -> "pollo"
            "salt" -> "sale"
            "sugar" -> "zucchero"
            "egg" -> "uovo"
            "fruit" -> "frutta"
            "vegetura" -> "verdura"
            "shirt" -> "camicia"
            "shoes" -> "scarpe"
            "pants" -> "pantaloni"
            "hat" -> "cappello"
            "bag" -> "borsa"
            "clock" -> "orologio"
            "sun" -> "sole"
            "moon" -> "luna"
            "star" -> "stella"
            "sky" -> "cielo"
            "rain" -> "pioggia"
            "snow" -> "neve"
            "fire" -> "fuoco"
            "good" -> "buono"
            "bad" -> "cattivo"
            "big" -> "grande"
            "small" -> "piccolo"
            "hot" -> "caldo"
            "cold" -> "freddo"
            "happy" -> "felice"
            "sad" -> "triste"
            "fast" -> "veloce"
            "slow" -> "lento"
            "new" -> "nuovo"
            "old" -> "vecchio"
            "young" -> "giovane"
            "high" -> "alto"
            "low" -> "basso"
            "long" -> "lungo"
            "short" -> "corto"
            "easy" -> "facile"
            "hard" -> "difficile"
            "rich" -> "ricco"
            "poor" -> "povero"
            "clean" -> "pulito"
            "dirty" -> "sporco"
            "strong" -> "forte"
            "weak" -> "debole"
            "go" -> "andare"
            "come" -> "venire"
            "eat" -> "mangiare"
            "drink" -> "bere"
            "sleep" -> "dormir"
            "wake" -> "svegliarsi"
            "work" -> "lavorare"
            "play" -> "giocare"
            "read" -> "leggere"
            "write" -> "scrivere"
            "speak" -> "parlare"
            "learn" -> "imparare"
            "study" -> "studiare"
            "buy" -> "comprare"
            "sell" -> "vendere"
            "pay" -> "pagare"
            "see" -> "vedere"
            "hear" -> "sentire"
            "feel" -> "sentire"
            "think" -> "pensare"
            "know" -> "sapere"
            "want" -> "volere"
            "need" -> "avere bisogno"
            "give" -> "dare"
            "take" -> "prendere"
            "always" -> "sempre"
            "never" -> "mai"
            "now" -> "ora"
            "today" -> "oggi"
            "here" -> "qui"
            "there" -> "lì"
            "where is the bathroom" -> "Dov\u0027è il bagno?"
            "how much does it cost" -> "Quanto costa?"
            "i am lost" -> "Mi sono perso."
            "can you help me" -> "Può aiutarmi?"
            "i dont understand" -> "Non capisco."
            "please speak slowly" -> "Per favore, parli lentamente."
            "what is this" -> "Cos\u0027è questo?"
            "where is the hotel" -> "Dov\u0027è l\u0027hotel?"
            "i need a doctor" -> "Ho bisogno di un medico."
            "call the police" -> "Chiami la polizia!"
            else -> word
        }
    }


    private fun translateToPortuguese(word: String): String {
        return when (word) {
            "hello" -> "olá"
            "goodbye" -> "adeus"
            "please" -> "por favor"
            "thanks" -> "obrigado"
            "yes" -> "sim"
            "no" -> "não"
            "i" -> "eu"
            "you" -> "você"
            "he" -> "ele"
            "she" -> "ela"
            "it" -> "isso"
            "we" -> "nós"
            "they" -> "eles"
            "who" -> "quem"
            "what" -> "o que"
            "where" -> "onde"
            "when" -> "quando"
            "why" -> "por que"
            "how" -> "como"
            "name" -> "nome"
            "friend" -> "amigo"
            "family" -> "família"
            "man" -> "homem"
            "woman" -> "mulher"
            "child" -> "criança"
            "one" -> "um"
            "two" -> "dois"
            "three" -> "três"
            "four" -> "quatro"
            "five" -> "cinco"
            "six" -> "seis"
            "seven" -> "sete"
            "eight" -> "oito"
            "nine" -> "nove"
            "ten" -> "dez"
            "day" -> "dia"
            "week" -> "semana"
            "month" -> "mês"
            "year" -> "ano"
            "house" -> "casa"
            "dog" -> "cachorro"
            "cat" -> "gato"
            "car" -> "carro"
            "water" -> "água"
            "food" -> "comida"
            "bread" -> "pão"
            "milk" -> "leite"
            "coffee" -> "café"
            "tea" -> "chá"
            "book" -> "livro"
            "pen" -> "caneta"
            "phone" -> "telefone"
            "city" -> "cidade"
            "school" -> "escola"
            "office" -> "escritório"
            "street" -> "rua"
            "store" -> "loja"
            "money" -> "dinheiro"
            "price" -> "preço"
            "room" -> "quarto"
            "door" -> "porta"
            "window" -> "janela"
            "bed" -> "cama"
            "kitchen" -> "cozinha"
            "apple" -> "maçã"
            "banana" -> "banana"
            "orange" -> "laranja"
            "meat" -> "carne"
            "fish" -> "peixe"
            "chicken" -> "frango"
            "salt" -> "sal"
            "sugar" -> "açúcar"
            "egg" -> "ovo"
            "fruit" -> "fruta"
            "vegetable" -> "vegetal"
            "shirt" -> "camisa"
            "shoes" -> "sapatos"
            "pants" -> "calças"
            "hat" -> "chapéu"
            "bag" -> "bolsa"
            "clock" -> "relógio"
            "sun" -> "sol"
            "moon" -> "lua"
            "star" -> "estrela"
            "sky" -> "céu"
            "rain" -> "chuva"
            "snow" -> "neve"
            "fire" -> "fogo"
            "good" -> "bom"
            "bad" -> "mau"
            "big" -> "grande"
            "small" -> "pequeno"
            "hot" -> "quente"
            "cold" -> "frio"
            "happy" -> "feliz"
            "sad" -> "triste"
            "fast" -> "rápido"
            "slow" -> "lento"
            "new" -> "novo"
            "old" -> "velho"
            "young" -> "jovem"
            "high" -> "alto"
            "low" -> "baixo"
            "long" -> "longo"
            "short" -> "curto"
            "easy" -> "fácil"
            "hard" -> "difícil"
            "rich" -> "rico"
            "poor" -> "pobre"
            "clean" -> "limpo"
            "dirty" -> "sujo"
            "strong" -> "forte"
            "weak" -> "fraco"
            "go" -> "ir"
            "come" -> "vir"
            "eat" -> "comer"
            "drink" -> "beber"
            "sleep" -> "dormir"
            "wake" -> "acordar"
            "work" -> "trabalhar"
            "play" -> "jogar"
            "read" -> "ler"
            "write" -> "escrever"
            "speak" -> "falar"
            "learn" -> "aprender"
            "study" -> "estudar"
            "buy" -> "comprar"
            "sell" -> "vender"
            "pay" -> "pagar"
            "see" -> "ver"
            "hear" -> "ouvir"
            "feel" -> "sentir"
            "think" -> "pensar"
            "know" -> "saber"
            "want" -> "querer"
            "need" -> "precisar"
            "give" -> "dar"
            "take" -> "pegar"
            "always" -> "sempre"
            "never" -> "nunca"
            "now" -> "agora"
            "today" -> "hoje"
            "here" -> "aqui"
            "there" -> "lá"
            "where is the bathroom" -> "Onde é o banheiro?"
            "how much does it cost" -> "Quanto custa?"
            "i am lost" -> "Estou perdido."
            "can you help me" -> "Pode me ajudar?"
            "i dont understand" -> "Não entendo."
            "please speak slowly" -> "Por favor, fale devagar."
            "what is this" -> "O que é isto?"
            "where is the hotel" -> "Onde é o hotel?"
            "i need a doctor" -> "Preciso de um médico."
            "call the police" -> "Chame a polícia!"
            else -> word
        }
    }


    private fun translateToAfrikaans(word: String): String {
        return when (word) {
            "hello" -> "hallo"
            "goodbye" -> "totsiens"
            "please" -> "asseblief"
            "thanks" -> "dankie"
            "yes" -> "ja"
            "no" -> "nee"
            "i" -> "ek"
            "you" -> "jy"
            "he" -> "hy"
            "she" -> "sy"
            "it" -> "dit"
            "we" -> "ons"
            "they" -> "hulle"
            "who" -> "wie"
            "what" -> "wat"
            "where" -> "waar"
            "when" -> "wanneer"
            "why" -> "hoekom"
            "how" -> "hoe"
            "name" -> "naam"
            "friend" -> "vriend"
            "family" -> "familie"
            "mother" -> "ma"
            "father" -> "pa"
            "son" -> "seun"
            "daughter" -> "dogter"
            "brother" -> "broer"
            "sister" -> "suster"
            "parent" -> "ouer"
            "kid" -> "kind"
            "wife" -> "vrou"
            "man" -> "man"
            "woman" -> "vrou"
            "child" -> "kind"
            "one" -> "een"
            "two" -> "twee"
            "three" -> "drie"
            "four" -> "vier"
            "five" -> "vyf"
            "six" -> "ses"
            "seven" -> "sewe"
            "eight" -> "agt"
            "nine" -> "nege"
            "ten" -> "tien"
            "day" -> "dag"
            "week" -> "week"
            "month" -> "maand"
            "year" -> "jaar"
            "time" -> "tyd"
            "hour" -> "uur"
            "minute" -> "minuut"
            "morning" -> "oggend"
            "night" -> "nag"
            "afternoon" -> "middag"
            "evening" -> "aand"
            "tomorrow" -> "môre"
            "yesterday" -> "gister"
            "house" -> "huis"
            "dog" -> "hond"
            "cat" -> "kat"
            "car" -> "motor"
            "water" -> "water"
            "food" -> "kos"
            "bread" -> "brood"
            "milk" -> "melk"
            "coffee" -> "koffie"
            "tea" -> "tee"
            "book" -> "boek"
            "pen" -> "pen"
            "phone" -> "telefoon"
            "city" -> "stad"
            "school" -> "skool"
            "office" -> "kantoor"
            "street" -> "straat"
            "store" -> "winkel"
            "money" -> "geld"
            "price" -> "prys"
            "room" -> "kamer"
            "door" -> "deur"
            "window" -> "venster"
            "bed" -> "bed"
            "kitchen" -> "kombuis"
            "apple" -> "appel"
            "banana" -> "piesang"
            "orange" -> "lemoen"
            "meat" -> "vleis"
            "fish" -> "vis"
            "chicken" -> "hoender"
            "salt" -> "sout"
            "sugar" -> "suiker"
            "egg" -> "eier"
            "fruit" -> "vrugte"
            "vegetable" -> "groente"
            "shirt" -> "hemp"
            "shoes" -> "skoene"
            "pants" -> "broek"
            "hat" -> "hoed"
            "bag" -> "sak"
            "clock" -> "horlosie"
            "sun" -> "son"
            "moon" -> "maan"
            "star" -> "ster"
            "sky" -> "lug"
            "rain" -> "reën"
            "snow" -> "sneeu"
            "fire" -> "vuur"
            "body" -> "liggaam"
            "head" -> "kop"
            "face" -> "gesig"
            "eye" -> "oog"
            "foot" -> "voet"
            "arm" -> "arm"
            "heart" -> "hart"
            "sick" -> "siek"
            "touch" -> "aanraak"
            "cloud" -> "wolk"
            "wind" -> "wind"
            "storm" -> "storm"
            "weather" -> "weer"
            "winter" -> "winter"
            "summer" -> "somer"
            "spring" -> "lente"
            "autumn" -> "herfs"
            "ice" -> "ys"
            "company" -> "maatskappy"
            "project" -> "projek"
            "meeting" -> "vergadering"
            "career" -> "loopbaan"
            "success" -> "sukses"
            "problem" -> "probleem"
            "solution" -> "oplossing"
            "result" -> "resultaat"
            "fact" -> "feit"
            "idea" -> "idee"
            "culture" -> "kultuur"
            "history" -> "geskiedenis"
            "art" -> "kuns"
            "music" -> "musiek"
            "movie" -> "fliek"
            "sport" -> "sport"
            "game" -> "spel"
            "nature" -> "natuur"
            "tree" -> "boom"
            "flower" -> "blom"
            "bird" -> "voël"
            "forest" -> "bos"
            "health" -> "gesondheid"
            "doctor" -> "dokter"
            "hospital" -> "hospitaal"
            "medicine" -> "medisyne"
            "pain" -> "pyn"
            "travel" -> "reis"
            "trip" -> "reis"
            "plane" -> "vliegtuig"
            "train" -> "trein"
            "hotel" -> "hotel"
            "beach" -> "strand"
            "passport" -> "paspoort"
            "ticket" -> "kaartjie"
            "economy" -> "ekonomie"
            "growth" -> "groei"
            "law" -> "wet"
            "rule" -> "reël"
            "government" -> "regering"
            "vote" -> "stem"
            "leader" -> "leier"
            "society" -> "samelewing"
            "keyboard" -> "sleutelbord"
            "mouse" -> "muis"
            "screen" -> "skerm"
            "internet" -> "internet"
            "website" -> "webwerf"
            "app" -> "toepassing"
            "message" -> "boodskap"
            "video" -> "video"
            "photo" -> "foto"
            "camera" -> "kamera"
            "concept" -> "begrip"
            "thought" -> "gedagte"
            "mind" -> "verstand"
            "soul" -> "siel"
            "power" -> "krag"
            "freedom" -> "vryheid"
            "justice" -> "geregtigheid"
            "truth" -> "waarheid"
            "beauty" -> "skoonheid"
            "honor" -> "eer"
            "research" -> "navorsing"
            "evidence" -> "bewyse"
            "theory" -> "teorie"
            "discovery" -> "ontdekking"
            "environment" -> "omgewing"
            "pollution" -> "besoedeling"
            "climate" -> "klimaat"
            "energy" -> "energie"
            "resource" -> "hulpbron"
            "relationship" -> "verhouding"
            "conflict" -> "konflik"
            "agreement" -> "ooreenkoms"
            "debate" -> "debat"
            "opinion" -> "mening"
            "reason" -> "rede"
            "purpose" -> "doel"
            "memory" -> "herinnering"
            "experience" -> "ervaring"
            "childhood" -> "kinderjare"
            "philosophy" -> "filosofie"
            "psychology" -> "sielkunde"
            "strategy" -> "strategie"
            "innovation" -> "innovasie"
            "structure" -> "struktuur"
            "perspective" -> "perspektief"
            "challenge" -> "uitdaging"
            "opportunity" -> "geleentheid"
            "threat" -> "bedreiging"
            "benefit" -> "voordeel"
            "consequence" -> "gevolg"
            "influence" -> "invloed"
            "impact" -> "impak"
            "reaction" -> "reaksie"
            "emotion" -> "emosie"
            "passion" -> "passie"
            "good" -> "goed"
            "bad" -> "sleg"
            "big" -> "groot"
            "small" -> "klein"
            "hot" -> "warm"
            "cold" -> "koud"
            "happy" -> "gelukkig"
            "sad" -> "hartseer"
            "fast" -> "vinnig"
            "slow" -> "stadig"
            "new" -> "nuut"
            "old" -> "oud"
            "young" -> "jonk"
            "high" -> "hoog"
            "low" -> "laag"
            "long" -> "lank"
            "short" -> "kort"
            "easy" -> "maklik"
            "hard" -> "moeilik"
            "rich" -> "ryk"
            "poor" -> "arm"
            "clean" -> "skoon"
            "dirty" -> "vuil"
            "strong" -> "sterk"
            "weak" -> "swak"
            "go" -> "gaan"
            "come" -> "kom"
            "eat" -> "eet"
            "drink" -> "drink"
            "sleep" -> "slaap"
            "wake" -> "wakker word"
            "work" -> "werk"
            "play" -> "speel"
            "read" -> "lees"
            "write" -> "skryf"
            "speak" -> "praat"
            "learn" -> "leer"
            "study" -> "studeer"
            "buy" -> "koop"
            "sell" -> "verkoop"
            "pay" -> "betaal"
            "see" -> "sien"
            "hear" -> "hoor"
            "feel" -> "voel"
            "think" -> "dink"
            "know" -> "weet"
            "want" -> "wil"
            "need" -> "nodig hê"
            "discuss" -> "bespreek"
            "analyze" -> "ontleed"
            "evaluate" -> "evalueer"
            "discover" -> "ontdek"
            "invent" -> "uitvind"
            "improve" -> "verbeter"
            "increase" -> "verhoog"
            "decrease" -> "verminder"
            "provide" -> "voorsien"
            "require" -> "vereis"
            "develop" -> "ontwikkel"
            "manage" -> "bestuur"
            "organize" -> "organiseer"
            "negotiate" -> "onderhandel"
            "persuade" -> "oorreed"
            "influence" -> "beïnvloed"
            "affect" -> "beïnvloed"
            "challenge" -> "uitdaag"
            "succeed" -> "slaag"
            "prepare" -> "voorberei"
            "decide" -> "besluit"
            "achieve" -> "bereik"
            "maintain" -> "handhaaf"
            "resolve" -> "oplos"
            "give" -> "gee"
            "take" -> "neem"
            "always" -> "altyd"
            "never" -> "nooit"
            "now" -> "nou"
            "today" -> "vandag"
            "here" -> "hier"
            "there" -> "daar"
            "where is the bathroom" -> "Waar is die badkamer?"
            "how much does it cost" -> "Hoeveel kos dit?"
            "i am lost" -> "Ek is verlore."
            "can you help me" -> "Kan u my help?"
            "i dont understand" -> "Ek verstaan nie."
            "please speak slowly" -> "Praat asseblief stadig."
            "what is this" -> "Wat is dit?"
            "where is the hotel" -> "Waar is die hotel?"
            "i need a doctor" -> "Ek het \u0027n dokter nodig."
            "call the police" -> "Bel die polisie!"
            else -> word
        }
    }


    private fun translateToSpanish(word: String): String {
        return when (word) {
            "hello" -> "hola"
            "goodbye" -> "adiós"
            "please" -> "por favor"
            "thanks" -> "gracias"
            "yes" -> "sí"
            "no" -> "no"
            "i" -> "yo"
            "you" -> "tú"
            "he" -> "él"
            "she" -> "ella"
            "it" -> "eso"
            "we" -> "nosotros"
            "they" -> "ellos"
            "who" -> "quién"
            "what" -> "qué"
            "where" -> "dónde"
            "when" -> "cuándo"
            "why" -> "por qué"
            "how" -> "cómo"
            "name" -> "nombre"
            "friend" -> "amigo"
            "family" -> "familia"
            "man" -> "hombre"
            "woman" -> "mujer"
            "child" -> "niño"
            "one" -> "uno"
            "two" -> "dos"
            "three" -> "tres"
            "four" -> "cuatro"
            "five" -> "cinco"
            "six" -> "seis"
            "seven" -> "siete"
            "eight" -> "ocho"
            "nine" -> "nueve"
            "ten" -> "diez"
            "day" -> "día"
            "week" -> "semana"
            "month" -> "mes"
            "year" -> "año"
            "house" -> "casa"
            "dog" -> "perro"
            "cat" -> "gato"
            "car" -> "coche"
            "water" -> "agua"
            "food" -> "comida"
            "bread" -> "pan"
            "milk" -> "leche"
            "coffee" -> "café"
            "tea" -> "té"
            "book" -> "libro"
            "pen" -> "bolígrafo"
            "phone" -> "teléfono"
            "city" -> "ciudad"
            "school" -> "escuela"
            "office" -> "oficina"
            "street" -> "calle"
            "store" -> "tienda"
            "money" -> "dinero"
            "price" -> "precio"
            "room" -> "habitación"
            "door" -> "puerta"
            "window" -> "ventana"
            "bed" -> "cama"
            "kitchen" -> "cocina"
            "apple" -> "manzana"
            "banana" -> "plátano"
            "orange" -> "naranja"
            "meat" -> "carne"
            "fish" -> "pescado"
            "chicken" -> "pollo"
            "salt" -> "sal"
            "sugar" -> "azúcar"
            "egg" -> "huevo"
            "fruit" -> "fruta"
            "vegetable" -> "verdura"
            "shirt" -> "camisa"
            "shoes" -> "zapatos"
            "pants" -> "pantalones"
            "hat" -> "sombrero"
            "bag" -> "bolsa"
            "clock" -> "reloj"
            "sun" -> "sol"
            "moon" -> "luna"
            "star" -> "estrella"
            "sky" -> "cielo"
            "rain" -> "lluvia"
            "snow" -> "nieve"
            "fire" -> "fuego"
            "good" -> "bueno"
            "bad" -> "malo"
            "big" -> "grande"
            "small" -> "pequeño"
            "hot" -> "caliente"
            "cold" -> "frío"
            "happy" -> "feliz"
            "sad" -> "triste"
            "fast" -> "rápido"
            "slow" -> "lento"
            "new" -> "nuevo"
            "old" -> "viejo"
            "young" -> "joven"
            "high" -> "alto"
            "low" -> "bajo"
            "long" -> "largo"
            "short" -> "corto"
            "easy" -> "fácil"
            "hard" -> "difícil"
            "rich" -> "rico"
            "poor" -> "pobre"
            "clean" -> "limpio"
            "dirty" -> "sucio"
            "strong" -> "fuerte"
            "weak" -> "débil"
            "go" -> "ir"
            "come" -> "venir"
            "eat" -> "comer"
            "drink" -> "beber"
            "sleep" -> "dormir"
            "wake" -> "despertar"
            "work" -> "trabajar"
            "play" -> "jugar"
            "read" -> "leer"
            "write" -> "escribir"
            "speak" -> "hablar"
            "learn" -> "aprender"
            "study" -> "estudiar"
            "buy" -> "comprar"
            "sell" -> "vender"
            "pay" -> "pagar"
            "see" -> "ver"
            "hear" -> "oír"
            "feel" -> "sentir"
            "think" -> "pensar"
            "know" -> "saber"
            "want" -> "querer"
            "need" -> "necesitar"
            "give" -> "dar"
            "take" -> "tomar"
            "always" -> "siempre"
            "never" -> "nunca"
            "now" -> "ahora"
            "today" -> "hoy"
            "here" -> "aquí"
            "there" -> "allí"
            "where is the bathroom" -> "¿Dónde está el baño?"
            "how much does it cost" -> "¿Cuánto cuesta?"
            "i am lost" -> "Estoy perdido."
            "can you help me" -> "¿Puede ayudarme?"
            "i dont understand" -> "No entiendo."
            "please speak slowly" -> "Por favor, hable despacio."
            "what is this" -> "¿Qué es esto?"
            "where is the hotel" -> "¿Dónde está el hotel?"
            "i need a doctor" -> "Necesito un médico."
            "call the police" -> "¡Llame a la policía!"
            else -> word
        }
    }


    private val keyChunks = vocabularyList.chunked(30)

    private fun generateLessonContent(
        targetLang: String,
        pairs: List<Pair<String, String>>,
        unitNum: Int
    ): List<LessonContent> {
        val allTargetWords = pairs.map { it.second }
        return pairs.flatMap { (eng, targetWord) ->
            val fullTranslations = mapOf(
                "English" to eng,
                targetLang to targetWord,
                "Spanish" to translateToSpanish(eng),
                "French" to translateToFrench(eng),
                "German" to translateToGerman(eng),
                "Italian" to translateToItalian(eng),
                "Portuguese" to translateToPortuguese(eng),
                "Afrikaans" to translateToAfrikaans(eng)
            )
            
            val teachCard = LessonContent(
                type = ContentType.TEACH,
                translations = fullTranslations,
                options = emptyList(),
                correctAnswerIndex = 0,
                explanation = "In $targetLang, '$eng' is '$targetWord'.",
                audioText = targetWord,
                phonetic = generatePhonetic(targetWord, targetLang),
                partOfSpeech = getPartOfSpeech(eng)
            )

            val (exEng, exTarget) = getRealisticExample(eng, targetLang)
            val exampleCard = LessonContent(
                type = ContentType.EXAMPLE,
                translations = mapOf(
                    "English" to exEng,
                    targetLang to exTarget
                ),
                options = emptyList(),
                correctAnswerIndex = 0,
                explanation = "See how '$targetWord' is used in a sentence.",
                audioText = exTarget,
                phonetic = "",
                partOfSpeech = teachCard.partOfSpeech
            )

            val otherWords = allTargetWords.filter { it != targetWord }.shuffled()
            val options = (listOf(targetWord) + otherWords.take(3)).shuffled()
            
            val questionCard = LessonContent(
                type = ContentType.QUESTION,
                translations = fullTranslations,
                options = options,
                correctAnswerIndex = options.indexOf(targetWord),
                explanation = "The correct answer for '$eng' is '$targetWord'.",
                audioText = targetWord,
                phonetic = generatePhonetic(targetWord, targetLang),
                partOfSpeech = teachCard.partOfSpeech
            )

            listOf(teachCard, exampleCard, questionCard)
        }
    }

    private fun getRealisticExample(eng: String, targetLang: String): Pair<String, String> {
        if (targetLang == "Afrikaans") {
            return getAfrikaansExample(eng)
        }

        val englishExample = getEnglishExample(eng)
        return englishExample to translateSentence(englishExample, targetLang)
    }

    private fun getEnglishExample(eng: String): String {
        val word = eng.lowercase()
        return when (word) {
            "hello" -> "Hello, how are you today?"
            "goodbye" -> "I will see you tomorrow. Goodbye!"
            "please" -> "Please open the door."
            "thanks" -> "Thanks for your help."
            "yes" -> "Yes, I understand."
            "no" -> "No, I do not know."
            "house" -> "I am going home after work."
            "dog" -> "The dog is playing in the park."
            "eat" -> "I like to eat fresh fruit."
            "water" -> "Could I have a glass of water, please?"
            "bread" -> "The bread is fresh this morning."
            "city" -> "The city is busy during the day."
            "friend" -> "My friend is waiting outside."
            "family" -> "My family lives nearby."
            "where is the bathroom" -> "Where is the bathroom?"
            "i am lost" -> "I am lost. Can you help me?"
            "can you help me" -> "Can you help me find the station?"
            "i dont understand" -> "I do not understand the question."
            "please speak slowly" -> "Please speak slowly."
            "what is this" -> "What is this called?"
            "where is the hotel" -> "Where is the hotel?"
            "i need a doctor" -> "I need a doctor."
            "call the police" -> "Please call the police."
            "how much does it cost" -> "How much does it cost?"
            else -> buildEnglishExample(word)
        }
    }

    private fun buildEnglishExample(word: String): String {
        val words = word.split(" ")
        if (words.size == 2 && adjectiveVocab.contains(words[0]) && baseVocab.contains(words[1])) {
            val adjective = words[0]
            val noun = words[1]
            return when (noun) {
                "banana" -> "This banana has a funny shape, but it tastes good."
                "meat" -> "The meat has a funny smell, so I will not eat it."
                "food" -> "The $adjective food was a hit at the family dinner."
                "dog" -> "The $adjective dog greeted every visitor at the park."
                "car" -> "The $adjective car attracted attention on the street."
                "movie" -> "The $adjective movie kept the audience entertained."
                "music" -> "The $adjective music filled the room."
                "idea" -> "Her $adjective idea solved the problem."
                "project" -> "The $adjective project was completed on time."
                "weather" -> "The $adjective weather changed our travel plans."
                else -> {
                    val templates = listOf(
                        "I noticed a $adjective $noun at the market.",
                        "Everyone talked about the $adjective $noun.",
                        "The $adjective $noun caught my attention.",
                        "We found a $adjective $noun near the entrance."
                    )
                    templates[Math.floorMod(word.hashCode(), templates.size)]
                }
            }
        }

        return when {
            verbVocab.contains(word) -> {
                val templates = listOf(
                    "I $word before breakfast.",
                    "We $word together after work.",
                    "They $word whenever they have time.",
                    "She plans to $word this weekend."
                )
                templates[Math.floorMod(word.hashCode(), templates.size)]
            }
            adjectiveVocab.contains(word) -> {
                val templates = listOf(
                    "The new design looks $word.",
                    "This jacket feels $word.",
                    "The teacher said the answer was $word.",
                    "That sounds like a $word plan."
                )
                templates[Math.floorMod(word.hashCode(), templates.size)]
            }
            adverbVocab.contains(word) -> when (word) {
                "always" -> "I always arrive on time."
                "never" -> "I never skip breakfast."
                "now" -> "I am busy now."
                "today" -> "I am working today."
                "here" -> "Please wait here."
                "there" -> "The station is over there."
                "very" -> "The coffee is very hot."
                "too" -> "The bag is too heavy."
                else -> "She speaks $word."
            }
            baseVocab.contains(word) -> {
                val templates = listOf(
                    "I keep the $word in the kitchen.",
                    "We talked about the $word on the way home.",
                    "The $word was waiting near the door.",
                    "She brought the $word to work."
                )
                templates[Math.floorMod(word.hashCode(), templates.size)]
            }
            else -> "I use the word \"$word\" in a sentence."
        }
    }

    private fun getAfrikaansExample(eng: String): Pair<String, String> {
        val word = eng.lowercase()
        return when {
            word == "hello" -> "Hello, how are you today?" to "Hallo, hoe gaan dit vandag?"
            word == "goodbye" -> "I will see you tomorrow. Goodbye!" to "Ek sien jou môre. Totsiens!"
            word == "please" -> "Please open the door." to "Maak asseblief die deur oop."
            word == "thanks" -> "Thanks for your help." to "Dankie vir jou hulp."
            word == "yes" -> "Yes, I understand." to "Ja, ek verstaan."
            word == "no" -> "No, I do not know." to "Nee, ek weet nie."
            word == "house" -> "I am going home after work." to "Ek gaan huis toe ná werk."
            word == "dog" -> "The dog is playing in the park." to "Die hond speel in die park."
            word == "eat" -> "I like to eat fresh fruit." to "Ek hou daarvan om vars vrugte te eet."
            word == "water" -> "Could I have a glass of water, please?" to "Kan ek asseblief 'n glas water kry?"
            word == "bread" -> "The bread is fresh this morning." to "Die brood is vars vanoggend."
            word == "city" -> "The city is busy during the day." to "Die stad is bedrywig gedurende die dag."
            word == "friend" -> "My friend is waiting outside." to "My vriend wag buite."
            word == "family" -> "My family lives nearby." to "My familie woon naby."
            word == "where is the bathroom" -> "Where is the bathroom?" to "Waar is die badkamer?"
            word == "i am lost" -> "I am lost. Can you help me?" to "Ek is verdwaal. Kan jy my help?"
            word == "can you help me" -> "Can you help me find the station?" to "Kan jy my help om die stasie te vind?"
            word == "i dont understand" -> "I do not understand the question." to "Ek verstaan nie die vraag nie."
            word == "please speak slowly" -> "Please speak slowly." to "Praat asseblief stadig."
            word == "what is this" -> "What is this called?" to "Wat noem 'n mens dit?"
            word == "where is the hotel" -> "Where is the hotel?" to "Waar is die hotel?"
            word == "i need a doctor" -> "I need a doctor." to "Ek het 'n dokter nodig."
            word == "call the police" -> "Please call the police." to "Bel asseblief die polisie."
            word == "how much does it cost" -> "How much does it cost?" to "Hoeveel kos dit?"
            word.split(" ").size == 2 && adjectiveVocab.contains(word.split(" ")[0]) && baseVocab.contains(word.split(" ")[1]) -> {
                val adjective = word.split(" ")[0]
                val noun = word.split(" ")[1]
                when (word) {
                    "funny banana" -> "This banana has a funny shape, but it tastes good." to "Hierdie piesang het 'n snaakse vorm, maar dit smaak lekker."
                    "funny meat" -> "The meat has a funny smell, so I will not eat it." to "Die vleis het 'n snaakse reuk, so ek sal dit nie eet nie."
                    else -> "The $noun is $adjective." to "Die ${translateAfrikaansExampleWord(noun)} is ${translateAfrikaansExampleWord(adjective)}."
                }
            }
            verbVocab.contains(word) -> {
                "I like to $word." to "Ek hou daarvan om ${translateAfrikaansExampleWord(word)}."
            }
            adjectiveVocab.contains(word) -> {
                "The room is $word." to "Die kamer is ${translateAfrikaansExampleWord(word)}."
            }
            adverbVocab.contains(word) -> {
                "She speaks $word." to "Sy praat ${translateAfrikaansExampleWord(word)}."
            }
            else -> {
                "I see the $word." to "Ek sien die ${translateAfrikaansExampleWord(word)}."
            }
        }
    }

    private fun translateAfrikaansExampleWord(word: String): String {
        val directTranslation = translateToAfrikaans(word)
        return if (directTranslation != word) {
            directTranslation
        } else {
            word.split(" ").joinToString(" ") { translateToAfrikaans(it) }
        }
    }

    private fun translateSentence(sentence: String, targetLang: String): String {
        return when (targetLang) {
            "Spanish" -> when (sentence) {
                "Hello, how are you today?" -> "Hola, ¿cómo estás hoy?"
                "It is time to say goodbye." -> "Es hora de decir adiós."
                "I am going to my house." -> "Voy a mi casa."
                "The dog is playing in the park." -> "El perro está jugando en el parque."
                "I like to eat apples." -> "Me gusta comer manzanas."
                "Can I have some water, please?" -> "¿Puedo tomar un poco de agua, por favor?"
                "The bread is fresh and warm." -> "El pan está fresco y caliente."
                "The city is very busy at night." -> "La ciudad está muy concurrida por la noche."
                "You are a very good friend." -> "Eres un muy buen amigo."
                "I love spending time with my family." -> "Me encanta pasar tiempo con mi familia."
                else -> {
                    val word = sentence.removePrefix("I want to see the ").removeSuffix(".")
                    val translatedWord = translateToSpanish(word)
                    "Quiero ver el $translatedWord."
                }
            }
            "French" -> when (sentence) {
                "Hello, how are you today?" -> "Bonjour, comment ça va aujourd'hui ?"
                "It is time to say goodbye." -> "Il est temps de dire au revoir."
                "I am going to my house." -> "Je vais à ma maison."
                "The dog is playing in the park." -> "Le chien joue dans le parc."
                "I like to eat apples." -> "J'aime manger des pommes."
                "Can I have some water, please?" -> "Puis-je avoir de l'eau, s'il vous plaît ?"
                "The bread is fresh and warm." -> "Le pain est frais et chaud."
                "The city is very busy at night." -> "La ville est très animée la nuit."
                "You are a very good friend." -> "Tu es un très bon ami."
                "I love spending time with my family." -> "J'aime passer du temps avec ma famille."
                else -> {
                    val word = sentence.removePrefix("I want to see the ").removeSuffix(".")
                    val translatedWord = translateToFrench(word)
                    "Je veux voir le $translatedWord."
                }
            }
            "German" -> when (sentence) {
                "Hello, how are you today?" -> "Hallo, wie geht es dir heute?"
                "It is time to say goodbye." -> "Es ist Zeit, Abschied zu nehmen."
                "I am going to my house." -> "Ich gehe zu meinem Haus."
                "The dog is playing in the park." -> "Der Hund spielt im Park."
                "I like to eat apples." -> "Ich esse gerne Äpfel."
                "Can I have some water, please?" -> "Kann ich bitte etwas Wasser haben?"
                "The bread is fresh and warm." -> "Das Brot ist frisch und warm."
                "The city is very busy at night." -> "Die Stadt ist nachts sehr belebt."
                "You are a very good friend." -> "Du bist ein sehr guter Freund."
                "I love spending time with my family." -> "Ich liebe es, Zeit mit meiner Familie zu verbringen."
                else -> {
                    val word = sentence.removePrefix("I want to see the ").removeSuffix(".")
                    val translatedWord = translateToGerman(word)
                    "Ich möchte das $translatedWord sehen."
                }
            }
            "Italian" -> when (sentence) {
                "Hello, how are you today?" -> "Ciao, come stai oggi?"
                "It is time to say goodbye." -> "È ora di dire addio."
                "I am going to my house." -> "Vado a casa mia."
                "The dog is playing in the park." -> "Il cane sta giocando nel parco."
                "I like to eat apples." -> "Mi piace mangiare le mele."
                "Can I have some water, please?" -> "Posso avere dell'acqua, per favore?"
                "The bread is fresh and warm." -> "Il pane è fresco e caldo."
                "The city is very busy at night." -> "La città è molto trafficata di notte."
                "You are a very good friend." -> "Sei un ottimo amico."
                "I love spending time with my family." -> "Amo passare il tempo con la mia famiglia."
                else -> {
                    val word = sentence.removePrefix("I want to see the ").removeSuffix(".")
                    val translatedWord = translateToItalian(word)
                    "Voglio vedere il $translatedWord."
                }
            }
            "Portuguese" -> when (sentence) {
                "Hello, how are you today?" -> "Olá, como você está hoje?"
                "It is time to say goodbye." -> "É hora de dizer adeus."
                "I am going to my house." -> "Vou para minha casa."
                "The dog is playing in the park." -> "O cachorro está brincando no parque."
                "I like to eat apples." -> "Eu gosto de comer maçãs."
                "Can I have some water, please?" -> "Posso tomar um pouco de água, por favor?"
                "The bread is fresh and warm." -> "O pão está fresco e quente."
                "The city is very busy at night." -> "A cidade está muito movimentada à noite."
                "You are a very good friend." -> "Você é um excelente amigo."
                "I love spending time with my family." -> "Eu amo passar tempo com minha família."
                else -> {
                    val word = sentence.removePrefix("I want to see the ").removeSuffix(".")
                    val translatedWord = translateToPortuguese(word)
                    "Eu quero ver o $translatedWord."
                }
            }
            "Afrikaans" -> when (sentence) {
                "Hello, how are you today?" -> "Hallo, hoe gaan dit vandag?"
                "It is time to say goodbye." -> "Dit is tyd om totsiens te sê."
                "I am going to my house." -> "Ek gaan na my huis toe."
                "The dog is playing in the park." -> "Die hond speel in die park."
                "I like to eat apples." -> "Ek hou daarvan om appels te eet."
                "Can I have some water, please?" -> "Kan ek asseblief water kry?"
                "The bread is fresh and warm." -> "Die brood is vars en warm."
                "The city is very busy at night." -> "Die stad is baie besig in die nag."
                "You are a very good friend." -> "Jy is 'n baie goeie vriend."
                "I love spending time with my family." -> "Ek is lief daarvoor om tyd saam met my familie te spandeer."
                else -> {
                    val word = sentence.removePrefix("I want to see the ").removeSuffix(".")
                    val translatedWord = translateToAfrikaans(word)
                    "Ek wil die $translatedWord sien."
                }
            }
            else -> sentence
        }
    }

    private fun getPartOfSpeech(eng: String): String {
        return when {
            verbVocab.contains(eng) -> "verb"
            adverbVocab.contains(eng) -> "adverb"
            adjectiveVocab.contains(eng) -> "adjective"
            baseVocab.contains(eng) -> "noun"
            else -> "phrase"
        }
    }

    private fun getTranslatedTitle(lessonNum: Int, lang: String): String {
        val base = when (lang) {
            "Spanish" -> "Lección"
            "French" -> "Leçon"
            "German" -> "Lektion"
            "Italian" -> "Lezione"
            "Portuguese" -> "Lição"
            "Afrikaans" -> "Les"
            else -> "Lesson"
        }
        return "$base $lessonNum"
    }

    private fun getTranslatedCategory(unitNum: Int, lang: String): String {
        val themes = listOf(
            "Greetings & Basics", "Family & People", "Numbers & Time", "Food & Dining",
            "Home & Daily Life", "Colors & Fashion", "Travel & Directions", "Health & Body",
            "Nature & Weather", "Shopping & Money", "Work & Business", "Hobbies & Leisure",
            "Transportation", "Technology & Media", "Animals & Environment", "Emotions & Traits",
            "Education & Study", "Arts & Culture", "Society & Community", "Law & Justice",
            "History & Heritage", "Science & Discovery", "Sports & Fitness", "Style & Design",
            "Economy & Trade", "Kitchen & Cooking", "Buildings & Places", "Celebrations",
            "Music & Sound", "Communication", "Future & Ambition", "Memories & Past",
            "Ideas & Opinions", "Philosophy", "Essential Phrases"
        )
        val theme = themes.getOrNull(unitNum - 1) ?: "General"
        
        val prefix = when (lang) {
            "Spanish" -> "Unidad"
            "French" -> "Unité"
            "German" -> "Einheit"
            "Italian" -> "Unità"
            "Portuguese" -> "Unidade"
            "Afrikaans" -> "Eenheid"
            else -> "Unit"
        }
        
        return "$prefix $unitNum: $theme"
    }

    private fun generatePhonetic(word: String, lang: String): String {
        return when (lang) {
            "Spanish" -> "/" + word.replace("v", "b").replace("h", "").replace("j", "h").replace("ll", "y").replace("ñ", "ny") + "/"
            "French" -> "/" + word.replace("ou", "u").replace("eau", "o").replace("ch", "sh").replace("gn", "ny").replace("qu", "k") + "/"
            "German" -> "/" + word.replace("w", "v").replace("v", "f").replace("z", "ts").replace("sch", "sh").replace("ie", "ee").replace("ei", "ai").replace("j", "y").replace("ch", "kh") + "/"
            "Italian" -> "/" + word.replace("gn", "ny").replace("gl", "ly").replace("ci", "chi").replace("ce", "che") + "/"
            "Portuguese" -> "/" + word.replace("lh", "ly").replace("nh", "ny").replace("ção", "são").replace("ch", "sh").replace("ou", "o") + "/"
            "Afrikaans" -> "/" + word.replace("g", "h").replace("v", "f").replace("sj", "sh").replace("oe", "u").replace("ij", "y").replace("ui", "ay").replace("aa", "ah").replace("oo", "oh") + "/"
            else -> "/$word/"
        }.lowercase()
    }

    override fun getLessons(): Flow<List<Lesson>> {
        return lessonProgressDao.getAllProgress(Constants.DEFAULT_USER_ID).map { progressList ->
            val progressMap = progressList.associate { it.lessonId to it.isCompleted }
            nativeLanguages.flatMap { lang ->
                val langCode = when(lang) {
                    "English" -> "EN"
                    "Spanish" -> "ES"
                    "French" -> "FR"
                    "German" -> "DE"
                    "Italian" -> "IT"
                    "Portuguese" -> "PT"
                    "Afrikaans" -> "AF"
                    else -> lang.take(2).uppercase()
                }
                (1..35).flatMap { unitNum ->
                    (1..5).map { lessonNum ->
                        val lessonIdx = (unitNum - 1) * 5 + (lessonNum - 1)
                        val id = "lesson_${lessonIdx}_$langCode"
                        Lesson(
                            id = id,
                            language = lang,
                            titleTranslations = nativeLanguages.associateWith { getTranslatedTitle(lessonNum, it) },
                            categoryTranslations = nativeLanguages.associateWith { getTranslatedCategory(unitNum, it) },
                            isCompleted = progressMap[id] ?: false,
                            content = emptyList()
                        )
                    }
                }
            }
        }
    }

    override suspend fun getLessonById(id: String): Lesson? {
        val userId = Constants.DEFAULT_USER_ID
        val parts = id.split("_")
        val lessonIdx = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val langPrefix = parts.getOrNull(2) ?: "ES"
        
        val targetLang = when(langPrefix) {
            "EN" -> "English"
            "SP", "ES" -> "Spanish"
            "FR" -> "French"
            "GE", "DE" -> "German"
            "IT" -> "Italian"
            "PO", "PT" -> "Portuguese"
            "AF" -> "Afrikaans"
            else -> nativeLanguages.find { it.take(2).uppercase() == langPrefix } ?: "Spanish"
        }
        val chunk = keyChunks.getOrNull(lessonIdx) ?: return null
        
        val unitNum = (lessonIdx / 5) + 1
        val lessonNum = (lessonIdx % 5) + 1
        val progress = lessonProgressDao.getProgress(userId, id)

        return Lesson(
            id = id,
            language = targetLang,
            titleTranslations = nativeLanguages.associateWith { getTranslatedTitle(lessonNum, it) },
            categoryTranslations = nativeLanguages.associateWith { getTranslatedCategory(unitNum, it) },
            isCompleted = progress?.isCompleted ?: false,
            content = generateLessonContent(
                targetLang,
                chunk.map { key -> key to (allLanguageVocab[targetLang]?.get(key) ?: key) },
                unitNum
            )
        )
    }

    override suspend fun completeLesson(userId: String, lessonId: String, score: Int) {
        lessonProgressDao.markCompleted(userId, lessonId, score, System.currentTimeMillis())
    }

    override suspend fun getMissedItems(userId: String): List<String> {
        val allProgress = lessonProgressDao.getAllProgress(userId).firstOrNull() ?: return emptyList()
        val missedItems = mutableListOf<String>()
        
        allProgress.filter { !it.isCompleted || it.score < 100 }.forEach { progress ->
            val lesson = getLessonById(progress.lessonId)
            lesson?.content?.forEach { item ->
                item.translations.keys.forEach { eng ->
                    missedItems.add(eng)
                }
            }
        }
        return missedItems.distinct().shuffled().take(30)
    }
}
