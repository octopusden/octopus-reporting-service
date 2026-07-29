package org.octopusden.octopus.reportingservice.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import feign.Feign
import feign.Request
import feign.Retryer
import feign.httpclient.ApacheHttpClient
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.slf4j.Slf4jLogger
import java.util.concurrent.TimeUnit

object ReportingServiceClientFactory {
    fun create(config: ReportingServiceClientConfig): ReportingServiceClient = create(config, defaultObjectMapper())

    fun create(
        config: ReportingServiceClientConfig,
        objectMapper: ObjectMapper,
    ): ReportingServiceClient =
        Feign
            .builder()
            .client(ApacheHttpClient())
            .encoder(JacksonEncoder(objectMapper))
            .decoder(JacksonDecoder(objectMapper))
            .errorDecoder(ReportingServiceErrorDecoder())
            .logger(Slf4jLogger(ReportingServiceClient::class.java))
            .retryer(Retryer.NEVER_RETRY)
            .options(
                Request.Options(
                    config.connectTimeoutMs,
                    TimeUnit.MILLISECONDS,
                    config.readTimeoutMs,
                    TimeUnit.MILLISECONDS,
                    true,
                ),
            ).target(ReportingServiceClient::class.java, config.baseUrl.trimEnd('/'))

    private fun defaultObjectMapper(): ObjectMapper =
        jacksonObjectMapper().apply {
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }
}
