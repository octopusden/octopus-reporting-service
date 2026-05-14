package org.octopusden.octopus.reportingservice.client.common.exception

class NotFoundException(message: String) : RuntimeException(message)

class InternalException(message: String) : RuntimeException(message)

class ValidationException(message: String) : RuntimeException(message)

class ExternalServiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)