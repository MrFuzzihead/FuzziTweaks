
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// The unit tests are JUnit 5 (see dependencies.gradle).
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
