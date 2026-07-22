package org.octopusden.octopus.reportingservice.client.common.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@MustBeDocumented
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER,
)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [NoBlankElementsValidator::class])
annotation class NoBlankElements(
    val message: String = "must not contain blank elements",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class NoBlankElementsValidator : ConstraintValidator<NoBlankElements, Collection<String>> {
    override fun isValid(
        value: Collection<String>?,
        context: ConstraintValidatorContext,
    ): Boolean {
        if (value == null) return true
        return value.none { it.isBlank() }
    }
}
