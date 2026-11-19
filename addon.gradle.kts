// Extra build configuration for the GTNH/RFG build.
//
// Two test layers live here:
//
//  * `test` (src/test/java) - headless JUnit 5 + Mockito, run on a Java 8 toolchain, measured by JaCoCo and
//    enforced against a coverage floor by `check`.
//  * `gameTest` (src/gameTest/java) - a separate, never-shipped `@Mod` that asserts FuzziTweaks against a real
//    dedicated server. It is only compiled by `check` and only put on the classpath by `./gradlew gameTestServer`
//    (or any run task with `-PgameTest`).
//
// The layout mirrors MrFuzzihead/PeripheralsPlusPlus, which has the same two layers.

import java.math.BigDecimal

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

// ---------------------------------------------------------------------------------------------------------
// Unit tests + coverage
// ---------------------------------------------------------------------------------------------------------

apply(plugin = "jacoco")

project.extensions.configure(JacocoPluginExtension::class.java) {
    toolVersion = "0.8.16"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    extensions.configure(JacocoTaskExtension::class.java) {
        isEnabled = true
    }
    // The mod targets Java 8 bytecode; run the suite on the same JVM generation the mod supports. Mockito 4's
    // inline mock maker also behaves best there.
    javaLauncher.set(
        project.extensions.getByType(JavaToolchainService::class.java).launcherFor {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    )
    testLogging {
        events("failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}

// Tests need the compileOnly dependencies too: several production classes implement compileOnly supertypes
// (Galacticraft, Railcraft, ...), so a test cannot even name the class without them on its classpath.
val mainSourceSet = project.extensions.getByType(SourceSetContainer::class.java).getByName("main")
configurations.named("testCompileClasspath") {
    extendsFrom(configurations.named("compileOnly").get())
}
configurations.named("testRuntimeClasspath") {
    extendsFrom(configurations.named("compileOnly").get())
}

// Only the unit test run feeds the report. A gameTestServer run also writes jacoco/gameTest.exec (see the agent
// below), but in a Forge 1.7.10 *dev* launch the LaunchClassLoader round-trips every class through ASM, so the class
// ids in that file cannot match the class files on disk. That data is reported separately against the classes the
// game actually loaded (jacocoGameTestReport).
val jacocoExecFiles = project.files(project.layout.buildDirectory.file("jacoco/test.exec"))

fun jacocoClassDirs() = project.files(mainSourceSet.output.classesDirs).asFileTree.matching {
    // The generated version holder and the mixin classes are not meaningfully unit-testable headless - mixins are
    // covered by the bytecode guards in src/test/java - and counting them would only dilute the number.
    exclude("com/mrfuzzihead/fuzzitweaks/Tags.class", "com/mrfuzzihead/fuzzitweaks/mixins/**")
}

tasks.named<JacocoReport>("jacocoTestReport") {
    group = "verification"
    description = "Generates HTML and XML JaCoCo coverage reports from the unit test run"
    dependsOn(tasks.named("test"))

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(jacocoClassDirs())
    sourceDirectories.setFrom(mainSourceSet.allJava.srcDirs)
    executionData.setFrom(jacocoExecFiles)
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    group = "verification"
    description = "Fails the build when unit test coverage drops below the configured floor"
    dependsOn(tasks.named("test"))

    classDirectories.setFrom(jacocoClassDirs())
    sourceDirectories.setFrom(mainSourceSet.allJava.srcDirs)
    executionData.setFrom(jacocoExecFiles)

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                // A regression guard at the current baseline, not a target. The suite covers ~58% of the measurable
                // lines; raise the floors when a change adds covered code.
                minimum = BigDecimal("0.56")
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.52")
            }
        }
    }
}

// `check` runs the tests and enforces the floor, so CI catches coverage regressions.
tasks.named("check").configure {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

// ---------------------------------------------------------------------------------------------------------
// In-game test mod (never shipped)
// ---------------------------------------------------------------------------------------------------------

val gameTestSourceSet = project.extensions.getByType(SourceSetContainer::class.java).create("gameTest")

configurations.getByName(gameTestSourceSet.compileClasspathConfigurationName)
    .extendsFrom(configurations.getByName("compileClasspath"))
configurations.getByName(gameTestSourceSet.runtimeClasspathConfigurationName)
    .extendsFrom(configurations.getByName("runtimeClasspath"))
gameTestSourceSet.compileClasspath += mainSourceSet.output
gameTestSourceSet.compileClasspath += mainSourceSet.compileClasspath
gameTestSourceSet.runtimeClasspath += mainSourceSet.output
gameTestSourceSet.runtimeClasspath += mainSourceSet.runtimeClasspath
// The game-test mod is plain Java 8. Leaving Jabel's compiler plugin on this source set's classpath makes javac
// auto-load it through ServiceLoader, which then needs ByteBuddy's agent (not present here).
gameTestSourceSet.compileClasspath = gameTestSourceSet.compileClasspath
    .filter { !it.name.contains("jabel") }

val isGameTestRun = project.hasProperty("gameTest") ||
    gradle.startParameter.taskNames.any { it.contains("gameTestServer") }

/**
 * Attaches the JaCoCo agent to a forked game.
 *
 * <p>
 * The `Test` tasks get the agent from the jacoco plugin; a `JavaExec` run task needs it as an explicit
 * `-javaagent` argument. The agent jar is declared as a `@Classpath` input so Gradle resolves it with the task.
 * </p>
 */
abstract class JacocoAgentArgumentProvider : CommandLineArgumentProvider {

    @get:Classpath
    abstract val agent: ConfigurableFileCollection

    @get:Input
    abstract val destFile: Property<String>

    @get:Input
    abstract val includes: Property<String>

    @get:Input
    abstract val classDumpDir: Property<String>

    override fun asArguments(): Iterable<String> {
        val jar = extractAgent()
        // A reused build directory would otherwise keep the dumps of earlier runs. Their class ids no longer match
        // the new execution data (the version string in Tags alone changes the class), and JaCoCo cannot report on
        // two copies of the same class.
        java.io.File(classDumpDir.get()).deleteRecursively()
        return listOf(
            "-javaagent:" + jar.absolutePath +
                "=destfile=" + destFile.get() +
                ",append=false,dumponexit=true" +
                ",includes=" + includes.get() +
                // `classdumpdir` makes the agent write every class it instruments, i.e. the post-transform bytes the
                // game actually loaded. That is the only class set whose JaCoCo ids can match this execution data: the
                // dev LaunchClassLoader round-trips every class through ASM, so the files under build/classes differ.
                ",classdumpdir=" + classDumpDir.get()
        )
    }

    /**
     * The `jacocoAgent` configuration resolves to the JaCoCo *distribution* jar, which has no `Premain-Class` of its
     * own: the real agent is `jacocoagent.jar` inside it. Gradle's own JaCoCo support unpacks it the same way.
     */
    private fun extractAgent(): java.io.File {
        val distribution = agent.files.single()
        val target = java.io.File(destFile.get()).parentFile.resolve("jacocoagent.jar")
        java.util.zip.ZipFile(distribution).use { zip ->
            val entry = zip.getEntry("jacocoagent.jar") ?: return distribution
            target.parentFile?.mkdirs()
            zip.getInputStream(entry).use { input -> target.outputStream().use { output -> input.copyTo(output) } }
        }
        return target
    }
}

if (isGameTestRun) {
    // Only our package is instrumented, and the file is dumped when the game calls System.exit (the game test's
    // exitJava with halt=false), so a crashed or hung run simply leaves no data behind.
    val jacocoProvider = project.objects.newInstance(JacocoAgentArgumentProvider::class.java)
    jacocoProvider.agent.from(configurations.named("jacocoAgent"))
    jacocoProvider.destFile.set(
        project.layout.buildDirectory.file("jacoco/gameTest.exec")
            .map { it.asFile.absolutePath }
    )
    jacocoProvider.includes.set("com.mrfuzzihead.fuzzitweaks.*")
    jacocoProvider.classDumpDir.set(
        project.layout.buildDirectory.dir("jacoco/gameTest-classes")
            .map { it.asFile.absolutePath }
    )

    tasks.withType<JavaExec>().configureEach {
        if (name.startsWith("runServer") || name.startsWith("runClient")) {
            classpath += gameTestSourceSet.output
            systemProperty("fuzzitweaks.gameTest", "true")
            // A reused dev world may reference blocks from mods that are no longer installed; the game test is not
            // about that, so answer FML's "continue anyway?" prompt up front instead of blocking on stdin.
            systemProperty("fml.queryResult", "confirm")
            jvmArgumentProviders.add(jacocoProvider)
        }
    }
}

tasks.named("check").configure {
    dependsOn(gameTestSourceSet.classesTaskName)
}

// ---------------------------------------------------------------------------------------------------------
// In-game coverage report
// ---------------------------------------------------------------------------------------------------------
// `gameTestServer` runs with the JaCoCo agent attached and writes build/jacoco/gameTest.exec. That data cannot be
// merged into jacocoTestReport: the Forge LaunchClassLoader round-trips every class through ASM, so the on-disk class
// files have different JaCoCo ids. The agent therefore also dumps what it instrumented to
// build/jacoco/gameTest-classes (the post-transform bytes the game loaded), and the report analyses those.
tasks.register<JacocoReport>("jacocoGameTestReport") {
    group = "verification"
    description = "Reports coverage from the in-game game test run, using the post-transform classes the game loaded"
    // No `onlyIf`: a script closure cannot be stored in the configuration cache. An empty execution-data collection
    // makes JaCoCo skip the task instead, which is the same outcome.
    executionData.setFrom(
        project.files(project.layout.buildDirectory.file("jacoco/gameTest.exec")).filter { it.exists() }
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
        xml.outputLocation.set(project.layout.buildDirectory.file("reports/jacoco/gameTest/jacocoGameTestReport.xml"))
        html.outputLocation.set(project.layout.buildDirectory.dir("reports/jacoco/gameTest/html"))
    }

    // The game-test mod itself is not part of the mod, and the client classes never run on a dedicated server.
    classDirectories.setFrom(
        project.files(project.layout.buildDirectory.dir("jacoco/gameTest-classes"))
            .asFileTree
            .matching {
                include("com/mrfuzzihead/fuzzitweaks/**/*.class")
                exclude(
                    "com/mrfuzzihead/fuzzitweaks/Tags.class",
                    "com/mrfuzzihead/fuzzitweaks/mixins/**",
                    "com/mrfuzzihead/fuzzitweaks/gametest/**",
                    "com/mrfuzzihead/fuzzitweaks/client/**"
                )
            }
    )
    sourceDirectories.setFrom(mainSourceSet.allJava.srcDirs)
}

tasks.register("gameTestServer") {
    group = "verification"
    description = "Boots a dedicated server, runs the in-game game test mod, and exits with its result"
    if (isGameTestRun && tasks.names.contains("runServer")) dependsOn(tasks.named("runServer"))
    finalizedBy(tasks.named("jacocoGameTestReport"))
}
