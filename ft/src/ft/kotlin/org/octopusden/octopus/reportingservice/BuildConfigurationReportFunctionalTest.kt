package org.octopusden.octopus.reportingservice

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockserver.client.MockServerClient
import org.mockserver.integration.ClientAndServer
import org.octopusden.octopus.reportingservice.client.ReportingServiceClient
import org.octopusden.octopus.reportingservice.client.ReportingServiceClientConfig
import org.octopusden.octopus.reportingservice.client.ReportingServiceClientFactory
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportChecksDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportComponentsFilterDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportRequestDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildStage
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.ComponentReportStatus

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BuildConfigurationReportFunctionalTest {

    @BeforeEach
    fun resetMocks() {
        teamCity.reset()
    }

    @Test
    fun emptyReportTest() {
        val response = client.generateBuildConfigurationReport(
            BuildConfigurationReportRequestDto(
                rootProjectId = ROOT_PROJECT_ID,
                componentsFilter = BuildConfigurationReportComponentsFilterDto(
                    includeSystems = setOf(SYSTEM)
                ),
                checks = BuildConfigurationReportChecksDto(buildStage = BuildStage.BUILD)
            )
        )
        assertEquals(ROOT_PROJECT_ID, response.rootProjectId)
        assertTrue(response.result.isEmpty())
    }

    @Test
    fun happyPathReportTest() {
        teamCity.stubRootProjects(ROOT_PROJECT_ID, "teamcity-mock-data/projects.json")
        teamCity.stubChildrenEmpty(ROOT_PROJECT_ID)
        teamCity.stubTemplate("teamcity-mock-data/template.json")

        val response = client.generateBuildConfigurationReport(
            BuildConfigurationReportRequestDto(
                rootProjectId = ROOT_PROJECT_ID,
                componentsFilter = BuildConfigurationReportComponentsFilterDto(
                    includeSystems = setOf(SYSTEM)
                ),
                checks = BuildConfigurationReportChecksDto(
                    buildStage = BuildStage.BUILD,
                    parameters = listOf("XRAY"),
                    steps = listOf("Compile")
                )
            )
        )

        val componentReport = response.result.firstOrNull { it.componentId == "componentA" }
        assertNotNull(componentReport)
        assertEquals(ComponentReportStatus.OK, componentReport!!.status)
        assertEquals(2, componentReport.checks.size)
        assertTrue(componentReport.checks.all { it.status })
    }

    companion object {
        private const val ROOT_PROJECT_ID = "RootProject"
        private const val BASE_PROJECT_ID = "RDDepartment"
        private const val SYSTEM = "TEST_SYSTEM"

        private val reportingServiceHost: String = System.getProperty("test.reporting-service-host")
            ?: error("System property 'test.reporting-service-host' must be defined")

        private val mockServerPort: Int = System.getProperty("test.mockserver-port", "1080").toInt()

        private lateinit var mockServer: ClientAndServer
        private lateinit var teamCity: TeamCityMockServer

        private val client: ReportingServiceClient = ReportingServiceClientFactory.create(
            ReportingServiceClientConfig(baseUrl = "http://$reportingServiceHost")
        )

        @BeforeAll
        @JvmStatic
        fun startMockServer() {
            mockServer = ClientAndServer.startClientAndServer(mockServerPort)
            teamCity = TeamCityMockServer(
                client = MockServerClient("localhost", mockServerPort),
                baseProjectId = BASE_PROJECT_ID
            )
        }

        @AfterAll
        @JvmStatic
        fun stopMockServer() {
            mockServer.stop()
        }
    }
}