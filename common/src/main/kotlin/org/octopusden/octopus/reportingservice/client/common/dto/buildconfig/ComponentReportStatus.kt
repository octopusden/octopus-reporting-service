package org.octopusden.octopus.reportingservice.client.common.dto.buildconfig

/**
 * Состояние отчёта по конкретному компоненту.
 *
 * - [OK] — проект и сборка для компонента найдены, проверки выполнены.
 * - [NO_PROJECT] — в TeamCity не найден проект для компонента.
 * - [NO_BUILD_CONFIGURATION] — проект найден, но сборка, унаследованная
 *   от шаблона стадии, отсутствует.
 */
enum class ComponentReportStatus {
    OK,
    NO_PROJECT,
    NO_BUILD_CONFIGURATION
}