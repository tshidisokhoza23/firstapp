package com.example.firstapp.data

import androidx.compose.ui.graphics.Color
import com.example.firstapp.model.Job

object JobService {
    val dummyJobs = listOf(
        Job(1, "Senior Software Quality Engineer", "Nedbank", location = "Johannesburg", logoColor = Color(0xFF006341)),
        Job(2, "Android Developer", "Google", location = "Mountain View, CA", logoColor = Color(0xFF4285F4)),
        Job(3, "Frontend Engineer", "Meta", location = "Menlo Park, CA", logoColor = Color(0xFF0668E1)),
        Job(4, "Product Designer", "Airbnb", location = "San Francisco, CA", logoColor = Color(0xFFFF5A5F)),
        Job(5, "Data Scientist", "Amazon", location = "Seattle, WA", logoColor = Color(0xFFFF9900)),
        Job(6, "Full Stack Developer", "Netflix", location = "Los Gatos, CA", logoColor = Color(0xFFE50914)),
        Job(7, "Backend Engineer", "Spotify", location = "Stockholm, Sweden", logoColor = Color(0xFF1DB954)),
        Job(8, "Cloud Architect", "Microsoft", location = "Redmond, WA", logoColor = Color(0xFF00A4EF)),
        Job(9, "Security Analyst", "IBM", location = "Armonk, NY", logoColor = Color(0xFF052FAD)),
        Job(10, "DevOps Engineer", "Apple", location = "Cupertino, CA", logoColor = Color(0xFF555555)),
        Job(11, "QA Engineer", "Samsung", location = "Suwon, South Korea", logoColor = Color(0xFF034EA2)),
        Job(12, "Mobile Engineer", "Uber", location = "San Francisco, CA", logoColor = Color(0xFF000000)),
        Job(13, "Machine Learning Engineer", "Tesla", location = "Austin, TX", logoColor = Color(0xFFCC0000)),
        Job(14, "Systems Architect", "Intel", location = "Santa Clara, CA", logoColor = Color(0xFF0071C5)),
        Job(15, "UX Researcher", "Twitter", location = "San Francisco, CA", logoColor = Color(0xFF1DA1F2)),
        Job(16, "Database Administrator", "Oracle", location = "Austin, TX", logoColor = Color(0xFFF80000)),
        Job(17, "Network Engineer", "Cisco", location = "San Jose, CA", logoColor = Color(0xFF049FD9)),
        Job(18, "Software Engineer", "Adobe", location = "San Jose, CA", logoColor = Color(0xFFFF0000)),
        Job(19, "Technical Lead", "Salesforce", location = "San Francisco, CA", logoColor = Color(0xFF00A1E0)),
        Job(20, "Blockchain Developer", "Coinbase", location = "Remote", logoColor = Color(0xFF0052FF))
    )
}
