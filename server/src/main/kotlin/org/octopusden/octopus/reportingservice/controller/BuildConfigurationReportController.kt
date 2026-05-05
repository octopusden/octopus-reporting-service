package org.octopusden.octopus.reportingservice.controller

import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportRequestDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportResponseDto
import org.octopusden.octopus.reportingservice.service.BuildConfigurationReportService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/rest/api/1/reports/build-configuration")
@RestController
class BuildConfigurationReportController(
    private val buildConfigurationReportService: BuildConfigurationReportService
) {
    @PostMapping
    fun generateReport(
        @RequestBody request: BuildConfigurationReportRequestDto
    ): BuildConfigurationReportResponseDto {
        logger.info("Generate Build Configuration Report: request={}", request)
        return buildConfigurationReportService.generateReport(request)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildConfigurationReportController::class.java)
    }
}
