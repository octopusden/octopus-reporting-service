package org.octopusden.octopus.reportingservice.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.io.InputStream

object TestUtils {

    val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    fun <T> loadObject(file: String, typeReference: TypeReference<T>): T =
        objectMapper.readValue(File(file), typeReference)
}