plugins {
    kotlin("jvm") version "2.0.20"
}

group = "com.kitaharaa"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}
tasks {
    val jar by getting(Jar::class) {
        manifest {
            attributes(
                "Main-Class" to "com.kitaharaa.AutomateTranslationKt" // Your main class
            )
        }
        from(sourceSets.main.get().output) // Include compiled classes in the JAR
        // Include all dependencies in the JAR
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    }
}