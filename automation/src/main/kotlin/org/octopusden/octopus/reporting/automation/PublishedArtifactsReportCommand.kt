package org.octopusden.octopus.reporting.automation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import org.octopusden.octopus.infrastructure.artifactory.client.ArtifactoryClassicClient
import org.octopusden.octopus.infrastructure.client.commons.ClientParametersProvider
import org.octopusden.octopus.infrastructure.client.commons.StandardBasicCredCredentialProvider
import org.slf4j.Logger
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class PublishedArtifactsReportCommand : CliktCommand(name = COMMAND) {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private val context by requireObject<MutableMap<String, Any>>()
    private val log by lazy { context[ReportCommand.LOG] as Logger }
    private val report by lazy { context[ReportCommand.REPORT] as ReportCommand }

    private val artifactoryUrl by option(ARTIFACTORY_URL_OPTION, help = "Artifactory URL")
        .convert { it.trim().trimEnd('/') }
        .required()
        .check("$ARTIFACTORY_URL_OPTION is empty") { it.isNotEmpty() }

    private val artifactoryUser by option(ARTIFACTORY_USER_OPTION, help = "Artifactory username")
        .convert { it.trim() }
        .required()
        .check("$ARTIFACTORY_USER_OPTION is empty") { it.isNotEmpty() }

    private val artifactoryPassword by option(ARTIFACTORY_PASSWORD_OPTION, help = "Artifactory password")
        .convert { it.trim() }
        .required()
        .check("$ARTIFACTORY_PASSWORD_OPTION is empty") { it.isNotEmpty() }

    private val buildNames by option(BUILD_NAME_OPTION, help = "Build names (comma-separated)")
        .convert { it.trim() }
        .required()
        .check("$BUILD_NAME_OPTION is empty") { it.isNotEmpty() }

    private val buildNumber by option(BUILD_NUMBER_OPTION, help = "Build number")
        .convert { it.trim() }
        .required()
        .check("$BUILD_NUMBER_OPTION is empty") { it.isNotEmpty() }

    override fun run() {
        val names = buildNames
            .split(Regex(SPLIT_SYMBOLS))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        log.info("Generate published artifacts report: buildNames={}, buildNumber={}", names, buildNumber)

        val response = generateReport(names)
        report.write(
            mutableMapOf("result" to response),
            response,
        )

        log.info("Published artifacts report generated: {} artifacts", response.size)
    }

    private fun generateReport(names: List<String>): PublishedArtifactsReportResponse {
        val client = ArtifactoryClassicClient(
            object : ClientParametersProvider {
                override fun getApiUrl(): String = artifactoryUrl

                override fun getAuth() = StandardBasicCredCredentialProvider(artifactoryUser, artifactoryPassword)
            },
        )

        val aqlQuery = buildAqlQuery(names, buildNumber)
        log.debug("Find artifacts AQL: {}", aqlQuery)

        val artifactsByBuildName = client
            .searchByAQL(aqlQuery)
            .results
            .mapNotNull { item ->
                val repo = item.repo
                val path = item.path
                val name = item.name
                val buildName = item.properties
                    ?.firstOrNull { it.key == "build.name" }
                    ?.value
                if (repo == null || path == null || name == null || buildName == null) {
                    log.warn("Skip published artifact: repo={}, path={}, name={}, buildName={}", repo, path, name, buildName)
                    return@mapNotNull null
                }
                buildName to ArtifactInfo(
                    name = name,
                    url = "$artifactoryUrl/artifactory/$repo/$path/$name",
                    repository = repo,
                    path = path,
                    created = item.created?.let { OffsetDateTime.parse(it).format(CREATED_FORMATTER) }.orEmpty(),
                )
            }.groupBy(
                keySelector = { it.first },
                valueTransform = { it.second },
            )
        val builds = names.map { buildName ->
            val artifacts = artifactsByBuildName[buildName]
                .orEmpty()
                .sortedBy { it.name }
            PublishedBuildArtifacts(
                buildName = buildName,
                buildNumber = buildNumber,
                size = artifacts.size,
                artifacts = artifacts,
            )
        }

        return PublishedArtifactsReportResponse(
            size = builds.sumOf { it.size },
            builds = builds,
        )
    }

    private fun buildAqlQuery(
        names: List<String>,
        buildNumber: String,
    ): String {
        val criteria = mapOf(
            $$"$or" to names.map { buildName ->
                mapOf(
                    "@build.name" to mapOf(
                        $$"$eq" to buildName,
                    ),
                )
            },
            "@build.number" to mapOf(
                $$"$eq" to buildNumber,
            ),
        )
        val serializedCriteria = objectMapper.writeValueAsString(criteria)
        return """
            items.find($serializedCriteria)
            .include("repo","path","name","created","property")
            """.trimIndent()
    }

    data class PublishedArtifactsReportResponse(
        val size: Int,
        val builds: List<PublishedBuildArtifacts>,
    )

    data class PublishedBuildArtifacts(
        val buildName: String,
        val buildNumber: String,
        val size: Int,
        val artifacts: List<ArtifactInfo>,
    )

    data class ArtifactInfo(
        val name: String,
        val url: String,
        val repository: String,
        val path: String,
        val created: String,
    )

    companion object {
        const val COMMAND = "generate-published-artifacts-report"

        const val ARTIFACTORY_URL_OPTION = "--artifactory-url"
        const val ARTIFACTORY_USER_OPTION = "--artifactory-user"
        const val ARTIFACTORY_PASSWORD_OPTION = "--artifactory-password"
        const val BUILD_NAME_OPTION = "--build-names"
        const val BUILD_NUMBER_OPTION = "--build-number"

        private val CREATED_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss XXX")
    }
}
