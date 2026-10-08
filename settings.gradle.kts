pluginManagement {
    plugins {
        kotlin("plugin.jpa") version "2.3.21"
    }
}
rootProject.name = "moment-link"

include(
    ":moment-link-admin",
    ":moment-link-api",
    ":moment-link-sync",
    ":moment-link-domain",
)
