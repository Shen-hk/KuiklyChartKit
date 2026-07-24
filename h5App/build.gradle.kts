import org.gradle.api.tasks.Sync

plugins {
    kotlin("multiplatform")
}

val generatedChartResources = layout.buildDirectory.dir("generated/chartResources")
val chartBundleJs = rootProject.layout.projectDirectory.file(
    "chartkit/build/dist/js/productionExecutable/nativevue2.js"
)
val chartAssets = rootProject.layout.projectDirectory.dir("chartkit/build/outputs/kuikly/assets")

kotlin {
    js(IR) {
        browser {
            webpackTask {
                outputFileName = "h5App.js"
            }
            commonWebpackConfig {
                output?.library = null
            }
        }
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            resources.srcDir(generatedChartResources)
            dependencies {
                implementation(
                    "com.tencent.kuikly-open.core-render-web:base:${Version.getKuiklyVersion()}"
                )
                implementation(
                    "com.tencent.kuikly-open.core-render-web:h5:${Version.getKuiklyVersion()}"
                )
            }
        }
    }
}

/**
 * Kuikly Web Render and the business bundle are two different JavaScript products.
 * Keep the official loading order by putting nativevue2.js in the host resources,
 * where index.html loads it before h5App.js.
 */
val syncChartBundle by tasks.registering(Sync::class) {
    group = "kuikly"
    description = "Builds and embeds the ChartKit Kuikly business bundle into the H5 host."
    dependsOn(":chartkit:jsBrowserDistribution")

    doFirst {
        check(chartBundleJs.asFile.isFile) {
            "Chart bundle was not generated: ${chartBundleJs.asFile.absolutePath}"
        }
    }

    from(chartBundleJs) {
        into("page")
    }
    from(chartAssets) {
        into("assets")
    }
    into(generatedChartResources)
}

tasks.matching { it.name == "jsProcessResources" }.configureEach {
    dependsOn(syncChartBundle)
}

tasks.register("publishChartShowcase") {
    group = "kuikly"
    description = "Builds the self-contained production H5 ChartKit showcase."
    dependsOn("jsBrowserDistribution")
}
