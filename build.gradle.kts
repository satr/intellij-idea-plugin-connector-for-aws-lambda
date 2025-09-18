plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "io.github.satr"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.amazonaws:aws-java-sdk-lambda:1.12.791")
    implementation("com.amazonaws:aws-java-sdk-iam:1.12.791")
    implementation("com.amazonaws:aws-java-sdk-logs:1.12.791")
    implementation("log4j:log4j:1.2.17")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    testImplementation(platform("org.mockito:mockito-all:1.10.19"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

intellij {
    version.set("2024.3")
    type.set("IU") // Ultimate Edition for Maven integration
    plugins.set(listOf("java", "maven")) // Add Maven plugin dependency
}

tasks.named("buildSearchableOptions") {
    enabled = false
}
