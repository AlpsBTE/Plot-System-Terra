plugins {
    java
    alias(libs.plugins.lombok)
    alias(libs.plugins.git.version)
    alias(libs.plugins.shadow)
}

repositories {
    //mavenLocal() // NEVER use in Production/Commits!
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        url = uri("https://maven.enginehub.org/repo/")
    }

    maven {
        url = uri("https://mvn.alps-bte.com/repository/alps-bte/")
    }

    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(libs.com.alpsbte.canvas)
    implementation(libs.com.zaxxer.hikaricp) {
        exclude(group = "org.slf4j")
    }
    implementation(libs.com.alpsbte.alpslib.alpslib.utils)
    implementation(libs.com.alpsbte.alpslib.alpslib.io)
    implementation(libs.com.squareup.okhttp3.okhttp.jvm)
    implementation(libs.org.mariadb.jdbc.mariadb.java.client)
    implementation(libs.com.googlecode.json.simple)
    implementation(platform(libs.com.intellectualsites.bom.bom.newest))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly(libs.io.papermc.paper.paper.api)
    compileOnly(libs.com.arcaniax.headdatabase.api)
    compileOnly(libs.org.jetbrains.annotations)
}

val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
val details = versionDetails()

group = "com.alpsbte"
version = "5.0.2" + "-" + details.gitHash + "-SNAPSHOT"
description = "PlotSystem-Terra"
java.sourceCompatibility = JavaVersion.VERSION_21

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveClassifier = ""
    relocationPrefix = "com.alpsbte.plotsystemterra.shaded"
    enableAutoRelocation = true
}

tasks.assemble {
    dependsOn(tasks.shadowJar) // Ensure that the shadowJar task runs before the build task
}

tasks.jar {
    archiveClassifier = "UNSHADED"
    enabled = false // Disable the default jar task since we are using shadowJar
}

tasks.processResources {
    // work around IDEA-296490
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    with(copySpec {
        from("src/main/resources/plugin.yml") {
            expand("version" to version)
        }
    })
}
