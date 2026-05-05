package org.octopusden.octopus.reportingservice.config

import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildStage
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "reporting.teamcity")
data class BuildConfigurationReportConfig(
    val baseProjectId: String,
    val templates: Templates = Templates()
) {
    fun getTemplates(stage: BuildStage): List<String> = templates.getTemplates(stage)
}

data class Templates(
    val build: List<String> = emptyList(),
    val releaseCandidate: List<String> = emptyList(),
    val release: List<String> = emptyList()
) {
    fun getTemplates(stage: BuildStage): List<String> {
        val result = when (stage) {
            BuildStage.BUILD -> build
            BuildStage.RELEASE_CANDIDATE -> releaseCandidate
            BuildStage.RELEASE -> release
        }
        check(result.isNotEmpty()) {
            "No TeamCity templates configured for BuildStage=$stage"
        }
        return result
    }
}