package org.octopusden.octopus.reportingservice.config

import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClientUrlProvider
import org.octopusden.octopus.infrastructure.client.commons.ClientParametersProvider
import org.octopusden.octopus.infrastructure.client.commons.StandardBasicCredCredentialProvider
import org.octopusden.octopus.infrastructure.teamcity.client.TeamcityClassicClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ClientsConfig {

    @Bean
    fun componentsRegistryClient(
        @Value($$"${components-registry-service.url}") url: String
    ): ClassicComponentsRegistryServiceClient =
        ClassicComponentsRegistryServiceClient(
            object : ClassicComponentsRegistryServiceClientUrlProvider {
                override fun getApiUrl() = url
            }
        )

    @Bean
    fun teamCityClient(teamCityConfig: TeamCityConfig): TeamcityClassicClient =
        TeamcityClassicClient(
            object : ClientParametersProvider {
                override fun getApiUrl(): String = teamCityConfig.url
                override fun getAuth() =
                    StandardBasicCredCredentialProvider(teamCityConfig.user, teamCityConfig.password)
            }
        )
}