package org.octopusden.octopus.reporting.automation

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import org.octopusden.octopus.reportingservice.client.ReportingServiceClientConfig
import org.octopusden.octopus.reportingservice.client.ReportingServiceClientFactory
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportChecksDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportComponentsFilterDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportRequestDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildStage
import org.slf4j.Logger

class BuildConfigurationReportCommand : CliktCommand(name = COMMAND) {

    private val context by requireObject<MutableMap<String, Any>>()
    private val log by lazy { context[ReportCommand.LOG] as Logger }
    private val report by lazy { context[ReportCommand.REPORT] as ReportCommand }

    private val reportingServiceUrl by option(REPORTING_SERVICE_URL_OPTION, help = "Reporting Service URL")
        .convert { it.trim() }
        .required()
        .check("$REPORTING_SERVICE_URL_OPTION is empty") {
            it.isNotEmpty()
        }

    private val rootProjectId by option(ROOT_PROJECT_ID_OPTION, help = "Root project ID")
        .convert { it.trim() }
        .required()
        .check("$ROOT_PROJECT_ID_OPTION is empty") {
            it.isNotEmpty()
        }

    private val includeSystems by option(INCLUDE_SYSTEMS_OPTION, help = "Comma-separated list of systems to include")
        .convert { it.trim() }
        .default("")

    private val excludeComponents by option(EXCLUDE_COMPONENTS_OPTION, help = "Comma-separated list of components to exclude")
        .convert { it.trim() }
        .default("")

    private val buildStage by option(BUILD_STAGE_OPTION, help = "Build stage (${BuildStage.entries.joinToString()})")
        .convert { it.trim() }
        .default(BuildStage.BUILD.name)

    private val parameters by option(PARAMETERS_OPTION, help = "Comma-separated list of parameters to check")
        .convert { it.trim() }
        .default("")

    private val steps by option(STEPS_OPTION, help = "Comma-separated list of steps to check")
        .convert { it.trim() }
        .default("")

    override fun run() {
        // TODO decrease timeout after async
        val client = ReportingServiceClientFactory.create(
            ReportingServiceClientConfig(
                baseUrl = reportingServiceUrl,
                connectTimeoutMs = 180_000,
                readTimeoutMs = 200_000
            )
        )

        log.info("Generate Build Configuration Report")

        val request = BuildConfigurationReportRequestDto(
            rootProjectId = rootProjectId,
            componentsFilter = BuildConfigurationReportComponentsFilterDto(
                includeSystems = includeSystems.split(SPLIT_SYMBOLS.toRegex()).filter { it.isNotEmpty() }.toSet(),
                excludeComponents = excludeComponents.split(SPLIT_SYMBOLS.toRegex()).filter { it.isNotEmpty() }.toSet()
            ),
            checks = BuildConfigurationReportChecksDto(
                buildStage = BuildStage.valueOf(buildStage),
                parameters = parameters.split(SPLIT_SYMBOLS.toRegex()).filter { it.isNotEmpty() },
                steps = steps.split(SPLIT_SYMBOLS.toRegex()).filter { it.isNotEmpty() }
            )
        )

        val response = client.generateBuildConfigurationReport(request)
        val reportContext = mutableMapOf<String, Any>(
            "rootProjectId" to response.rootProjectId,
            "result" to response.result
        )
        report.write(reportContext, response)
        log.info("Build Configuration Report is done!")
    }

    companion object {
        const val COMMAND = "generate-build-configuration-report"
        const val REPORTING_SERVICE_URL_OPTION = "--reporting-service-url"
        const val ROOT_PROJECT_ID_OPTION = "--root-project-id"
        const val INCLUDE_SYSTEMS_OPTION = "--include-systems"
        const val EXCLUDE_COMPONENTS_OPTION = "--exclude-components"
        const val BUILD_STAGE_OPTION = "--build-stage"
        const val PARAMETERS_OPTION = "--parameters"
        const val STEPS_OPTION = "--steps"
    }
}