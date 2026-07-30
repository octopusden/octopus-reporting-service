package org.octopusden.octopus.reportingservice.service.impl

import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClientUrlProvider
import org.octopusden.octopus.components.registry.core.dto.ComponentV2
import org.octopusden.octopus.reportingservice.client.common.exception.ExternalServiceException
import org.octopusden.octopus.reportingservice.service.ComponentsRegistryService
import org.springframework.stereotype.Service

@Service
class ComponentsRegistryServiceImpl(
    private val client: ClassicComponentsRegistryServiceClient,
    private val urlProvider: ClassicComponentsRegistryServiceClientUrlProvider,
) : ComponentsRegistryService {
    override fun getComponentsBySystems(systems: Set<String>): List<ComponentV2> =
        try {
            client.getAllComponents(systems = systems.toList()).components.filter { !it.archived }
        } catch (e: Exception) {
            throw ExternalServiceException("Components Registry call failed: getAllComponents(systems=$systems)", e)
        }

    override fun getComponentUrl(componentId: String): String =
        "${urlProvider.getApiUrl()}/$COMPONENT_PATH/$componentId"

    companion object {
        const val COMPONENT_PATH = "rest/api/2/components"
    }
}
