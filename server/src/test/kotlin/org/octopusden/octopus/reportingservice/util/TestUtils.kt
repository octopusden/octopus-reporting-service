package org.octopusden.octopus.reportingservice.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object TestUtils {
    fun getObjectMapper(): ObjectMapper = ObjectMapper().registerKotlinModule()

    fun <T> loadObject(resourcePath: String, typeReference: TypeReference<T>): T {
        val stream = javaClass.classLoader.getResourceAsStream(resourcePath)
            ?: error("Resource '$resourcePath' not found in classpath")
        return stream.use { getObjectMapper().readValue(it, typeReference) }
    }
}