plugins {
    kotlin("jvm")
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
    ftImplementation("org.junit.jupiter:junit-jupiter-api")
    ftImplementation("org.junit.jupiter:junit-jupiter-params")
    ftImplementation("org.mock-server:mockserver-netty:${properties["mockserver.version"]}")
    ftImplementation("org.mock-server:mockserver-client-java:${properties["mockserver.version"]}")
    "ftRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine")
    "ftRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

val ft by tasks.creating(Test::class) {
    group = "verification"
    description = "Runs reporting-service functional tests."

    testClassesDirs = sourceSets["ft"].output.classesDirs
    classpath = sourceSets["ft"].runtimeClasspath

    useJUnitPlatform()

    // Хосты можно переопределить через -Dtest.<prop>=... при запуске `./gradlew :ft:ft`.
    systemProperty("test.reporting-service-host", System.getProperty("test.reporting-service-host", "localhost:8080"))
    systemProperty("test.mockserver-port", System.getProperty("test.mockserver-port", "1080"))

    testLogging {
        events("passed", "skipped", "failed")
    }
}