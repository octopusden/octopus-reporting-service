package org.octopusden.octopus.reporting.automation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import org.octopusden.octopus.reporting.automation.generator.VelocityEngine
import org.slf4j.LoggerFactory
import java.io.File

class ReportCommand : CliktCommand(name = "") {

    private val context by findOrSetObject { mutableMapOf<String, Any>() }

    private val jsonFile by option(JSON_FILE, help = "Report json file")
        .file(mustExist = false)
        .required()
        .validate { require(it.extension == "json") }

    private val reportCustom by option(REPORT_CUSTOM, help = "Enable custom report")
        .convert { it.trim().toBoolean() }
        .default(false)

    private val reportCustomTemplateValue by option(
        REPORT_CUSTOM_TEMPLATE,
        help = "Report custom template file"
    ).convert { it.trim() }

    private val reportCustomTemplateClasspathValue by option(
        REPORT_CUSTOM_TEMPLATE_CLASSPATH,
        help = "Report custom template classpath resource"
    ).convert { it.trim() }

    private val reportCustomTemplate: File?
        get() = reportCustomTemplateValue
            ?.takeIf { it.isNotEmpty() }
            ?.let(::File)

    private val reportCustomTemplateClasspath: String?
        get() = reportCustomTemplateClasspathValue
            ?.takeIf { it.isNotEmpty() }

    private val reportCustomFile by option(REPORT_CUSTOM_FILE, help = "Report custom file")
        .file()

    private val reportCustomEscapeHtml by option(REPORT_ESCAPE, help = "Escape Html")
        .convert { it.trim().toBoolean() }
        .default(true)

    private val teamCityPublish by option(
        TEAMCITY_PUBLISH,
        help = "Publish generated reports to TeamCity artifacts"
    )
        .convert { it.trim().toBoolean() }
        .default(false)

    private val reportEngine = VelocityEngine()

    override fun run() {
        val log = LoggerFactory.getLogger(ReportCommand::class.java.`package`.name)
        context[LOG] = log
        context[REPORT] = this

        // Check parameters
        if (reportCustom) {
            if (reportCustomTemplate == null && reportCustomTemplateClasspath == null) {
                throw BadParameterValue("$REPORT_CUSTOM_TEMPLATE or $REPORT_CUSTOM_TEMPLATE_CLASSPATH is required when $REPORT_CUSTOM is true")
            }

            if (reportCustomTemplate != null && reportCustomTemplateClasspath != null) {
                throw BadParameterValue("$REPORT_CUSTOM_TEMPLATE and $REPORT_CUSTOM_TEMPLATE_CLASSPATH cannot be specified together")
            }

            if (reportCustomFile == null) {
                throw BadParameterValue("$REPORT_CUSTOM_FILE is required when $REPORT_CUSTOM is true")
            }

            reportCustomTemplate?.let { templateFile ->
                if (!templateFile.exists()) {
                    throw BadParameterValue(
                        "$REPORT_CUSTOM_TEMPLATE: File ${templateFile.absolutePath} does not exist"
                    )
                }
            }

            reportCustomTemplateClasspath?.let { resource ->
                if (javaClass.classLoader.getResource(resource) == null) {
                    throw BadParameterValue("$REPORT_CUSTOM_TEMPLATE_CLASSPATH: Resource '$resource' does not exist")
                }
            }
        }
    }

    fun write(reportContext: Map<String, Any>, reportData: Any) {
        jacksonObjectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValue(jsonFile, reportData)

        if (reportCustom) {
            val templateFile = reportCustomTemplate
                ?: loadClasspathTemplate(reportCustomTemplateClasspath!!)

            reportCustomFile!!.writeText(
                reportEngine.generate(
                    reportContext,
                    templateFile,
                    reportCustomEscapeHtml
                )
            )
        }

        if (teamCityPublish) {
            publishReportToTeamCity()
        }
    }

    private fun publishReportToTeamCity() {
        println("##teamcity[publishArtifacts '${escapeTeamCityValue(jsonFile.path)}']")
        if (reportCustom) {
            println("##teamcity[publishArtifacts '${escapeTeamCityValue(reportCustomFile!!.path)}']")
        }
    }

    private fun escapeTeamCityValue(value: String): String =
        value
            .replace("|", "||")
            .replace("'", "|'")
            .replace("\n", "|n")
            .replace("\r", "|r")
            .replace("[", "|[")
            .replace("]", "|]")

    private fun loadClasspathTemplate(resource: String): File {
        val input = javaClass.classLoader.getResourceAsStream(resource)
            ?: throw BadParameterValue("$REPORT_CUSTOM_TEMPLATE_CLASSPATH: Resource '$resource' does not exist")

        return File.createTempFile("report-template-", ".vm").apply {
            outputStream().use { output ->
                input.use { it.copyTo(output) }
            }
            deleteOnExit()
        }
    }

    companion object {
        const val REPORT = "report"
        const val JSON_FILE = "--json-file"
        const val REPORT_CUSTOM = "--report-custom"
        const val REPORT_CUSTOM_TEMPLATE = "--report-custom-template"
        const val REPORT_CUSTOM_TEMPLATE_CLASSPATH = "--report-custom-template-classpath"
        const val REPORT_CUSTOM_FILE = "--report-custom-file"
        const val REPORT_ESCAPE = "--report-escape"
        const val TEAMCITY_PUBLISH = "--teamcity-publish"
        const val LOG = "log"
    }
}