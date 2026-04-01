plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("signing")
}

android {
    namespace = "com.prashant.droidkit"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures { compose = true }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("debug") {
            manifest.srcFile("src/debug/AndroidManifest.xml")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}

val pomGroupId: String = findProperty("POM_GROUP_ID")?.toString() ?: "io.github.er-vprashant"
val pomArtifactId: String = findProperty("POM_ARTIFACT_ID")?.toString() ?: "droidkit"
val pomVersion: String = findProperty("POM_VERSION")?.toString() ?: "1.0.0"

val stagingDir = layout.buildDirectory.dir("maven-staging")

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = pomGroupId
                artifactId = pomArtifactId
                version = pomVersion

                pom {
                    name.set("DroidKit")
                    description.set("On-device debug toolkit for Android — inspect storage, fire deep links, test push notifications.")
                    url.set("https://github.com/er-vprashant/droidkit")
                    inceptionYear.set("2024")

                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            id.set("er-vprashant")
                            name.set("Prashant Verma")
                            url.set("https://github.com/er-vprashant")
                        }
                    }

                    scm {
                        url.set("https://github.com/er-vprashant/droidkit")
                        connection.set("scm:git:git://github.com/er-vprashant/droidkit.git")
                        developerConnection.set("scm:git:ssh://git@github.com/er-vprashant/droidkit.git")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "staging"
                url = uri(stagingDir)
            }
        }
    }

    signing {
        val signingKey = findProperty("signingInMemoryKey")?.toString()
        if (signingKey != null) {
            val keyId = findProperty("signingInMemoryKeyId")?.toString()
            val password = findProperty("signingInMemoryKeyPassword")?.toString() ?: ""
            useInMemoryPgpKeys(keyId, signingKey, password)
            sign(publishing.publications["release"])
        }
    }
}

tasks.register<Zip>("bundleCentralPortal") {
    dependsOn("publishReleasePublicationToStagingRepository")
    from(stagingDir)
    archiveFileName.set("central-portal-bundle.zip")
    destinationDirectory.set(layout.buildDirectory.dir("central-portal"))
}
