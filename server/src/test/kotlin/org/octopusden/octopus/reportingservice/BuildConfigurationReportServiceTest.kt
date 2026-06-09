package org.octopusden.octopus.reportingservice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.octopusden.octopus.components.registry.core.dto.ComponentV2
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationConstants
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.CheckType
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.ComponentReportStatus
import org.octopusden.octopus.reportingservice.config.BuildConfigurationReportConfig
import org.octopusden.octopus.reportingservice.config.Templates
import org.octopusden.octopus.reportingservice.domain.BuildConfiguration
import org.octopusden.octopus.reportingservice.domain.BuildConfigurationProject
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.BASE_PROJECT_ID
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.BUILD_TEMPLATE_ID
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.COMPONENT_A
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.COMPONENT_A_BUILD_ID
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.COMPONENT_A_PROJECT_ID
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.COMPONENT_A_PROJECT_URL
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.ROOT_PROJECT_ID
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.build
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.checkResult
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.component
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.componentReport
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.param
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.project
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.request
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.response
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.step
import org.octopusden.octopus.reportingservice.service.ComponentsRegistryService
import org.octopusden.octopus.reportingservice.service.TeamCityService
import org.octopusden.octopus.reportingservice.service.impl.BuildConfigurationReportServiceImpl

@DisplayName("BuildConfigurationReportService")
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

    private fun stubMocks(
        components: List<ComponentV2> = emptyList(),
        template: BuildConfiguration? = null,
        projects: List<BuildConfigurationProject> = emptyList()
    ) {
        whenever(componentsRegistryService.getComponentsBySystems(any())).thenReturn(components)
        whenever(teamCityService.findSubprojects(any())).thenReturn(projects)
        if (template != null) {
            whenever(teamCityService.getTemplateByProjectIdAndTemplateId(any(), any())).thenReturn(template)
        }
    }

    @Nested
    @DisplayName("Parameter checks")
    inner class ParameterChecks {

        @Test
        @DisplayName("Parameters match")
        fun parameterMatches() {
            stubMocks(
                components = listOf(component(COMPONENT_A)),
                template = build(BUILD_TEMPLATE_ID, parameters = listOf(param("XRAY", "true"))),
                projects = listOf(
                    project(
                        COMPONENT_A_PROJECT_ID, COMPONENT_A,
                        webUrl = COMPONENT_A_PROJECT_URL,
                        buildConfigurations = setOf(
                            build(
                                COMPONENT_A_BUILD_ID,
                                templateIds = setOf(BUILD_TEMPLATE_ID),
                                parameters = listOf(param("XRAY", "true"))
                            )
                        )
                    )
                )
            )

            val actual = service.generateReport(request(parameters = listOf("XRAY")))

            assertEquals(
                response(
                    result = listOf(
                        componentReport(
                            componentId = COMPONENT_A,
                            status = ComponentReportStatus.OK,
                            buildConfigurationUrl = COMPONENT_A_PROJECT_URL,
                            buildTypeId = COMPONENT_A_BUILD_ID,
                            checks = listOf(checkResult(CheckType.PARAMETER, "XRAY", "true", "true"))
                        )
                    )
                ),
                actual
            )
        }

        @Test
        @DisplayName("Parameters mismatch")
        fun parameterMismatch() {
            stubMocks(
                components = listOf(component(COMPONENT_A)),
                template = build(BUILD_TEMPLATE_ID, parameters = listOf(param("XRAY", "true"))),
                projects = listOf(
                    project(
                        COMPONENT_A_PROJECT_ID, COMPONENT_A,
                        webUrl = COMPONENT_A_PROJECT_URL,
                        buildConfigurations = setOf(
                            build(
                                COMPONENT_A_BUILD_ID,
                                templateIds = setOf(BUILD_TEMPLATE_ID),
                                parameters = listOf(param("XRAY", "false"))
                            )
                        )
                    )
                )
            )

            val actual = service.generateReport(request(parameters = listOf("XRAY")))

            assertEquals(
                response(
                    result = listOf(
                        componentReport(
                            componentId = COMPONENT_A,
                            status = ComponentReportStatus.OK,
                            buildConfigurationUrl = COMPONENT_A_PROJECT_URL,
                            buildTypeId = COMPONENT_A_BUILD_ID,
                            checks = listOf(checkResult(CheckType.PARAMETER, "XRAY", "false", "true"))
                        )
                    )
                ),
                actual
            )
        }

        @Test
        @DisplayName("Both parameters not defined")
        fun parameterNotDefined() {
            stubMocks(
                components = listOf(component(COMPONENT_A)),
                template = build(BUILD_TEMPLATE_ID),
                projects = listOf(
                    project(
                        COMPONENT_A_PROJECT_ID, COMPONENT_A,
                        webUrl = COMPONENT_A_PROJECT_URL,
                        buildConfigurations = setOf(build(COMPONENT_A_BUILD_ID, templateIds = setOf(BUILD_TEMPLATE_ID)))
                    )
                )
            )

            val actual = service.generateReport(request(parameters = listOf("XRAY")))

            assertEquals(
                response(
                    result = listOf(
                        componentReport(
                            componentId = COMPONENT_A,
                            status = ComponentReportStatus.OK,
                            buildConfigurationUrl = COMPONENT_A_PROJECT_URL,
                            buildTypeId = COMPONENT_A_BUILD_ID,
                            checks = listOf(checkResult(CheckType.PARAMETER, "XRAY", BuildConfigurationConstants.NOT_DEFINED, BuildConfigurationConstants.NOT_DEFINED, status = false))
                        )
                    )
                ),
                actual
            )
        }
    }

    @Nested
    @DisplayName("Step checks")
    inner class StepChecks {

        @Test
        @DisplayName("Steps match")
        fun stepsMatch() {
            stubMocks(
                components = listOf(component(COMPONENT_A)),
                template = build(BUILD_TEMPLATE_ID, steps = listOf(step("Compile", disabled = false))),
                projects = listOf(
                    project(
                        COMPONENT_A_PROJECT_ID, COMPONENT_A,
                        webUrl = COMPONENT_A_PROJECT_URL,
                        buildConfigurations = setOf(
                            build(
                                COMPONENT_A_BUILD_ID,
                                templateIds = setOf(BUILD_TEMPLATE_ID),
                                steps = listOf(step("Compile", disabled = false))
                            )
                        )
                    )
                )
            )

            val actual = service.generateReport(request(steps = listOf("Compile")))

            assertEquals(
                response(
                    result = listOf(
                        componentReport(
                            componentId = COMPONENT_A,
                            status = ComponentReportStatus.OK,
                            buildConfigurationUrl = COMPONENT_A_PROJECT_URL,
                            buildTypeId = COMPONENT_A_BUILD_ID,
                            checks = listOf(checkResult(CheckType.STEP, "Compile", "ENABLED", "ENABLED"))
                        )
                    )
                ),
                actual
            )
        }

        @Test
        @DisplayName("Steps mismatch")
        fun stepsMismatch() {
            stubMocks(
                components = listOf(component(COMPONENT_A)),
                template = build(BUILD_TEMPLATE_ID, steps = listOf(step("Compile", disabled = false))),
                projects = listOf(
                    project(
                        COMPONENT_A_PROJECT_ID, COMPONENT_A,
                        webUrl = COMPONENT_A_PROJECT_URL,
                        buildConfigurations = setOf(
                            build(
                                COMPONENT_A_BUILD_ID,
                                templateIds = setOf(BUILD_TEMPLATE_ID),
                                steps = listOf(step("Compile", disabled = true))
                            )
                        )
                    )
                )
            )

            val actual = service.generateReport(request(steps = listOf("Compile")))

            assertEquals(
                response(
                    result = listOf(
                        componentReport(
                            componentId = COMPONENT_A,
                            status = ComponentReportStatus.OK,
                            buildConfigurationUrl = COMPONENT_A_PROJECT_URL,
                            buildTypeId = COMPONENT_A_BUILD_ID,
                            checks = listOf(checkResult(CheckType.STEP, "Compile", "DISABLED", "ENABLED"))
                        )
                    )
                ),
                actual
            )
        }
    }

    @Nested
    @DisplayName("Component status")
    inner class ComponentStatuses {

        @Test
        @DisplayName("Component without TC project")
        fun componentWithoutProject() {
            stubMocks(
                components = listOf(component("orphanComponent")),
                template = build(BUILD_TEMPLATE_ID),
                projects = emptyList()
            )

            val actual = service.generateReport(request(parameters = listOf("XRAY")))
            assertEquals(
                response(
                    result = listOf(
                        componentReport(
                            componentId = "orphanComponent",
                            status = ComponentReportStatus.NO_PROJECT
                        )
                    )
                ),
                actual
            )
        }

        @Test
        @DisplayName("Component without matching build configuration")
        fun projectWithoutMatchingBuild() {
            stubMocks(
                components = listOf(component(COMPONENT_A)),
                template = build(BUILD_TEMPLATE_ID),
                projects = listOf(
                    project(
                        COMPONENT_A_PROJECT_ID, COMPONENT_A,
                        webUrl = COMPONENT_A_PROJECT_URL,
                        buildConfigurations = setOf(
                            build("${COMPONENT_A_PROJECT_ID}_OtherBuild", templateIds = setOf("SomeOtherTemplate"))
                        )
                    )
                )
            )

            val actual = service.generateReport(request(parameters = listOf("XRAY")))

            assertEquals(
                response(
                    result = listOf(
                        componentReport(
                            componentId = COMPONENT_A,
                            status = ComponentReportStatus.NO_BUILD_CONFIGURATION,
                            buildConfigurationUrl = COMPONENT_A_PROJECT_URL
                        )
                    )
                ),
                actual
            )
        }
    }

    @Nested
    @DisplayName("Filtering and sorting")
    inner class Filtering {

        @Test
        @DisplayName("excludeComponents filtering")
        fun excludeComponents() {
            stubMocks(
                components = listOf(component(COMPONENT_A), component("skipped")),
                template = build(BUILD_TEMPLATE_ID),
                projects = emptyList()
            )

            val actual = service.generateReport(
                request(parameters = listOf("XRAY"), excludeComponents = setOf("skipped"))
            )

            assertEquals(
                response(
                    result = listOf(
                        componentReport(componentId = COMPONENT_A, status = ComponentReportStatus.NO_PROJECT)
                    )
                ),
                actual
            )
        }

        @Test
        @DisplayName("Sorting by componentId")
        fun multipleComponentsSorted() {
            stubMocks(
                components = listOf(
                    component("Betta"),
                    component("alpha"),
                    component("gamma")
                ),
                template = build(BUILD_TEMPLATE_ID),
                projects = emptyList()
            )

            val actual = service.generateReport(request(parameters = listOf("XRAY")))

            assertEquals(
                response(
                    result = listOf(
                        componentReport(componentId = "alpha", status = ComponentReportStatus.NO_PROJECT),
                        componentReport(componentId = "Betta", status = ComponentReportStatus.NO_PROJECT),
                        componentReport(componentId = "gamma", status = ComponentReportStatus.NO_PROJECT)
                    )
                ),
                actual
            )
        }
    }

    @Nested
    @DisplayName("Empty inputs")
    inner class EmptyInputs {

        @Test
        @DisplayName("Empty checks")
        fun emptyChecks() {
            val actual = service.generateReport(request())
            assertEquals(response(rootProjectId = ROOT_PROJECT_ID, result = emptyList()), actual)
        }

        @Test
        @DisplayName("No components after filter")
        fun noComponentsAfterFilter() {
            stubMocks(components = emptyList(), template = build(BUILD_TEMPLATE_ID))
            val actual = service.generateReport(request(parameters = listOf("XRAY")))
            assertEquals(response(rootProjectId = ROOT_PROJECT_ID, result = emptyList()), actual)
        }
    }
}