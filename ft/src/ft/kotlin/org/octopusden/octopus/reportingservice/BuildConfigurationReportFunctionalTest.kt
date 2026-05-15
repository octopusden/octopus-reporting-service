package org.octopusden.octopus.reportingservice

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockserver.client.MockServerClient
import org.octopusden.octopus.reportingservice.client.ReportingServiceClient
import org.octopusden.octopus.reportingservice.client.ReportingServiceClientConfig
import org.octopusden.octopus.reportingservice.client.ReportingServiceClientFactory
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportResponseDto
import org.octopusden.octopus.reportingservice.client.common.exception.ExternalServiceException
import org.octopusden.octopus.reportingservice.Fixtures.BASE_PROJECT_ID
import org.octopusden.octopus.reportingservice.Fixtures.ROOT_PROJECT_ID
import org.octopusden.octopus.reportingservice.Fixtures.SYSTEM
import org.octopusden.octopus.reportingservice.Fixtures.requestBuildConfigurationReport

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Reporting Service / FT")
class BuildConfigurationReportFunctionalTest {

    private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

    private val client: ReportingServiceClient = ReportingServiceClientFactory.create(
        ReportingServiceClientConfig(baseUrl = "http://$reportingServiceHost")
    )

    private lateinit var teamCity: TeamCityMockServer

    @BeforeAll
    fun startMockServer() {
        teamCity = TeamCityMockServer(
            client = MockServerClient(mockServerHost, mockServerPort),
            baseProjectId = BASE_PROJECT_ID
        )
    }

    @BeforeEach
    fun resetMocks() {
        teamCity.reset()
    }

    @Nested
    @DisplayName("Happy path")
    inner class HappyPath {

        @Test
        @DisplayName("Empty checks -> empty response")
        fun emptyReportTest() {
            val response = client.generateBuildConfigurationReport(
                requestBuildConfigurationReport(rootProjectId = ROOT_PROJECT_ID, systems = setOf(SYSTEM))
            )
            assertEquals(ROOT_PROJECT_ID, response.rootProjectId)
            assertTrue(response.result.isEmpty())
        }

        @Test
        @DisplayName("Full scenario: components with 4 different statuses, one parameter and one step")
        fun generateReportFullTest() {
            teamCity.stubRootProjects(ROOT_PROJECT_ID, "teamcity-mock-data/projects.json")
            teamCity.stubChildrenPages(
                ROOT_PROJECT_ID,
                "teamcity-mock-data/children-page-1.json",
                "teamcity-mock-data/children-page-2.json"
            )
            teamCity.stubTemplate("teamcity-mock-data/templates.json")

            val actual = client.generateBuildConfigurationReport(
                requestBuildConfigurationReport(
                    rootProjectId = ROOT_PROJECT_ID,
                    systems = setOf(SYSTEM),
                    parameters = listOf("XRAY"),
                    steps = listOf("Compile")
                )
            )
            val expected = loadExpected("expected-reports/generateReportFullTest.json")
            assertEquals(expected, actual)
        }
    }

    @Nested
    @DisplayName("Error handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("TeamCity returns 5xx on root request -> client receives ExternalServiceException (502)")
        fun teamCityServerErrorBecomesBadGateway() {
            teamCity.stubRootProjectsStatus(ROOT_PROJECT_ID, statusCode = 500)
            teamCity.stubTemplate("teamcity-mock-data/templates.json")

            val ex = assertThrows(ExternalServiceException::class.java) {
                client.generateBuildConfigurationReport(
                    requestBuildConfigurationReport(
                        rootProjectId = ROOT_PROJECT_ID,
                        systems = setOf(SYSTEM),
                        parameters = listOf("XRAY")
                    )
                )
            }
            assertTrue(ex.message?.contains("502") == true)
        }

        @Test
        @DisplayName("TeamCity returns 5xx on template request -> ExternalServiceException (502)")
        fun teamCityTemplateFailureBecomesBadGateway() {
            teamCity.stubRootProjects(ROOT_PROJECT_ID, "teamcity-mock-data/projects.json")
            teamCity.stubChildrenPages(
                ROOT_PROJECT_ID,
                "teamcity-mock-data/children-page-1.json",
                "teamcity-mock-data/children-page-2.json"
            )
            teamCity.stubTemplateStatus(statusCode = 503)

            val ex = assertThrows(ExternalServiceException::class.java) {
                client.generateBuildConfigurationReport(
                    requestBuildConfigurationReport(
                        rootProjectId = ROOT_PROJECT_ID,
                        systems = setOf(SYSTEM),
                        parameters = listOf("XRAY")
                    )
                )
            }
            assertTrue(ex.message?.contains("502") == true)
        }
    }

    private fun loadExpected(resourcePath: String): BuildConfigurationReportResponseDto {
        val stream = javaClass.classLoader.getResourceAsStream(resourcePath)
            ?: error("Resource '$resourcePath' not found in classpath")
        return stream.use { objectMapper.readValue(it, BuildConfigurationReportResponseDto::class.java) }
    }

    companion object {
        private val reportingServiceHost: String = System.getProperty("test.reporting-service-host")
            ?: error("System property 'test.reporting-service-host' must be defined")
        private val mockServerPort: Int = System.getProperty("test.mockserver-port").toInt()
        private val mockServerHost: String = System.getProperty("test.mockserver-host")
            ?: error("System property 'test.mockserver-host' must be defined")
    }
}