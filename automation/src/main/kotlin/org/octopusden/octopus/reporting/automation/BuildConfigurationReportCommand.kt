package org.octopusden.octopus.reporting.automation

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import feign.RequestInterceptor
import org.octopusden.octopus.infrastructure.client.commons.ClientParametersProvider
import org.octopusden.octopus.infrastructure.client.commons.CredentialProvider
import org.octopusden.octopus.infrastructure.confluence.client.ConfluenceClassicClient
import org.octopusden.octopus.infrastructure.confluence.client.dto.ConfluencePageBody
import org.octopusden.octopus.infrastructure.confluence.client.dto.ConfluencePageUpdateRequest
import org.octopusden.octopus.infrastructure.confluence.client.dto.ConfluencePageVersion
import org.octopusden.octopus.infrastructure.confluence.client.dto.ConfluenceStorage
import org.octopusden.octopus.reporting.automation.generator.VelocityEngine
import org.octopusden.octopus.reportingservice.client.ReportingServiceClientConfig
import org.octopusden.octopus.reportingservice.client.ReportingServiceClientFactory
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportChecksDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportComponentsFilterDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportRequestDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildStage
import org.slf4j.Logger
import java.util.Base64

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

    private val publishToWiki by option(PUBLISH_TO_WIKI_OPTION, help = "Publish report to Confluence wiki")
        .convert { it.trim().toBoolean() }.default(false)

    private val wikiReportTemplate by option(WIKI_REPORT_TEMPLATE_OPTION, help = "Wiki report template file (Velocity)")
        .file()

    private val wikiPageId by option(WIKI_PAGE_ID_OPTION, help = "Confluence page ID to update")
        .convert { it.trim() }

    private val wikiURL by option(WIKI_URL_OPTION, help = "Confluence base URL")
        .convert { it.trim() }

    private val wikiUser by option(WIKI_USER_OPTION, help = "Confluence username")
        .convert { it.trim() }

    private val wikiPassword by option(WIKI_PASSWORD_OPTION, help = "Confluence password")
        .convert { it.trim() }

    private val reportEngine = VelocityEngine()

    override fun run() {
        // TODO Decrease timeout after implementing async call
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
        val reportContext = mutableMapOf(
            "rootProjectId" to response.rootProjectId,
            "result" to response.result
        )
        report.write(reportContext, response)
        if (publishToWiki) {
            publishToWiki(reportContext)
        }
        log.info("Build Configuration Report is done!")
    }

    private fun publishToWiki(reportContext: Map<String, Any>) {
        val url = wikiURL
            ?: throw IllegalArgumentException("$WIKI_URL_OPTION is required when $PUBLISH_TO_WIKI_OPTION is true")
        val user = wikiUser
            ?: throw IllegalArgumentException("$WIKI_USER_OPTION is required when $PUBLISH_TO_WIKI_OPTION is true")
        val password = wikiPassword
            ?: throw IllegalArgumentException("$WIKI_PASSWORD_OPTION is required when $PUBLISH_TO_WIKI_OPTION is true")
        val template = wikiReportTemplate
            ?: throw IllegalArgumentException("$WIKI_REPORT_TEMPLATE_OPTION is required when $PUBLISH_TO_WIKI_OPTION is true")
        val pageId = wikiPageId
            ?: throw IllegalArgumentException("$WIKI_PAGE_ID_OPTION is required when $PUBLISH_TO_WIKI_OPTION is true")

        log.info("Publishing report to Confluence: pageId=$pageId")

        val confluenceClient = ConfluenceClassicClient(
            object : ClientParametersProvider {
                override fun getApiUrl() = url
                override fun getAuth(): CredentialProvider =
                    object : CredentialProvider({
                        RequestInterceptor { template ->
                            val basic = Base64.getEncoder()
                                .encodeToString("$user:$password".toByteArray())
                            template.header("Authorization", "Basic $basic")
                        }
                    }) {}
            }
        )
        val page = confluenceClient.getPageById(pageId, mapOf("expand" to "body.storage,version,space,ancestors"))
        log.info("Fetched Confluence page: id=${page.id}, title=${page.title}, version=${page.version?.number}")

        val wikiContent = reportEngine.generate(reportContext, template)

        val updateRequest = ConfluencePageUpdateRequest(
            id = pageId,
            title = page.title,
            body = ConfluencePageBody(ConfluenceStorage(wikiContent)),
            version = ConfluencePageVersion(number = page.version?.number?.plus(1) ?: 1)
        )
        val updated = confluenceClient.updatePage(pageId, updateRequest)
        log.info("Confluence page updated: id=${updated.id}, title=${updated.title}, version=${updated.version?.number}")
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
        const val PUBLISH_TO_WIKI_OPTION = "--publish-to-wiki"
        const val WIKI_REPORT_TEMPLATE_OPTION = "--wiki-report-template"
        const val WIKI_PAGE_ID_OPTION = "--wiki-page-id"
        const val WIKI_URL_OPTION = "--wiki-url"
        const val WIKI_USER_OPTION = "--wiki-user"
        const val WIKI_PASSWORD_OPTION = "--wiki-password"
    }
}