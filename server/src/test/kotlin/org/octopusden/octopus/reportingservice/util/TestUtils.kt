package org.octopusden.octopus.reportingservice.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object TestUtils {

    val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    fun <T> loadObject(resourcePath: String, typeReference: TypeReference<T>): T {
        val stream = TestUtils::class.java.classLoader.getResourceAsStream(resourcePath)
            ?: error("Resource '$resourcePath' not found in classpath")
        return stream.use { objectMapper.readValue(it, typeReference) }
    }
}