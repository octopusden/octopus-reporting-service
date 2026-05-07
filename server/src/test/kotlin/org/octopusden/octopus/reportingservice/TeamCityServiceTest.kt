package org.octopusden.octopus.reportingservice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.octopusden.octopus.infrastructure.teamcity.client.TeamcityClient
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityBuildTypes
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityProjects
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityProperties
import org.octopusden.octopus.infrastructure.teamcity.client.dto.locator.ProjectLocator
import org.octopusden.octopus.reportingservice.client.common.exception.ExternalServiceException
import org.octopusden.octopus.reportingservice.client.common.exception.NotFoundException
import org.octopusden.octopus.reportingservice.domain.BuildConfigurationParameter
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.build
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.project
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.step
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.tcBuildType
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.tcProject
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.tcProjectsPage
import org.octopusden.octopus.reportingservice.fixtures.Fixtures.tcStep
import org.octopusden.octopus.reportingservice.service.impl.TeamCityServiceImpl

@DisplayName("TeamCityService")
class TeamCityServiceTest {

    private lateinit var client: TeamcityClient
    private lateinit var service: TeamCityServiceImpl

    @BeforeEach
    fun setUp() {
        client = mock()
        service = TeamCityServiceImpl(client = client)
    }

    private fun stubRoot(rootProjectId: String, vararg rootProjects: org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityProject) {
        whenever(client.getProjectsWithLocatorAndFields(locatorWithId(rootProjectId), any()))
            .thenReturn(TeamcityProjects(projects = rootProjects.toList()))
    }

    private fun stubChildren(rootProjectId: String, vararg pages: TeamcityProjects) {
        var stub = whenever(client.getProjectsWithLocatorAndFields(locatorWithAffectedProject(rootProjectId), any()))
        if (pages.isEmpty()) {
            stub.thenReturn(TeamcityProjects(nextHref = null, projects = emptyList()))
        } else {
            pages.forEach { page -> stub = stub.thenReturn(page) }
        }
    }

    private fun stubProjectAbsent(projectId: String) {
        whenever(client.getProjectsWithLocatorAndFields(locatorWithId(projectId), any()))
            .thenReturn(TeamcityProjects(projects = emptyList()))
    }

    @Nested
    @DisplayName("findSubprojects")
    inner class FindSubprojects {

        @Test
        @DisplayName("Maps TeamCity project to domain BuildConfigurationProject")
        fun singleRootProject() {
            stubRoot(
                "RootProject_ComponentA",
                tcProject(
                    id = "RootProject_ComponentA",
                    componentName = "componentA",
                    buildTypes = listOf(
                        tcBuildType(
                            id = "RootProject_ComponentA_Build",
                            templateIds = setOf("CDCompileUTGradle"),
                            parameters = mapOf("P1" to "v1"),
                            steps = listOf(tcStep("Compile", disabled = false, id = "s1"))
                        )
                    )
                )
            )
            stubChildren("RootProject_ComponentA")

            val actual = service.findSubprojects("RootProject_ComponentA")

            assertEquals(
                listOf(
                    project(
                        id = "RootProject_ComponentA",
                        componentId = "componentA",
                        webUrl = "http://tc/RootProject_ComponentA",
                        buildConfigurations = setOf(
                            build(
                                buildTypeId = "RootProject_ComponentA_Build",
                                templateIds = setOf("CDCompileUTGradle"),
                                parameters = listOf(
                                    BuildConfigurationParameter("P1", "v1")
                                ),
                                steps = listOf(step("Compile", disabled = false, id = "s1"))
                            )
                        )
                    )
                ),
                actual
            )
        }

        @Test
        @DisplayName("Skips projects without buildTypes")
        fun skipProjectsWithoutBuildTypes() {
            stubRoot(
                "RootProject",
                tcProject(id = "RootProject_NoBuilds", componentName = "no", buildTypes = emptyList()),
                tcProject(
                    id = "RootProject_Ok",
                    componentName = "ok",
                    buildTypes = listOf(tcBuildType(id = "RootProject_Ok_Build"))
                )
            )
            stubChildren("RootProject")

            val actual = service.findSubprojects("RootProject")

            assertEquals(
                listOf(
                    project(
                        id = "RootProject_Ok",
                        componentId = "ok",
                        webUrl = "http://tc/RootProject_Ok",
                        buildConfigurations = setOf(build(buildTypeId = "RootProject_Ok_Build"))
                    )
                ),
                actual
            )
        }

        @Test
        @DisplayName("Skips projects without COMPONENT_NAME parameter")
        fun skipProjectsWithoutComponentName() {
            stubRoot(
                "RootProject",
                tcProject(
                    id = "RootProject_X",
                    componentName = null,
                    buildTypes = listOf(tcBuildType(id = "b"))
                )
            )
            stubChildren("RootProject")

            val actual = service.findSubprojects("RootProject")

            assertEquals(emptyList<Any>(), actual)
        }

        @Test
        @DisplayName("Walks through children pages while nextHref is not null")
        fun paginatedChildren() {
            stubRoot(
                "RootProject",
                tcProject(
                    id = "RootProject",
                    componentName = "s-root",
                    buildTypes = listOf(tcBuildType(id = "RootProject_Build"))
                )
            )
            stubChildren(
                "RootProject",
                tcProjectsPage(
                    nextHref = "/next",
                    projects = listOf(
                        tcProject(
                            id = "RootProject_A",
                            componentName = "a",
                            buildTypes = listOf(tcBuildType(id = "RootProject_A_Build"))
                        )
                    )
                ),
                tcProjectsPage(
                    nextHref = null,
                    projects = listOf(
                        tcProject(
                            id = "RootProject_B",
                            componentName = "b",
                            buildTypes = listOf(tcBuildType(id = "RootProject_B_Build"))
                        )
                    )
                )
            )

            val actual = service.findSubprojects("RootProject")

            assertEquals(
                listOf(
                    project(id = "RootProject", componentId = "s-root",
                        buildConfigurations = setOf(build("RootProject_Build"))),
                    project(id = "RootProject_A", componentId = "a",
                        buildConfigurations = setOf(build("RootProject_A_Build"))),
                    project(id = "RootProject_B", componentId = "b",
                        buildConfigurations = setOf(build("RootProject_B_Build")))
                ),
                actual
            )
        }

        @Test
        @DisplayName("Step without explicit 'disabled' flag is treated as disabled (current behaviour)")
        fun stepWithoutDisabledIsTreatedAsDisabled() {
            stubRoot(
                "Root",
                tcProject(
                    id = "Root",
                    componentName = "comp",
                    buildTypes = listOf(
                        tcBuildType(
                            id = "Build",
                            steps = listOf(
                                org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityStep(
                                    id = "s1",
                                    name = "Compile",
                                    type = "custom",
                                    disabled = null,
                                    properties = TeamcityProperties()
                                )
                            )
                        )
                    )
                )
            )
            stubChildren("Root")

            val actual = service.findSubprojects("Root")

            val onlyStep = actual.single().buildConfigurations.single().steps.single()
            assertEquals(true, onlyStep.disabled)
        }

        @Test
        @DisplayName("TeamCity client error is wrapped into ExternalServiceException")
        fun clientErrorWrappedAsExternal() {
            whenever(client.getProjectsWithLocatorAndFields(locatorWithId("Root"), any()))
                .thenThrow(RuntimeException("boom"))
            assertThrows(ExternalServiceException::class.java) {
                service.findSubprojects("Root")
            }
        }
    }

