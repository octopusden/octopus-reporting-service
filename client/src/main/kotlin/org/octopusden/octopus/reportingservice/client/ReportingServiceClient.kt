package org.octopusden.octopus.reportingservice.client

import feign.Headers
import feign.RequestLine
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportRequestDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportResponseDto

interface ReportingServiceClient {
    @RequestLine("POST /rest/api/1/reports/build-configuration")
    @Headers("Content-Type: application/json")
    fun generateBuildConfigurationReport(request: BuildConfigurationReportRequestDto): BuildConfigurationReportResponseDto
}
