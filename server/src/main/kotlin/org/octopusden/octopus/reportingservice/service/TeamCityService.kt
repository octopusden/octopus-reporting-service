package org.octopusden.octopus.reportingservice.service

import org.octopusden.octopus.reportingservice.dto.BuildConfiguration
import org.octopusden.octopus.reportingservice.dto.BuildConfigurationProject

interface TeamCityService {
    fun findSubprojects(rootProjectId: String): List<BuildConfigurationProject>
    fun getTemplateByProjectIdAndTemplateId(projectId: String, templateId: String): BuildConfiguration
}