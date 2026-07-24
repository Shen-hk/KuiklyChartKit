plugins {
    //trick: for the same plugin versions in all sub-modules
    id("com.android.application").version("7.4.2").apply(false)
    id("com.android.library").version("7.4.2").apply(false)
    kotlin("android").version("2.1.21").apply(false)
    kotlin("multiplatform").version("2.1.21").apply(false)
    id("com.google.devtools.ksp").version("2.1.21-2.0.1").apply(false)

}

buildscript {
    dependencies {
        classpath(BuildPlugin.kuikly)
        // Kotlin 2.1 class files require D8/R8 8.6.17+. Keep the official
        // Kuikly AGP 7.4.2 baseline while overriding only its bundled compiler.
        classpath("com.android.tools:r8:8.6.17")
    }
}
