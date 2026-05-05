package org.octopusden.octopus.reportingservice.service.impl

import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClientUrlProvider
import org.octopusden.octopus.components.registry.core.dto.ComponentV2
import org.octopusden.octopus.reportingservice.service.ComponentsRegistryService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ComponentsRegistryServiceImpl(
    @param:Value($$"${components-registry-service.url}")
    private val componentsRegistryServiceUrl: String
) : ComponentsRegistryService {

    private val client by lazy {
        ClassicComponentsRegistryServiceClient(
            object : ClassicComponentsRegistryServiceClientUrlProvider {
                override fun getApiUrl() = componentsRegistryServiceUrl
            }
        )
    }

    override fun getComponentsBySystems(systems: Set<String>): List<ComponentV2> {
        return client.getAllComponents(systems = systems.toList()).components.toList()
    }
}