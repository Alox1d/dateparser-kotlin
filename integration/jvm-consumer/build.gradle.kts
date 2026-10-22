import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.20"
    application
}

dependencies {
    implementation("io.github.alox1d:dateparser-kotlin:0.1.3")
    testImplementation(kotlin("test-junit"))
}

// Use the current JDK (17-21); no extra JDK/toolchain installation is required.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

application { mainClass.set("io.github.alox1d.dateparser.jvmconsumer.DemoKt") }

tasks.named<JavaExec>("run") {
    for ((property, option) in listOf(
        "demoText" to "--text",
        "demoDateOrder" to "--date-order",
        "demoReference" to "--reference",
        "demoTimeZone" to "--time-zone"
    )) {
        providers.gradleProperty(property).orNull?.let { args(option, it) }
    }
}
