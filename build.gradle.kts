plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "ksmn"
version = "1.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("io.github.spair:imgui-java-app:1.86.11")
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-analysis:9.6")
    implementation("org.ow2.asm:asm-commons:9.6")
    implementation("org.ow2.asm:asm-tree:9.6")
    implementation("org.ow2.asm:asm-util:9.6")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

tasks.jar {
    manifest.attributes(
        "Main-Class" to "MainKt"
    )

    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    configurations["compileClasspath"].forEach { file : File ->
        from(zipTree(file.absoluteFile))
    }
}