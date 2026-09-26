package com.tpsk.tj.chat.jobs.model

data class JobProfileData(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val education: String = "",
    val experience: String = "",
    val skills: String = "",
    val aboutMe: String = "",
    val linkedinUrl: String = "",
    val portfolioUrl: String = "",
    val workPreference: String = "Remote",
    val availability: String = "",
    val expectedSalary: String = "",
    val headline: String = "",
    val targetJobTitles: List<String> = emptyList(),
    val isRemoteOpen: Boolean = true,
    val experienceLevel: String = "mid",
    val minSalaryTarget: Double = 0.0,
    val resumeFileName: String? = null
) {
    val isCompleteForEasyApply: Boolean
        get() = headline.isNotBlank() || skills.isNotBlank() || !resumeFileName.isNullOrBlank()
}
