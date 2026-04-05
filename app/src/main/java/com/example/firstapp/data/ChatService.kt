package com.example.firstapp.data

import androidx.compose.ui.graphics.Color
import com.example.firstapp.model.*

object ChatService {
    val dummyChats = listOf(
        ChatMessage(id = 1, senderName = "Sbongile Tindleni", lastMessage = "Are we still meeting at 5?", time = "04:30 PM", unreadCount = 2, avatarColor = Color(0xFF3F51B5), isOnline = true),
        ChatMessage(id = 2, senderName = "Lebza", lastMessage = "Check out the new track! 🔥", time = "03:15 PM", unreadCount = 0, avatarColor = Color(0xFF5E35B1), isOnline = false),
        ChatMessage(id = 3, senderName = "Palesa Mojaki", lastMessage = "The job application was successful!", time = "02:00 PM", unreadCount = 0, avatarColor = Color(0xFF00796B), isOnline = true),
        ChatMessage(id = 4, senderName = "Baradi ba St. Phillimon", lastMessage = "Next rehearsal is on Sunday morning.", time = "12:30 PM", unreadCount = 5, avatarColor = Color(0xFF455A64)),
        ChatMessage(id = 5, senderName = "Tshidiso", lastMessage = "I sent the documents to your email.", time = "10:15 AM", unreadCount = 0, avatarColor = Color(0xFF7E57C2), isOnline = true),
        ChatMessage(id = 6, senderName = "Mama", lastMessage = "Don't forget to buy milk on your way home.", time = "Yesterday", unreadCount = 0, avatarColor = Color(0xFF795548)),
        ChatMessage(id = 7, senderName = "Work Group", lastMessage = "The project deadline has been extended.", time = "Yesterday", unreadCount = 12, avatarColor = Color(0xFF37474F), isMuted = true),
        ChatMessage(id = 8, senderName = "Kabelo", lastMessage = "See you at the gym!", time = "Monday", unreadCount = 0, avatarColor = Color(0xFF1976D2), isOnline = true),
        ChatMessage(id = 9, senderName = "Thabo", lastMessage = "Happy Birthday! Hope you have a great day.", time = "Sunday", unreadCount = 0, avatarColor = Color(0xFF00695C)),
        ChatMessage(id = 10, senderName = "Lerato", lastMessage = "Can you call me when you're free?", time = "Saturday", unreadCount = 0, avatarColor = Color(0xFFE64A19))
    )

    val dummyStories = listOf(
        UserStory(
            id = 1,
            userName = "My Status",
            userAvatarColor = Color(0xFF546E7A),
            stories = (1..5).map { i ->
                StoryItem(id = i, content = "My personal status update #$i", type = StoryType.TEXT, timestamp = "Today, ${i+8}:00 AM", backgroundColor = Color(0xFF311B92))
            },
            lastUpdated = "Today, 12:00 PM"
        )
    ) + (2..11).map { userId ->
        val contactName = listOf("Sbongile", "Lebza", "Palesa", "Baradi", "Tshidiso", "Mama", "Kabelo", "Thabo", "Lerato", "Neo")[userId-2]
        val palette = listOf(0xFF5E35B1, 0xFF3949AB, 0xFF1976D2, 0xFF00695C, 0xFF455A64, 0xFF7E57C2, 0xFF37474F, 0xFF1565C0)
        val colorInt = palette[userId % palette.size]
        UserStory(
            id = userId,
            userName = contactName,
            userAvatarColor = Color(colorInt),
            stories = (1..5).map { storyId ->
                StoryItem(
                    id = userId * 100 + storyId,
                    content = "Hey! This is $contactName's update #$storyId. Loving this app! 🚀",
                    type = if (storyId == 3) StoryType.VIDEO else StoryType.TEXT,
                    timestamp = "${storyId}h ago",
                    backgroundColor = Color(colorInt).copy(alpha = 0.8f)
                )
            },
            lastUpdated = "${userId-1}h ago"
        )
    }

