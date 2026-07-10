package org.octopusden.octopus.reporting.automation

import com.github.ajalt.clikt.core.subcommands

const val SPLIT_SYMBOLS = "[,;]"

fun main(args: Array<String>) {
    ReportCommand().subcommands(
        IpsReportCommand(),
        BuildConfigurationReportCommand(),
        PublishedArtifactsReportCommand()
    ).main(args)
}
