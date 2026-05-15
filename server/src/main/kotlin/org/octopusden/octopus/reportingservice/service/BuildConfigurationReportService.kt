package org.octopusden.octopus.reportingservice.service

import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportRequestDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportResponseDto

interface BuildConfigurationReportService {
    fun generateReport(request: BuildConfigurationReportRequestDto): BuildConfigurationReportResponseDto
}