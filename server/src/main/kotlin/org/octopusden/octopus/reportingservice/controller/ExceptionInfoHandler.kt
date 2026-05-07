package org.octopusden.octopus.reportingservice.controller

import org.octopusden.octopus.reportingservice.client.common.exception.ErrorResponse
import org.octopusden.octopus.reportingservice.client.common.exception.ExternalServiceException
import org.octopusden.octopus.reportingservice.client.common.exception.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ExceptionInfoHandler {

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(exception: RuntimeException): ErrorResponse {
        val message = exception.message ?: "Not found"
        logger.info("Not found: {}", message)
        return ErrorResponse(
            code = HttpStatus.NOT_FOUND.value().toString(),
            message = message
        )
    }

    @ExceptionHandler(ExternalServiceException::class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    fun handleExternalServiceException(exception: ExternalServiceException): ErrorResponse {
        val message = exception.message ?: "External service is unavailable"
        logger.error("External service error: {}", exception.message, exception)
        return ErrorResponse(
            code = HttpStatus.BAD_GATEWAY.value().toString(),
            message = message
        )
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleException(exception: Exception): ErrorResponse {
        val message = exception.message ?: "Unexpected error"
        logger.error(message, exception)
        return ErrorResponse(
            code = HttpStatus.INTERNAL_SERVER_ERROR.value().toString(),
            message = message
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExceptionInfoHandler::class.java)
    }
}