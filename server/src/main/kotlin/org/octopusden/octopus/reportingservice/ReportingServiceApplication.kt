package org.octopusden.octopus.reportingservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class ReportingServiceApplication

fun main(args: Array<String>) {
    runApplication<ReportingServiceApplication>(*args)
}
