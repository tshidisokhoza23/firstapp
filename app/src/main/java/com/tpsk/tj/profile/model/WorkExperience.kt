package com.tpsk.tj.profile.model

import com.google.gson.annotations.SerializedName

data class WorkExperience(
    @SerializedName("id") val id: String? = null,
    @SerializedName("user_id") val userId: Long? = null,
    @SerializedName("job_title") val jobTitle: String,
    @SerializedName("company_name") val companyName: String,
    @SerializedName("location") val location: String? = null,
    @SerializedName("employment_type") val employmentType: String,
    @SerializedName("is_current_job") val isCurrentJob: Boolean = false,
    @SerializedName("start_date") val startDate: Long,
    @SerializedName("end_date") val endDate: Long? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("created_at") val createdAt: Long? = null,
    @SerializedName("updated_at") val updatedAt: Long? = null
)
