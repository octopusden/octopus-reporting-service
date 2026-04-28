pluginManagement {
    plugins {
        kotlin("jvm") version extra["kotlin.version"] as String
        kotlin("plugin.spring") version extra["kotlin.version"] as String
        id("io.spring.dependency-management") version extra["spring-dependency-management.version"] as String
        id("org.springframework.boot") version extra["spring-boot.version"] as String
        id("com.bmuschko.docker-spring-boot-application") version extra["bmuschko-docker-plugin.version"] as String
        id("org.octopusden.octopus.oc-template") version extra["octopus-oc-template.version"] as String
        id("io.github.gradle-nexus.publish-plugin") version extra["gradle-nexus-publish-plugin.version"] as String
    }
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "octopus-reporting-service"
//include("automation")
include("client")
include("common")
//include("ft")
include("server")
findProject(":server")?.name = "reporting-service"