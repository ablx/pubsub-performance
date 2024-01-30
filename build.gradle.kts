plugins {
    kotlin("jvm") version "1.9.21"
}

group = "dev.verbosemode"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("com.google.cloud:libraries-bom:26.28.0"))
    implementation("com.google.cloud:google-cloud-pubsub")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}