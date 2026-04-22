package org.octopusden.octopus.reportingservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ReportingServiceApplication

fun main(args: Array<String>) {
    runApplication<ReportingServiceApplication>(*args)
}
