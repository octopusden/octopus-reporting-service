package org.octopusden.octopus.reportingservice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.core.dto.ComponentV2
import org.octopusden.octopus.components.registry.core.dto.ComponentsDTO
import org.octopusden.octopus.reportingservice.client.common.exception.ExternalServiceException
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.component
import org.octopusden.octopus.reportingservice.service.impl.ComponentsRegistryServiceImpl

@DisplayName("ComponentsRegistryService")
class ComponentsRegistryServiceTest {

    private lateinit var client: ClassicComponentsRegistryServiceClient
    private lateinit var service: ComponentsRegistryServiceImpl

    @BeforeEach
    fun setUp() {
        client = mock()
        service = ComponentsRegistryServiceImpl(client = client)
    }

    private fun stubComponents(vararg components: ComponentV2) {
        whenever(
            client.getAllComponents(
                vcsPath = ArgumentMatchers.isNull(),
                buildSystem = ArgumentMatchers.isNull(),
                solution = ArgumentMatchers.isNull(),
                systems = any()
            )
        ).thenReturn(ComponentsDTO(components.toList()))
    }

    @Nested
    @DisplayName("getComponentsBySystems")
    inner class GetComponentsBySystems {

        @Test
        @DisplayName("Returns non-archived components")
        fun returnsNonArchivedComponents() {
            stubComponents(component("a"), component("b"))

            val actual = service.getComponentsBySystems(setOf("TEST_SYSTEM"))

            assertEquals(listOf(component("a"), component("b")), actual)
        }

        @Test
        @DisplayName("Filters out archived components")
        fun filtersOutArchivedComponents() {
            val archived = component("archived").also { it.archived = true }
            stubComponents(component("active"), archived)

            val actual = service.getComponentsBySystems(setOf("TEST_SYSTEM"))

            assertEquals(listOf(component("active")), actual)
        }

        @Test
        @DisplayName("Empty systems set -> empty result")
        fun emptySystemsReturnsEmpty() {
            stubComponents()

            val actual = service.getComponentsBySystems(emptySet())

            assertEquals(emptyList<ComponentV2>(), actual)
        }

        @Test
        @DisplayName("Client error is wrapped into ExternalServiceException")
        fun clientErrorWrappedAsExternal() {
            whenever(
                client.getAllComponents(
                    vcsPath = ArgumentMatchers.isNull(),
                    buildSystem = ArgumentMatchers.isNull(),
                    solution = ArgumentMatchers.isNull(),
                    systems = any()
                )
            ).thenThrow(RuntimeException("boom"))

            assertThrows(ExternalServiceException::class.java) {
                service.getComponentsBySystems(setOf("TEST_SYSTEM"))
            }
        }
    }
}