    @Nested
    @DisplayName("getTemplateByProjectIdAndTemplateId")
    inner class GetTemplate {

        @Test
        @DisplayName("Maps TeamCity template to domain BuildConfiguration")
        fun gradleTemplate() {
            val project = tcProject(id = "RDDepartment").copy(
                templates = TeamcityBuildTypes(
                    buildTypes = listOf(
                        tcBuildType(
                            id = "CDCompileUTGradle",
                            parameters = mapOf("XRAY" to "true"),
                            steps = listOf(tcStep("Compile", disabled = false, id = "s1"))
                        )
                    )
                )
            )
            whenever(client.getProjectsWithLocatorAndFields(locatorWithId("RDDepartment"), any()))
                .thenReturn(TeamcityProjects(projects = listOf(project)))

            val actual = service.getTemplateByProjectIdAndTemplateId("RDDepartment", "CDCompileUTGradle")

            assertEquals(
                build(
                    buildTypeId = "CDCompileUTGradle",
                    parameters = listOf(
                        BuildConfigurationParameter("XRAY", "true")
                    ),
                    steps = listOf(step("Compile", disabled = false, id = "s1"))
                ),
                actual
            )
        }

        @Test
        @DisplayName("Template id not found -> NotFoundException")
        fun templateIdAbsent() {
            val project = tcProject(id = "RDDepartment").copy(
                templates = TeamcityBuildTypes(
                    buildTypes = listOf(tcBuildType(id = "SomeOtherTemplate"))
                )
            )
            whenever(client.getProjectsWithLocatorAndFields(locatorWithId("RDDepartment"), any()))
                .thenReturn(TeamcityProjects(projects = listOf(project)))

            assertThrows(NotFoundException::class.java) {
                service.getTemplateByProjectIdAndTemplateId("RDDepartment", "Missing")
            }
        }

        @Test
        @DisplayName("Project not found -> NotFoundException")
        fun projectAbsent() {
            stubProjectAbsent("RDDepartment")
            assertThrows(NotFoundException::class.java) {
                service.getTemplateByProjectIdAndTemplateId("RDDepartment", "Any")
            }
        }
    }

    private fun locatorWithId(expectedId: String): ProjectLocator =
        argThat { this.id == expectedId && this.affectedProject == null }

    private fun locatorWithAffectedProject(expectedAffectedId: String): ProjectLocator =
        argThat { this.affectedProject?.id == expectedAffectedId }
}