plugins {
    id("org.octopusden.octopus.oc-template")
}

sourceSets {
    create("ft") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val ftImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
}
ftImplementation.isCanBeResolved = true

configurations["ftRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

dependencies {
    ftImplementation(project(":client"))
    ftImplementation(project(":common"))
    ftImplementation("org.junit.jupiter:junit-jupiter")
    "ftRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    ftImplementation("org.mock-server:mockserver-netty:${properties["mockserver.version"]}")
    ftImplementation("org.mock-server:mockserver-client-java:${properties["mockserver.version"]}")
}

fun String.getExt() = project.ext[this] as String

val commonOkdParameters = mapOf(
    "ACTIVE_DEADLINE_SECONDS" to "okdActiveDeadlineSeconds".getExt(),
    "DOCKER_REGISTRY" to "dockerRegistry".getExt(),
)

tasks["ocProcess"].dependsOn(":reporting-service:dockerPushImage")

ocTemplate {
    workDir.set(layout.buildDirectory.dir("okd"))

    clusterDomain.set("okdClusterDomain".getExt())
    namespace.set("okdProject".getExt())
    prefix.set("reporting-ft")
    attempts.set(25)

    "okdWebConsoleUrl".getExt().takeIf { it.isNotBlank() }?.let {
        webConsoleUrl.set(it)
    }

    service("comp-reg") {
        templateFile.set(rootProject.layout.projectDirectory.file("okd/components-registry.yaml"))
        val componentsRegistryWorkDir = layout.projectDirectory
            .dir("src/ft/resources/components-registry-data")
            .asFile.absolutePath
        parameters.set(
            commonOkdParameters + mapOf(
                "COMPONENTS_REGISTRY_SERVICE_VERSION" to properties["octopus-components-registry.version"] as String,
                "AGGREGATOR_GROOVY_CONTENT" to file("$componentsRegistryWorkDir/Aggregator.groovy").readText(),
                "DEFAULTS_GROOVY_CONTENT" to file("$componentsRegistryWorkDir/Defaults.groovy").readText(),
                "TEST_COMPONENTS_GROOVY_CONTENT" to file("$componentsRegistryWorkDir/TestComponents.groovy").readText(),
                "APPLICATION_DEV_CONTENT" to
                    rootProject.layout.projectDirectory
                        .dir("okd/app-configs/components-registry-service.yaml")
                        .asFile
                        .readText(),
            ),
        )
    }

    service("mockserver") {
        templateFile.set(rootProject.layout.projectDirectory.file("okd/mockserver.yaml"))
        parameters.set(
            commonOkdParameters + mapOf(
                "MOCK_SERVER_VERSION" to properties["mockserver.version"] as String,
            ),
        )
    }

    service("reporting") {
        templateFile.set(rootProject.layout.projectDirectory.file("okd/reporting-service.yaml"))
        parameters.set(
            commonOkdParameters + mapOf(
                "TEST_MOCKSERVER_HOST" to ocTemplate.getOkdHost("mockserver"),
                "TEST_COMPONENTS_REGISTRY_HOST" to ocTemplate.getOkdHost("comp-reg"),
                "REPORTING_SERVICE_VERSION" to version.toString(),
                "APPLICATION_DEV_CONTENT" to rootProject.layout.projectDirectory
                    .dir("okd/app-configs/reporting-service.yaml")
                    .asFile
                    .readText(),
            ),
        )
    }
}

val ft by tasks.creating(Test::class) {
    group = "verification"
    description = "Runs reporting-service integration tests."
    testClassesDirs = sourceSets["ft"].output.classesDirs
    classpath = sourceSets["ft"].runtimeClasspath

    ocTemplate.isRequiredBy(this)
    systemProperties["test.reporting-service-host"] = ocTemplate.getOkdHost("reporting")
    systemProperties["test.mockserver-host"] = ocTemplate.getOkdHost("mockserver")
    systemProperties["test.mockserver-port"] = 80
}
