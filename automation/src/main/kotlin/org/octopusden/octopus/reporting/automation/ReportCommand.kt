package org.octopusden.octopus.reporting.automation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.types.file
import org.octopusden.octopus.reporting.automation.generator.VelocityEngine
import org.slf4j.LoggerFactory

class ReportCommand : CliktCommand(name = "") {
    private val context by findOrSetObject { mutableMapOf<String, Any>() }

    private val jsonFile by option(JSON_FILE, help = "Report json file")
        .file(mustExist = false)
        .required()
        .validate { require(it.extension == "json") }

    private val reportCustom by option(REPORT_CUSTOM, help = "Enable custom report")
        .convert { it.trim().toBoolean() }.default(false)
    private val reportCustomTemplate by option(REPORT_CUSTOM_TEMPLATE, help = "Report custom template file")
        .file()

    private val reportCustomFile by option(REPORT_CUSTOM_FILE, help = "Report custom file")
        .file()

    private val reportCustomEscapeHtml by option(REPORT_ESCAPE, help = "Escape Html")
        .convert { it.trim().toBoolean() }.default(true)

    private val reportEngine = VelocityEngine()

    override fun run() {
        val log = LoggerFactory.getLogger(ReportCommand::class.java.`package`.name)
        context[LOG] = log
        context[REPORT] = this

        // Check parameters
        if (reportCustom) {
            if (reportCustomTemplate == null) {
                throw BadParameterValue("$REPORT_CUSTOM_TEMPLATE is required when $REPORT_CUSTOM is true")
            }
            if (reportCustomFile == null) {
                throw BadParameterValue("$REPORT_CUSTOM_FILE is required when $REPORT_CUSTOM is true")
            }
            reportCustomTemplate?.let { templateFile ->
                if (!templateFile.exists()) {
                    throw BadParameterValue("$REPORT_CUSTOM_TEMPLATE : File ${templateFile.absolutePath} does not exist")
                }
            }
        }
    }

    fun write(reportContext: Map<String, Any>, reportData: Any) {
        jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValue(jsonFile, reportData)
        if (reportCustom) {
            reportCustomFile?.writeText(
                reportEngine.generate(reportContext, reportCustomTemplate!!, reportCustomEscapeHtml)
            )
        }
    }

    companion object {
        const val REPORT = "report"
        const val JSON_FILE = "--json-file"
        const val REPORT_CUSTOM = "--report-custom"
        const val REPORT_CUSTOM_TEMPLATE = "--report-custom-template"
        const val REPORT_CUSTOM_FILE = "--report-custom-file"
        const val REPORT_ESCAPE = "--report-escape"
        const val LOG = "log"
    }
}