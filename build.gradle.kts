buildscript {
    dependencies {
        classpath("gradle.plugin.com.github.sherter.google-java-format:google-java-format-gradle-plugin:0.8")
    }
}

repositories {
    maven("https://oss.jfrog.org/artifactory/oss-snapshot-local") {
        mavenContent {
            snapshotsOnly()
        }
    }
}

allprojects {
    group = "com.newrelic.telemetry"
    version = project.findProperty("releaseVersion") as String
    repositories {
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    }
    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

plugins {
    id("com.github.sherter.google-java-format") version "0.8"
    `java-library`
    `maven-publish`
    signing
}

googleJavaFormat {
    exclude(".**")
}

dependencies {
    api("com.newrelic.telemetry:telemetry:0.3.1")
    implementation("io.opentelemetry:opentelemetry-sdk:0.2.0")
    implementation("com.newrelic.telemetry:telemetry-http-okhttp:0.3.1")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    testRuntimeOnly("org.slf4j:slf4j-simple:1.7.26")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
    testImplementation("org.mockito:mockito-core:3.0.0")
    testImplementation("org.mockito:mockito-junit-jupiter:3.0.0")
    testImplementation("com.google.guava:guava:28.0-jre")
}

val jar: Jar by tasks
jar.apply {
    manifest.attributes["Implementation-Version"] = project.version
    manifest.attributes["Implementation-Vendor"] = "New Relic, Inc"
}

tasks {
    val taskScope = this
    val sources = sourceSets
    val sourcesJar by creating(Jar::class) {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        archiveClassifier.set("sources")
        from(sources.main.get().allSource)
    }

    val javadocJar by creating(Jar::class) {
        dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
        archiveClassifier.set("javadoc")
        from(taskScope.javadoc)
    }
}
val useLocalSonatype = project.properties["useLocalSonatype"] == "true"

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            pom {
                name.set(project.name)
                description.set("Open Telemetry Java Exporters that send data to New Relic ingest.")
                url.set("https://github.com/newrelic/opentelemetry-exporters-newrelic")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("newrelic")
                        name.set("New Relic")
                        email.set("opensource@newrelic.com")
                    }
                }
                scm {
                    url.set("git@github.com:newrelic/opentelemetry-exporters-newrelic.git")
                    connection.set("scm:git@github.com:newrelic/opentelemetry-exporters-newrelicc.git")
                }
            }
        }
    }
    repositories {
        maven {
            if (useLocalSonatype) {
                val releasesRepoUrl = uri("http://localhost:8081/repository/maven-releases/")
                val snapshotsRepoUrl = uri("http://localhost:8081/repository/maven-snapshots/")
                url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            } else {
                val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
                url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                configure<SigningExtension> {
                    sign(publications["mavenJava"])
                }
            }
            credentials {
                username = project.properties["sonatypeUsername"] as String?
                password = project.properties["sonatypePassword"] as String?
            }
        }
    }
}
