package org.octopusden.octopus.reportingservice

import com.fasterxml.jackson.core.type.TypeReference
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.octopusden.octopus.components.registry.client.impl.ClassicComponentsRegistryServiceClient
import org.octopusden.octopus.components.registry.core.dto.ComponentV2
import org.octopusden.octopus.components.registry.core.dto.ComponentsDTO
import org.octopusden.octopus.reportingservice.service.impl.ComponentsRegistryServiceImpl
import org.octopusden.octopus.reportingservice.util.TestUtils

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComponentsRegistryServiceTest {

    private lateinit var client: ClassicComponentsRegistryServiceClient
    private lateinit var service: ComponentsRegistryServiceImpl

    @BeforeEach
    fun setUp() {
        client = mock()
        service = ComponentsRegistryServiceImpl(client = client)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getComponentsBySystemsArguments")
    fun getComponentsBySystemsTest(
        @Suppress("UNUSED_PARAMETER") caseName: String,
        setup: (ClassicComponentsRegistryServiceClient) -> Unit,
        systems: Set<String>,
        expectedResourceFile: String
    ) {
        setup(client)
        val actual = service.getComponentsBySystems(systems)
        val expected = TestUtils.loadObject(
            "$RESOURCES_ROOT/$expectedResourceFile",
            object : TypeReference<List<ComponentV2>>() {}
        )
        assertEquals(expected, actual)
    }

    companion object {
        private const val RESOURCES_ROOT = "components-registry-service"

        private fun setupMocks(
            components: List<ComponentV2> = emptyList()
        ): (ClassicComponentsRegistryServiceClient) -> Unit =
            { c ->
                whenever(
                    c.getAllComponents(
                        vcsPath = ArgumentMatchers.isNull(),
                        buildSystem = ArgumentMatchers.isNull(),
                        solution = ArgumentMatchers.isNull(),
                        systems = any()
                    )
                ).thenReturn(ComponentsDTO(components))
            }

        @JvmStatic
        @Suppress("unused")
        private fun getComponentsBySystemsArguments(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "twoComponentsForNonEmptySystems",
                setupMocks(
                    components = listOf(
                        ComponentV2(id = "a", name = "A", componentOwner = "owner"),
                        ComponentV2(id = "b", name = "B", componentOwner = "owner")
                    )
                ),
                setOf("TEST_SYSTEM"),
                "twoComponents.json"
            ),
            Arguments.of(
                "emptyComponentsForEmptySystems",
                setupMocks(components = emptyList()),
                emptySet<String>(),
                "emptyComponents.json"
            )
        )
    }
}