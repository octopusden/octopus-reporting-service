package org.octopusden.octopus.reportingservice.service

import org.octopusden.octopus.reportingservice.domain.BuildConfiguration
import org.octopusden.octopus.reportingservice.domain.BuildConfigurationProject

interface TeamCityService {
    fun findSubprojects(rootProjectId: String): List<BuildConfigurationProject>

    fun getTemplateByProjectIdAndTemplateId(
        projectId: String,
        templateId: String,
    ): BuildConfiguration
}
