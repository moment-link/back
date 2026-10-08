import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    kotlin("plugin.jpa")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom(SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    api("jakarta.persistence:jakarta.persistence-api")
}
