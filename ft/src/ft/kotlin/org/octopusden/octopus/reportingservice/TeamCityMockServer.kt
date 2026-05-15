package org.octopusden.octopus.reportingservice

import org.mockserver.client.MockServerClient
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType

class TeamCityMockServer(
    private val client: MockServerClient,
    private val baseProjectId: String
) {

    fun reset() {
        client.reset()
    }

    fun stubRootProjects(rootProjectId: String, bodyResourcePath: String) {
        client
            .`when`(rootProjectsRequest(rootProjectId))
            .respond(jsonResponse(readResource(bodyResourcePath)))
    }

    fun stubRootProjectsStatus(rootProjectId: String, statusCode: Int) {
        client
            .`when`(rootProjectsRequest(rootProjectId))
            .respond(response().withStatusCode(statusCode))
    }

    fun stubChildrenPages(rootProjectId: String, vararg pageBodyResourcePaths: String) {
        pageBodyResourcePaths.forEach { resourcePath ->
            client
                .`when`(childrenPagesRequest(rootProjectId), Times.once())
                .respond(jsonResponse(readResource(resourcePath)))
        }
    }

    fun stubTemplate(bodyResourcePath: String) {
        client
            .`when`(templateRequest())
            .respond(jsonResponse(readResource(bodyResourcePath)))
    }

    fun stubTemplateStatus(statusCode: Int) {
        client
            .`when`(templateRequest())
            .respond(response().withStatusCode(statusCode))
    }

    private fun rootProjectsRequest(rootProjectId: String) =
        request()
            .withMethod("GET")
            .withPath(PROJECTS_PATH)
            .withQueryStringParameter("locator", "archived:false,id:$rootProjectId")

    private fun childrenPagesRequest(rootProjectId: String) =
        request()
            .withMethod("GET")
            .withPath(PROJECTS_PATH)
            .withQueryStringParameter("locator", "affectedProject:\\(id:$rootProjectId\\).*")

    private fun templateRequest() =
        request()
            .withMethod("GET")
            .withPath(PROJECTS_PATH)
            .withQueryStringParameter("locator", "id:$baseProjectId")

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
        private const val PROJECTS_PATH = "/app/rest/2018.1/projects"
    }
}