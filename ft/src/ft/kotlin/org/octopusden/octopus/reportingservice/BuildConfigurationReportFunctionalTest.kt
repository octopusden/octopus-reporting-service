package org.octopusden.octopus.reportingservice

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockserver.client.MockServerClient
import org.octopusden.octopus.reportingservice.client.ReportingServiceClient
import org.octopusden.octopus.reportingservice.client.ReportingServiceClientConfig
import org.octopusden.octopus.reportingservice.client.ReportingServiceClientFactory
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportChecksDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportComponentsFilterDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportRequestDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportResponseDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildStage

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BuildConfigurationReportFunctionalTest {

    private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    private val client: ReportingServiceClient = ReportingServiceClientFactory.create(
        ReportingServiceClientConfig(baseUrl = "http://$reportingServiceHost")
    )

    private lateinit var teamCity: TeamCityMockServer

    @BeforeEach
    fun resetMocks() {
        teamCity.reset()
    }

    @BeforeAll
    fun startMockServer() {
        teamCity = TeamCityMockServer(
            client = MockServerClient(mockServerHost, mockServerPort),
            baseProjectId = BASE_PROJECT_ID
        )
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
    fun generateReportFullTest() {
        teamCity.stubRootProjects(ROOT_PROJECT_ID, "teamcity-mock-data/projects.json")
        teamCity.stubChildrenPages(
            ROOT_PROJECT_ID,
            "teamcity-mock-data/children-page-1.json",
            "teamcity-mock-data/children-page-2.json"
        )
        teamCity.stubTemplate("teamcity-mock-data/templates.json")

        val actual = client.generateBuildConfigurationReport(
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
        val expected = loadExpected("expected-reports/generateReportFullTest.json")
        assertEquals(expected, actual)
    }

    private fun loadExpected(resourcePath: String): BuildConfigurationReportResponseDto {
        val stream = javaClass.classLoader.getResourceAsStream(resourcePath)
            ?: error("Resource '$resourcePath' not found in classpath")
        return stream.use { objectMapper.readValue(it, BuildConfigurationReportResponseDto::class.java) }
    }

    companion object {
        private const val ROOT_PROJECT_ID = "RootProject"
        private const val BASE_PROJECT_ID = "RDDepartment"
        private const val SYSTEM = "TEST_SYSTEM"

        private val reportingServiceHost: String = System.getProperty("test.reporting-service-host")
            ?: error("System property 'test.reporting-service-host' must be defined")
        private val mockServerPort: Int = System.getProperty("test.mockserver-port").toInt()
        private val mockServerHost: String = System.getProperty("test.mockserver-host")
            ?: error("System property 'test.mockserver-host' must be defined")
    }
}