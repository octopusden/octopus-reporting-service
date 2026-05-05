package org.octopusden.octopus.reportingservice.client.common.dto.buildconfig

data class BuildConfigurationCheckResultDto(
    val checkType: CheckType,
    val checkName: String,
    val actualValue: String,
    val expectedValue: String,
    val status: Boolean
)