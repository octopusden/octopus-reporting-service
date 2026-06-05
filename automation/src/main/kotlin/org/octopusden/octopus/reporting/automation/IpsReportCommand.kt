package org.octopusden.octopus.reporting.automation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import org.octopusden.octopus.jira.api.client.JiraApiClient
import org.octopusden.octopus.jira.api.client.impl.ClassicJiraApiClient
import org.octopusden.octopus.jira.api.client.impl.JiraApiClientParametersProvider
import org.slf4j.Logger

class IpsReportCommand : CliktCommand(name = COMMAND) {
    private val context by requireObject<MutableMap<String, Any>>()
    private val log by lazy { context[ReportCommand.LOG] as Logger }
    private val report by lazy { context[ReportCommand.REPORT] as ReportCommand }

    private val url by option(URL_OPTION, help = "Jira URL").convert { it.trim() }.required()
        .check("$URL_OPTION is empty") { it.isNotEmpty() }
    private val ips by option(IPS_OPTION, help = "IPS").convert { it.trim() }.required()
        .check("$IPS_OPTION is empty") { it.isNotEmpty() }
    private val sinceYear by option(SINCE_YEAR_OPTION, help = "Since year").int()
    private val sinceDate by option(SINCE_DATE_OPTION, help = "Since date").convert { it.trim() }
    private val release by option(RELEASE_OPTION, help = "Release").convert { it.trim() }
    private val system by option(SYSTEM_OPTION, help = "System").convert { it.trim() }
    private val mandatory by option(MANDATORY_OPTION, help = "Mandatory").convert { it.trim().toBoolean() }
        .default(true)

    override fun run() {
        val client: JiraApiClient = ClassicJiraApiClient(object : JiraApiClientParametersProvider {
            override fun getApiUrl() = url
            override fun getBasicCredentials() = null
            override fun getBearerToken() = null
        })

        log.info("Generate IPS Report")

        val ipsData = client.getIps(ips, sinceYear, sinceDate, release, system, mandatory)
        val reportContext = mutableMapOf(
            "ips" to ips,
            "ipsRelease" to ipsData
        ).apply {
            if (!release.isNullOrBlank()) put("release", release!!)
        }
        report.write(reportContext, ipsData)
    }

    companion object {
        const val COMMAND = "generate-ips-report"
        const val URL_OPTION = "--url"
        const val IPS_OPTION = "--ips"
        const val SINCE_YEAR_OPTION = "--sinceYear"
        const val SINCE_DATE_OPTION = "--sinceDate"
        const val RELEASE_OPTION = "--release"
        const val SYSTEM_OPTION = "--system"
        const val MANDATORY_OPTION = "--mandatory"
    }

}