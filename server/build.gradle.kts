import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("com.bmuschko.docker-spring-boot-application")
    // Applied but declaring NO publication: this module is not published to Maven Central.
    // Keeping the plugin leaves the `publish` lifecycle task in place as a no-op, so a CI
    // invocation of it does not fail. Declaring no MavenPublication is what keeps the module
    // off Central; the declared `centralPublications` set in the root build's
    // `octopusQuality { publication { } }` block (which this module is absent from) enforces that.
    `maven-publish`
}

fun String.getExt() = project.ext[this] as String

docker {
    springBootApplication {
        baseImage.set("${"dockerRegistry".getExt()}/eclipse-temurin:21-jdk")
        ports.set(listOf(8080))
        images.set(setOf("${"octopusGithubDockerRegistry".getExt()}/octopusden/$name:$version"))
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":common"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${properties["springdoc-openapi.version"]}")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation(
        "org.octopusden.octopus.infrastructure:components-registry-service-client:${properties["octopus-components-registry.version"]}",
    )
    implementation(
        "org.octopusden.octopus.octopus-external-systems-clients:teamcity-client:${properties["octopus-teamcity-client.version"]}",
    )
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${properties["mockito-kotlin.version"]}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

springBoot {
    buildInfo()
}
