package org.octopusden.octopus.reportingservice.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildStage

@DisplayName("BuildConfigurationReportConfig")
class BuildConfigurationReportConfigTest {
    @Nested
    @DisplayName("getTemplates")
    inner class GetTemplates {
        @Test
        @DisplayName("Returns templates for BUILD stage")
        fun buildStage() {
            val config = BuildConfigurationReportConfig(
                baseProjectId = "base",
                templates = Templates(build = listOf("B1", "B2")),
            )

            assertEquals(listOf("B1", "B2"), config.getTemplates(BuildStage.BUILD))
        }

        @Test
        @DisplayName("Returns templates for RELEASE_CANDIDATE stage")
        fun releaseCandidateStage() {
            val config = BuildConfigurationReportConfig(
                baseProjectId = "base",
                templates = Templates(releaseCandidate = listOf("RC1")),
            )

            assertEquals(listOf("RC1"), config.getTemplates(BuildStage.RELEASE_CANDIDATE))
        }

        @Test
        @DisplayName("Returns templates for RELEASE stage")
        fun releaseStage() {
            val config = BuildConfigurationReportConfig(
                baseProjectId = "base",
                templates = Templates(release = listOf("R1")),
            )

            assertEquals(listOf("R1"), config.getTemplates(BuildStage.RELEASE))
        }

        @Test
        @DisplayName("Empty templates list for stage throws IllegalStateException")
        fun emptyTemplatesForStageThrows() {
            val config = BuildConfigurationReportConfig(
                baseProjectId = "base",
                templates = Templates(),
            )

            val ex = assertThrows(IllegalStateException::class.java) {
                config.getTemplates(BuildStage.BUILD)
            }
            assertEquals("No TeamCity templates configured for BuildStage=BUILD", ex.message)
        }
    }
}
