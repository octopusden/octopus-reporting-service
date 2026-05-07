package org.octopusden.octopus.reportingservice.service.impl

import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.core.dto.ComponentV2
import org.octopusden.octopus.components.registry.core.exceptions.NotFoundException as RegistryNotFoundException
import org.octopusden.octopus.reportingservice.client.common.exception.ExternalServiceException
import org.octopusden.octopus.reportingservice.service.ComponentsRegistryService
import org.springframework.stereotype.Service

@Service
class ComponentsRegistryServiceImpl(
    private val client: ClassicComponentsRegistryServiceClient
) : ComponentsRegistryService {

    override fun getComponentsBySystems(systems: Set<String>): List<ComponentV2> = try {
        client.getAllComponents(systems = systems.toList()).components.filter { !it.archived }
    } catch (e: Exception) {
        throw ExternalServiceException("Components Registry call failed: getAllComponents(systems=$systems)", e)
    }
}