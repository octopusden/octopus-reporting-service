package org.octopusden.octopus.reportingservice.service

import org.octopusden.octopus.components.registry.core.dto.ComponentV2

interface ComponentsRegistryService {
    fun getComponentsBySystems(systems: Set<String>): List<ComponentV2>
}
