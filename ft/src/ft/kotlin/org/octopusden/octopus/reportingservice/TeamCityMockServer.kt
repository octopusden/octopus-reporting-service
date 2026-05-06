package org.octopusden.octopus.reportingservice

import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType

/**
 * Обёртка над MockServer, настраивающая TeamCity REST API-стабы для FT.
 *
 * Семантика TC-эндпоинтов (пути, query-параметры) живёт в этом классе.
 * Тела ответов читаются из classpath-ресурсов (`ft/src/ft/resources/team-city/...`)
 * — в них лежат «чистые» TeamCity JSON-объекты.
 */
class TeamCityMockServer(
    private val client: MockServerClient,
    private val baseProjectId: String
) {

    fun reset() {
        client.reset()
    }

    fun stubRootProjects(rootProjectId: String, bodyResourcePath: String) {
        client
            .`when`(
                request()
                    .withMethod("GET")
                    .withPath("/app/rest/projects")
                    .withQueryStringParameter("locator", "id:$rootProjectId,archived:false")
            )
            .respond(jsonResponse(readResource(bodyResourcePath)))
    }

    fun stubChildrenEmpty(rootProjectId: String) {
        client
            .`when`(
                request()
                    .withMethod("GET")
                    .withPath("/app/rest/projects")
                    .withQueryStringParameter("locator", ".*affectedProject.*id:$rootProjectId.*")
            )
            .respond(jsonResponse(EMPTY_PROJECTS_PAGE))
    }

    /**
     * Стаб на запрос шаблона: `GET /app/rest/projects?locator=id:<baseProjectId>`.
     */
    fun stubTemplate(bodyResourcePath: String) {
        client
            .`when`(
                request()
                    .withMethod("GET")
                    .withPath("/app/rest/projects")
                    .withQueryStringParameter("locator", "id:$baseProjectId")
            )
            .respond(jsonResponse(readResource(bodyResourcePath)))
    }


    private fun jsonResponse(body: String) = response()
        .withStatusCode(200)
        .withContentType(MediaType.APPLICATION_JSON)
        .withBody(body)

    private fun readResource(path: String): String {
        val stream = javaClass.classLoader.getResourceAsStream(path)
            ?: error("Resource '$path' not found in classpath")
        return stream.use { it.bufferedReader().readText() }
    }

    companion object {
        private const val EMPTY_PROJECTS_PAGE = """{"count":0,"project":[]}"""
    }
}