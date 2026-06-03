package org.octopusden.octopus.reporting.automation.generator

import org.apache.commons.lang.StringEscapeUtils
import org.apache.velocity.Template
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.event.EventCartridge
import org.apache.velocity.app.event.ReferenceInsertionEventHandler
import org.apache.velocity.runtime.RuntimeConstants
import java.io.File
import java.io.StringWriter

class VelocityEngine {

    fun generate(contextMap: Map<String, Any>, templateFile: File, escapeHtml: Boolean = false): String {
        val ve = initVelocityEngineWithSimpleLog4jLog()
        val template = getTemplate(ve, templateFile)
        return getResult(contextMap, template, escapeHtml)
    }

    fun generate(contextMap: Map<String, Any>, templateFile: String, escapeHtml: Boolean = false): String {
        val ve = initVelocityEngineWithNullLog()
        val template = ve.getTemplate(templateFile, "UTF-8")
        return getResult(contextMap, template, escapeHtml)
    }

    private fun initVelocityEngineWithSimpleLog4jLog(): org.apache.velocity.app.VelocityEngine {
        val ve = org.apache.velocity.app.VelocityEngine()
        ve.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.SimpleLog4JLogSystem")
        ve.setProperty("runtime.log.logsystem.log4j.category", "velocity")
        ve.setProperty("runtime.log.logsystem.log4j.logger", "velocity")
        return ve
    }

    private fun getEventCartridge(): EventCartridge {
        val eventCartridge = EventCartridge()
        eventCartridge.addReferenceInsertionEventHandler(ReferenceInsertionEventHandler { _, value ->
            if (value == null) {
                return@ReferenceInsertionEventHandler null
            }
            StringEscapeUtils.escapeHtml(value.toString())
        })
        return eventCartridge
    }

    private fun getResult(contextMap: Map<String, Any>, template: Template, escapeHtml: Boolean): String {
        val context = getVelocityContext(contextMap, escapeHtml)
        val writer = StringWriter()
        template.merge(context, writer)
        return writer.toString()
    }


    private fun getTemplate(velocityEngine: org.apache.velocity.app.VelocityEngine, templateFile: File): Template {
        velocityEngine.setProperty(RuntimeConstants.ENCODING_DEFAULT, "UTF-8")
        velocityEngine.setProperty(RuntimeConstants.OUTPUT_ENCODING, "UTF-8")
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "file")
        velocityEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, templateFile.parentFile.absolutePath)
        return velocityEngine.getTemplate(templateFile.name, "UTF-8")
    }

    private fun getVelocityContext(contextMap: Map<String, Any>, escapeHtml: Boolean): VelocityContext {
        val context = createVelocityContext(escapeHtml)
        for (key in contextMap.keys) {
            context.put(key, contextMap[key])
        }
        return context
    }

    private fun createVelocityContext(escapeHtml: Boolean): VelocityContext {
        val context = VelocityContext()
        if (escapeHtml) {
            context.attachEventCartridge(getEventCartridge())
        }
        return context
    }


    private fun initVelocityEngineWithNullLog(): org.apache.velocity.app.VelocityEngine {
        val ve = org.apache.velocity.app.VelocityEngine()
        fixLogging(ve)
        return ve
    }


    // MRELENG-92
    private fun fixLogging(ve: org.apache.velocity.app.VelocityEngine) {
        ve.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem")
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath")
//        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader::class.java.name)
    }
}

