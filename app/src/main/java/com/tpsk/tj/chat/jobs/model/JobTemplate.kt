package com.tpsk.tj.chat.jobs.model

data class JobTemplate(
    val id: String,
    val title: String,
    val description: String,
    val employmentType: String,
    val responsibilities: List<String>,
    val requirements: List<String>
)

object JobTemplatesMock {
    val templates = listOf(
        JobTemplate(
            id = "1",
            title = "General Worker",
            description = "We are looking for a hardworking and reliable General Worker to join our team. The successful candidate will assist with daily operations, maintain cleanliness and perform general duties as assigned.",
            employmentType = "Permanent",
            responsibilities = listOf(
                "Assist with daily operations.",
                "Maintain a clean working environment.",
                "Handle stock and materials.",
                "Follow workplace safety procedures."
            ),
            requirements = listOf(
                "Grade 10 or higher.",
                "Good communication skills.",
                "Ability to work in a team.",
                "Previous experience advantageous."
            )
        ),
        JobTemplate(
            id = "2",
            title = "Driver",
            description = "We are seeking a responsible Driver to safely transport passengers or goods. Ensure the vehicle is maintained and logbooks are updated regularly.",
            employmentType = "Contract",
            responsibilities = listOf(
                "Transport passengers or goods safely.",
                "Perform daily vehicle inspections.",
                "Maintain accurate logbooks.",
                "Ensure compliance with road regulations."
            ),
            requirements = listOf(
                "Valid driver's license with PrDP.",
                "Clean driving record.",
                "At least 2 years of professional driving experience.",
                "Good knowledge of local routes."
            )
        ),
        JobTemplate(
            id = "3",
            title = "Security Guard",
            description = "Looking for a vigilant Security Guard to protect premises, assets, and personnel. You will maintain a high visibility presence and prevent all illegal or inappropriate actions.",
            employmentType = "Permanent",
            responsibilities = listOf(
                "Patrol premises regularly.",
                "Monitor surveillance equipment.",
                "Inspect buildings, equipment, and access points.",
                "Report irregularities and potential safety hazards."
            ),
            requirements = listOf(
                "PSIRA registered (Grade C or above).",
                "Proven experience as a Security Guard.",
                "Excellent surveillance and observation skills.",
                "High school diploma or equivalent."
            )
        ),
        JobTemplate(
            id = "4",
            title = "Waiter / Waitress",
            description = "We are looking for an energetic Waiter/Waitress to take orders and deliver food and beverages to our customers.",
            employmentType = "Permanent",
            responsibilities = listOf(
                "Greet and escort customers to their tables.",
                "Present menu and provide detailed information when asked.",
                "Prepare tables by setting up linens, silverware and glasses.",
                "Take accurate food and drinks orders."
            ),
            requirements = listOf(
                "Proven work experience as a Waiter or Waitress.",
                "Basic math skills.",
                "Attentiveness and patience for customers.",
                "Strong organizational and multitasking skills."
            )
        ),
        JobTemplate(
            id = "5",
            title = "IT Support",
            description = "We are looking for an IT Support Specialist to provide technical assistance to our staff. You will help install, upgrade and troubleshoot hardware and software systems.",
            employmentType = "Permanent",
            responsibilities = listOf(
                "Install and configure computer hardware, software, systems.",
                "Monitor and maintain computer systems and networks.",
                "Respond in a timely manner to service issues and requests.",
                "Provide technical support across the company."
            ),
            requirements = listOf(
                "Degree or Diploma in IT or related field.",
                "Proven experience as an IT Help Desk technician.",
                "Good understanding of computer systems, mobile devices.",
                "Ability to diagnose and resolve basic technical issues."
            )
        )
    )
}
