pluginManagement {
    plugins {
        kotlin("jvm") version providers.gradleProperty("kotlin.version").get()
        kotlin("plugin.spring") version providers.gradleProperty("kotlin.version").get()
        id("io.spring.dependency-management") version providers.gradleProperty("spring-dependency-management.version").get()
        id("org.springframework.boot") version providers.gradleProperty("spring-boot.version").get()
        id("com.bmuschko.docker-spring-boot-application") version providers.gradleProperty("bmuschko-docker-plugin.version").get()
        id("org.octopusden.octopus.oc-template") version providers.gradleProperty("octopus-oc-template.version").get()
        id("io.github.gradle-nexus.publish-plugin") version providers.gradleProperty("gradle-nexus-publish-plugin.version").get()
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