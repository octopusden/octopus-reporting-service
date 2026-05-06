package org.octopusden.octopus.reportingservice

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.octopusden.octopus.components.registry.core.dto.ComponentV2
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportChecksDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportComponentsFilterDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportRequestDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportResponseDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildStage
import org.octopusden.octopus.reportingservice.config.BuildConfigurationReportConfig
import org.octopusden.octopus.reportingservice.config.Templates
import org.octopusden.octopus.reportingservice.dto.BuildConfiguration
import org.octopusden.octopus.reportingservice.dto.BuildConfigurationParameter
import org.octopusden.octopus.reportingservice.dto.BuildConfigurationProject
import org.octopusden.octopus.reportingservice.dto.BuildConfigurationStep
import org.octopusden.octopus.reportingservice.service.ComponentsRegistryService
import org.octopusden.octopus.reportingservice.service.TeamCityService
import org.octopusden.octopus.reportingservice.service.impl.BuildConfigurationReportServiceImpl

/**
 * Unit-тесты отчёта о состояниях сборок.
 *
 * Моки `TeamCityService` и `ComponentsRegistryService` настраиваются прямо в
 * аргументах параметризованного теста. Ожидаемый ответ загружается из
 * JSON-файла в `server/src/test/resources/build-configuration-report/`.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BuildConfigurationReportServiceTest {

    private lateinit var teamCityService: TeamCityService
    private lateinit var componentsRegistryService: ComponentsRegistryService
    private lateinit var service: BuildConfigurationReportServiceImpl

    @BeforeEach
    fun setUp() {
        teamCityService = mock()
        componentsRegistryService = mock()
        service = BuildConfigurationReportServiceImpl(
            config = BuildConfigurationReportConfig(
                baseProjectId = BASE_PROJECT_ID,
                templates = Templates(
                    build = listOf(BUILD_TEMPLATE_ID),
                    releaseCandidate = listOf("RcTemplate"),
                    release = listOf("ReleaseTemplate")
                )
            ),
            teamCityService = teamCityService,
            componentsRegistryService = componentsRegistryService
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generateReportArguments")
    fun generateReportTest(
        @Suppress("UNUSED_PARAMETER") caseName: String,
        setup: (TeamCityService, ComponentsRegistryService) -> Unit,
        request: BuildConfigurationReportRequestDto,
        expectedResourceFile: String
    ) {
        setup(teamCityService, componentsRegistryService)
        val actual = service.generateReport(request)
        val expected = OBJECT_MAPPER.readValue(
            javaClass.classLoader.getResourceAsStream("$RESOURCES_ROOT/$expectedResourceFile")
                ?: error("Resource '$RESOURCES_ROOT/$expectedResourceFile' not found"),
            BuildConfigurationReportResponseDto::class.java
        )
        assertEquals(expected, actual)
    }

    companion object {
        private const val ROOT_PROJECT_ID = "RootProject"
        private const val BASE_PROJECT_ID = "RDDepartment"
        private const val BUILD_TEMPLATE_ID = "CDCompileUTGradle"
        private const val SYSTEM = "TEST_SYSTEM"
        private const val COMPONENT_A = "componentA"
        private const val COMPONENT_A_PROJECT_ID = "RootProject_ComponentA"
        private const val COMPONENT_A_PROJECT_URL = "http://tc/RootProject_ComponentA"
        private const val COMPONENT_A_BUILD_ID = "RootProject_ComponentA_Build"
        private const val RESOURCES_ROOT = "build-configuration-report"

        private val OBJECT_MAPPER: ObjectMapper = ObjectMapper().registerKotlinModule()

        private fun setupMocks(
            components: List<ComponentV2> = emptyList(),
            template: BuildConfiguration? = null,
            projects: List<BuildConfigurationProject> = emptyList()
        ): (TeamCityService, ComponentsRegistryService) -> Unit =
            { tc, cr ->
                whenever(cr.getComponentsBySystems(any())).thenReturn(components)
                whenever(tc.findSubprojects(any())).thenReturn(projects)
                if (template != null) {
                    whenever(tc.getTemplateByProjectIdAndTemplateId(any(), any())).thenReturn(template)
                }
            }

        private fun request(
            parameters: List<String> = emptyList(),
            steps: List<String> = emptyList(),
            excludeComponents: Set<String> = emptySet()
        ) = BuildConfigurationReportRequestDto(
            rootProjectId = ROOT_PROJECT_ID,
            componentsFilter = BuildConfigurationReportComponentsFilterDto(
                includeSystems = setOf(SYSTEM),
                excludeComponents = excludeComponents
            ),
            checks = BuildConfigurationReportChecksDto(
                buildStage = BuildStage.BUILD,
                parameters = parameters,
                steps = steps
            )
        )

        @JvmStatic
        @Suppress("unused")
        private fun generateReportArguments(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "parameterMatches",
                setupMocks(
                    components = listOf(ComponentV2(id = COMPONENT_A, name = COMPONENT_A, componentOwner = "owner")),
                    template = BuildConfiguration(
                        buildTypeId = BUILD_TEMPLATE_ID,
                        parameters = listOf(BuildConfigurationParameter("XRAY", "true"))
                    ),
                    projects = listOf(
                        BuildConfigurationProject(
                            id = COMPONENT_A_PROJECT_ID,
                            name = COMPONENT_A_PROJECT_ID,
                            webUrl = COMPONENT_A_PROJECT_URL,
                            componentId = COMPONENT_A,
                            buildConfigurations = setOf(
                                BuildConfiguration(
                                    buildTypeId = COMPONENT_A_BUILD_ID,
                                    templateIds = setOf(BUILD_TEMPLATE_ID),
                                    parameters = listOf(BuildConfigurationParameter("XRAY", "true"))
                                )
                            )
                        )
                    )
                ),
                request(parameters = listOf("XRAY")),
                "parameterMatches.json"
            ),
            Arguments.of(
                "parameterMismatch",
                setupMocks(
                    components = listOf(ComponentV2(id = COMPONENT_A, name = COMPONENT_A, componentOwner = "owner")),
                    template = BuildConfiguration(
                        buildTypeId = BUILD_TEMPLATE_ID,
                        parameters = listOf(BuildConfigurationParameter("XRAY", "true"))
                    ),
                    projects = listOf(
                        BuildConfigurationProject(
                            id = COMPONENT_A_PROJECT_ID,
                            name = COMPONENT_A_PROJECT_ID,
                            webUrl = COMPONENT_A_PROJECT_URL,
                            componentId = COMPONENT_A,
                            buildConfigurations = setOf(
                                BuildConfiguration(
                                    buildTypeId = COMPONENT_A_BUILD_ID,
                                    templateIds = setOf(BUILD_TEMPLATE_ID),
                                    parameters = listOf(BuildConfigurationParameter("XRAY", "false"))
                                )
                            )
                        )
                    )
                ),
                request(parameters = listOf("XRAY")),
                "parameterMismatch.json"
            ),
            Arguments.of(
                "parameterNotDefined",
                setupMocks(
                    components = listOf(ComponentV2(id = COMPONENT_A, name = COMPONENT_A, componentOwner = "owner")),
                    template = BuildConfiguration(buildTypeId = BUILD_TEMPLATE_ID),
                    projects = listOf(
                        BuildConfigurationProject(
                            id = COMPONENT_A_PROJECT_ID,
                            name = COMPONENT_A_PROJECT_ID,
                            webUrl = COMPONENT_A_PROJECT_URL,
                            componentId = COMPONENT_A,
                            buildConfigurations = setOf(
                                BuildConfiguration(
                                    buildTypeId = COMPONENT_A_BUILD_ID,
                                    templateIds = setOf(BUILD_TEMPLATE_ID)
                                )
                            )
                        )
                    )
                ),
                request(parameters = listOf("XRAY")),
                "parameterNotDefined.json"
            ),
            Arguments.of(
                "stepDisabledMismatch",
                setupMocks(
                    components = listOf(ComponentV2(id = COMPONENT_A, name = COMPONENT_A, componentOwner = "owner")),
                    template = BuildConfiguration(
                        buildTypeId = BUILD_TEMPLATE_ID,
                        steps = listOf(BuildConfigurationStep("s1", "Compile", disabled = false))
                    ),
                    projects = listOf(
                        BuildConfigurationProject(
                            id = COMPONENT_A_PROJECT_ID,
                            name = COMPONENT_A_PROJECT_ID,
                            webUrl = COMPONENT_A_PROJECT_URL,
                            componentId = COMPONENT_A,
                            buildConfigurations = setOf(
                                BuildConfiguration(
                                    buildTypeId = COMPONENT_A_BUILD_ID,
                                    templateIds = setOf(BUILD_TEMPLATE_ID),
                                    steps = listOf(BuildConfigurationStep("s1", "Compile", disabled = true))
                                )
                            )
                        )
                    )
                ),
                request(steps = listOf("Compile")),
                "stepDisabledMismatch.json"
            ),
            Arguments.of(
                "componentWithoutProject",
                setupMocks(
                    components = listOf(
                        ComponentV2(id = "orphanComponent", name = "orphanComponent", componentOwner = "owner")
                    ),
                    template = BuildConfiguration(buildTypeId = BUILD_TEMPLATE_ID),
                    projects = emptyList()
                ),
                request(parameters = listOf("XRAY")),
                "componentWithoutProject.json"
            ),
            Arguments.of(
                "projectWithoutMatchingBuild",
                setupMocks(
                    components = listOf(ComponentV2(id = COMPONENT_A, name = COMPONENT_A, componentOwner = "owner")),
                    template = BuildConfiguration(buildTypeId = BUILD_TEMPLATE_ID),
                    projects = listOf(
                        BuildConfigurationProject(
                            id = COMPONENT_A_PROJECT_ID,
                            name = COMPONENT_A_PROJECT_ID,
                            webUrl = COMPONENT_A_PROJECT_URL,
                            componentId = COMPONENT_A,
                            buildConfigurations = setOf(
                                BuildConfiguration(
                                    buildTypeId = "${COMPONENT_A_PROJECT_ID}_OtherBuild",
                                    templateIds = setOf("SomeOtherTemplate")
                                )
                            )
                        )
                    )
                ),
                request(parameters = listOf("XRAY")),
                "projectWithoutMatchingBuild.json"
            ),
            Arguments.of(
                "emptyChecks",
                setupMocks(),
                request(),
                "emptyChecks.json"
            ),
            Arguments.of(
                "excludeComponents",
                setupMocks(
                    components = listOf(
                        ComponentV2(id = COMPONENT_A, name = COMPONENT_A, componentOwner = "owner"),
                        ComponentV2(id = "skipped", name = "skipped", componentOwner = "owner")
                    ),
                    template = BuildConfiguration(buildTypeId = BUILD_TEMPLATE_ID),
                    projects = emptyList()
                ),
                request(parameters = listOf("XRAY"), excludeComponents = setOf("skipped")),
                "excludeComponents.json"
            ),
            Arguments.of(
                "multipleComponentsSorted",
                setupMocks(
                    components = listOf(
                        ComponentV2(id = "Bravo", name = "Bravo", componentOwner = "owner"),
                        ComponentV2(id = "alpha", name = "alpha", componentOwner = "owner"),
                        ComponentV2(id = "charlie", name = "charlie", componentOwner = "owner")
                    ),
                    template = BuildConfiguration(buildTypeId = BUILD_TEMPLATE_ID),
                    projects = emptyList()
                ),
                request(parameters = listOf("XRAY")),
                "multipleComponentsSorted.json"
            )
        )
    }
}