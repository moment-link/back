import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version "2.3.21" apply false
    kotlin("plugin.spring") version "2.3.21" apply false
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.momentlink"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    if (childProjects.isEmpty()) {
        apply(plugin = "org.jetbrains.kotlin.jvm")
        apply(plugin = "java-library")

        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(17)
            compilerOptions {
                freeCompilerArgs.addAll(
                    "-Xjsr305=strict",
                    "-Xannotation-default-target=param-property",
                )
            }
        }

        dependencies {
            add("testImplementation", kotlin("test"))
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
