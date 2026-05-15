package org.octopusden.octopus.reportingservice.domain

data class BuildConfigurationProject(
    val id: String,
    val name: String,
    val webUrl: String,
    val componentId: String,
    val buildConfigurations: Set<BuildConfiguration>
)

data class BuildConfiguration(
    val buildTypeId: String,
    val templateIds: Set<String> = emptySet(),
    val parameters: List<BuildConfigurationParameter> = emptyList(),
    val steps: List<BuildConfigurationStep> = emptyList()
)

data class BuildConfigurationParameter(
    val name: String,
    val value: String
)

data class BuildConfigurationStep(
    val id: String,
    val name: String,
    val disabled: Boolean
)