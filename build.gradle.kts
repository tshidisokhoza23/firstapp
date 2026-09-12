// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.sonarqube)
}

sonar {
    properties {
        property("sonar.projectKey", System.getenv("SONAR_PROJECT_KEY") ?: "firstapp")
        property("sonar.projectName", "firstapp")
        property("sonar.organization", System.getenv("SONAR_ORGANIZATION") ?: "")
        property("sonar.host.url", System.getenv("SONAR_HOST_URL") ?: "https://sonarcloud.io")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "app/build/outputs/unit_test_code_coverage/betaDebugUnitTest/testBetaDebugUnitTestCoverage.xml," +
                "app/build/reports/jacoco/testBetaDebugUnitTestCoverage/testBetaDebugUnitTestCoverage.xml"
        )
        property(
            "sonar.android.lint.reportPaths",
            "app/build/reports/lint-results-betaDebug.xml"
        )
        property(
            "sonar.exclusions",
            listOf(
                "**/build/**",
                "**/generated/**",
                "**/*Binding.*",
                "**/R.class",
                "**/R\$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "**/*_Impl.*",
                "**/*Hilt*.*",
                "**/hilt_aggregated_deps/**",
                "**/databinding/**"
            ).joinToString(",")
        )
    }
}
