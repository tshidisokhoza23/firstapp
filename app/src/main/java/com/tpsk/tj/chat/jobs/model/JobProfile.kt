package com.tpsk.tj.chat.jobs.model

data class JobProfileData(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val city: String = "",
    val country: String = "ZA",
    val education: String = "",
    val experience: String = "",
    val skills: String = "",
    val skillsList: List<String> = emptyList(),
    val aboutMe: String = "",
    val linkedinUrl: String = "",
    val portfolioUrl: String = "",
    val workPreference: String = "Remote",
    val availability: String = "",
    val expectedSalary: String = "",
    val currency: String = "ZAR",
    val headline: String = "",
    val targetJobTitles: List<String> = emptyList(),
    val isRemoteOpen: Boolean = true,
    val noFormalExperience: Boolean = false,
    val experienceLevel: String = "mid",
    val minSalaryTarget: Double = 0.0,
    val resumeFileName: String? = null
) {
    val isCompleteForEasyApply: Boolean
        get() = headline.isNotBlank() || skills.isNotBlank() || skillsList.isNotEmpty() || !resumeFileName.isNullOrBlank()

    fun getParsedSkills(): List<String> {
        if (skillsList.isNotEmpty()) return skillsList
        if (skills.isBlank()) return emptyList()
        return skills.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
    }
}
