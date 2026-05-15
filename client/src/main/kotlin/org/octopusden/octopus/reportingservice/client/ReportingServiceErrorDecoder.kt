package org.octopusden.octopus.reportingservice.client

import feign.Response
import feign.Util
import feign.codec.ErrorDecoder
import org.octopusden.octopus.reportingservice.client.common.exception.ExternalServiceException
import org.octopusden.octopus.reportingservice.client.common.exception.InternalException
import org.octopusden.octopus.reportingservice.client.common.exception.NotFoundException
import org.octopusden.octopus.reportingservice.client.common.exception.ValidationException
import java.io.IOException

class ReportingServiceErrorDecoder : ErrorDecoder {
    override fun decode(methodKey: String, response: Response): Exception {
        val status = response.status()
        val method = response.request().httpMethod().name
        val url = response.request().url()
        val body = response.safelyReadBody()
        val message = buildMessage(status, method, url, body)
        return when (status) {
            400 -> ValidationException(message)
            404 -> NotFoundException(message)
            502 -> ExternalServiceException(message)
            else -> InternalException(message)
        }
    }

    private fun buildMessage(status: Int, method: String, url: String, body: String?): String =
        "reporting-service responded $status on $method $url" +
                (body?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: "")

    private fun Response.safelyReadBody(): String? = try {
        body()?.asInputStream()?.use { Util.toString(it.reader(Charsets.UTF_8)) }
    } catch (_: IOException) {
        null
    }
}