    val dummyJobs = listOf(
        Job(id = 1, title = "Android Developer", companyName = "FirstApp Tech", location = "Johannesburg", logoColor = Color(0xFF1565C0), salaryRange = "R45k - R65k", postedTime = "2d ago", isVerified = true),
        Job(id = 2, title = "UI/UX Designer", companyName = "Creative Minds", location = "Remote", logoColor = Color(0xFF512DA8), salaryRange = "R30k - R45k", postedTime = "1d ago", jobType = "Contract"),
        Job(id = 3, title = "Project Manager", companyName = "BuildIt SA", location = "Pretoria", logoColor = Color(0xFF00695C), salaryRange = "R55k - R80k", postedTime = "3h ago"),
        Job(id = 4, title = "Data Analyst", companyName = "Insight Corp", location = "Sandton", logoColor = Color(0xFF263238), salaryRange = "R40k - R55k", postedTime = "5h ago")
    )

    val dummyAdverts = listOf(
        Advert(id = 1, advertiserName = "GadgetHub", title = "Summer Sale!", time = "2h ago", avatarColor = Color(0xFF283593), contentColor = Color(0xFFE8EAF6), contentText = "Up to 50% off on all electronics at GadgetHub.", actionText = "Shop Now"),
        Advert(id = 2, advertiserName = "CodeAcademy", title = "Learn Kotlin", time = "5h ago", avatarColor = Color(0xFF4527A0), contentColor = Color(0xFFEDE7F6), contentText = "Join our intensive 4-week Kotlin bootcamp starting next week!", actionText = "Enroll"),
        Advert(id = 3, advertiserName = "AutoFuture", title = "New Car Launch", time = "1d ago", avatarColor = Color(0xFF37474F), contentColor = Color(0xFFECEFF1), contentText = "Experience the future of driving with the new EV-1. Book a test drive.", actionText = "Book Now")
    )

    val dummyChannels = listOf(
        Channel(id = 1, name = "Android Weekly", followers = "1.2M followers", avatarColor = Color(0xFF1A237E), lastPostImageColor = Color(0xFFE8EAF6), lastPostText = "Latest news and tutorials for Android developers.", isVerified = true),
        Channel(id = 2, name = "Tech News SA", followers = "450k followers", avatarColor = Color(0xFF004D40), lastPostImageColor = Color(0xFFE0F2F1), lastPostText = "Local and international tech news updates.", isVerified = true),
        Channel(id = 3, name = "Music Vibes", followers = "2.1M followers", avatarColor = Color(0xFF4A148C), lastPostImageColor = Color(0xFFF3E5F5), lastPostText = "Daily curated playlists and artist spotlights.")
    )

    val dummyShorts = listOf(
        Short(id = 1, creatorName = "catlover", description = "Funny Cat 😹", likes = "12k", comments = "1.2k", shares = "150", thumbnailColor = Color(0xFF0D47A1)),
        Short(id = 2, creatorName = "traveler", description = "Amazing View 🏔️", likes = "45k", comments = "5k", shares = "2.3k", thumbnailColor = Color(0xFFB71C1C)),
        Short(id = 3, creatorName = "dev_guru", description = "Code Tips 💻", likes = "8k", comments = "800", shares = "45", thumbnailColor = Color(0xFF1B5E20))
    )

    val dummySongs = listOf(
        Song(id = 1, title = "Midnight City", artist = "M83", albumArtColor = Color(0xFF6A1B9A), duration = "4:03", category = "Synth-pop"),
        Song(id = 2, title = "Starboy", artist = "The Weeknd", albumArtColor = Color(0xFF212121), duration = "3:50", category = "R&B"),
        Song(id = 3, title = "Blinding Lights", artist = "The Weeknd", albumArtColor = Color(0xFF880E4F), duration = "3:20", category = "Synth-pop")
    )

    val dummyPosts = listOf(
        Post(id = 1, authorName = "TravelSA", authorHandle = "@travel_sa", authorAvatarColor = Color(0xFF0277BD), content = "Sunset over Cape Town 🌅", timestamp = "2h ago", likes = "1.2k", repliesCount = "120", shares = "45", views = "12k", isVerifiedAuthor = true),
        Post(id = 2, authorName = "DevLife", authorHandle = "@dev_life", authorAvatarColor = Color(0xFF00695C), content = "New workspace setup! 💻", timestamp = "5h ago", likes = "2.5k", repliesCount = "230", shares = "89", views = "25k"),
        Post(id = 3, authorName = "Foodie", authorHandle = "@foodie_life", authorAvatarColor = Color(0xFFBF360C), content = "Delicious Sunday brunch 🥞", timestamp = "1d ago", likes = "800", repliesCount = "56", shares = "12", views = "5k")
    )
}
