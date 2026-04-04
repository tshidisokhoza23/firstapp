package com.example.firstapp.data

import androidx.compose.ui.graphics.Color
import com.example.firstapp.model.Job

object JobService {
    val dummyJobs = listOf(
        Job(1, "Senior Software Quality Engineer", "Nedbank", "Johannesburg", Color(0xFF006341)),
        Job(2, "Android Developer", "Google", "Mountain View, CA", Color(0xFF4285F4)),
        Job(3, "Frontend Engineer", "Meta", "Menlo Park, CA", Color(0xFF0668E1)),
        Job(4, "Product Designer", "Airbnb", "San Francisco, CA", Color(0xFFFF5A5F)),
        Job(5, "Data Scientist", "Amazon", "Seattle, WA", Color(0xFFFF9900)),
        Job(6, "Full Stack Developer", "Netflix", "Los Gatos, CA", Color(0xFFE50914)),
        Job(7, "Backend Engineer", "Spotify", "Stockholm, Sweden", Color(0xFF1DB954)),
        Job(8, "Cloud Architect", "Microsoft", "Redmond, WA", Color(0xFF00A4EF)),
        Job(9, "Security Analyst", "IBM", "Armonk, NY", Color(0xFF052FAD)),
        Job(10, "DevOps Engineer", "Apple", "Cupertino, CA", Color(0xFF555555)),
        Job(11, "QA Engineer", "Samsung", "Suwon, South Korea", Color(0xFF034EA2)),
        Job(12, "Mobile Engineer", "Uber", "San Francisco, CA", Color(0xFF000000)),
        Job(13, "Machine Learning Engineer", "Tesla", "Austin, TX", Color(0xFFCC0000)),
        Job(14, "Systems Architect", "Intel", "Santa Clara, CA", Color(0xFF0071C5)),
        Job(15, "UX Researcher", "Twitter", "San Francisco, CA", Color(0xFF1DA1F2)),
        Job(16, "Database Administrator", "Oracle", "Austin, TX", Color(0xFFF80000)),
        Job(17, "Network Engineer", "Cisco", "San Jose, CA", Color(0xFF049FD9)),
        Job(18, "Software Engineer", "Adobe", "San Jose, CA", Color(0xFFFF0000)),
        Job(19, "Technical Lead", "Salesforce", "San Francisco, CA", Color(0xFF00A1E0)),
        Job(20, "Blockchain Developer", "Coinbase", "Remote", Color(0xFF0052FF))
    )
}
