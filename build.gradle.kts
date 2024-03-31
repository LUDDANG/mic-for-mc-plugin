import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import kr.entree.spigradle.data.Load
import kr.entree.spigradle.kotlin.spigot
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.ir.backend.js.compile

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("kr.entree.spigradle") version "2.4.3"
}

group = "kr.enak.minecraft.plguins"
version = "1.0"

tasks.compileJava.get().options.encoding = "UTF-8"

lateinit var localMaven: MavenArtifactRepository

repositories {
    mavenLocal().also { localMaven = it }
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://maven.maxhenkel.de/repository/public")
}

fun ifExistsInLocal(
    artifactResolve: String,
) = artifactResolve.split(":").let { (group, artifactName, version) ->
    ifExistsInLocal(group, artifactName, version)
}

fun ifExistsInLocal(
    group: String,
    packageName: String,
    version: String,
    classifier: String? = null,
) = listOf(
    group.replace(".", "/"),
    packageName,
    listOfNotNull(version, classifier).joinToString("-")
).joinToString("/").let {
    File(localMaven.url.resolve("$it/$packageName-$version.jar"))
}.exists()

dependencies {
    compileOnly(kotlin("reflect"))
    compileOnly("de.maxhenkel.voicechat:voicechat-api:2.5.0")
    compileOnly(kotlin("stdlib-jdk8")) // Maybe you need to apply the plugin 'shadowJar' for shading 'kotlin-stdlib'.
    compileOnly(spigot("1.20.4"))

    implementation("io.typst:bukkit-kotlin-serialization:1.0.0")
//    implementation(ktor("serialization-kotlinx-json"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    if (ifExistsInLocal("org.spigotmc", "spigot", "1.20.4-R0.1-SNAPSHOT"))
        compileOnly("org.spigotmc:spigot:1.20.4-R0.1-SNAPSHOT")

    if (ifExistsInLocal("org.spigotmc", "spigot", "1.20.4-R0.1-SNAPSHOT", "remapped-mojang"))
        compileOnly("org.spigotmc:spigot:1.20.4-R0.1-SNAPSHOT:remapped-mojang")
}

spigot {
    description = "마이크로 방송을 해보자"
    apiVersion = "1.20"
    depends = listOf("voicechat")
    load = Load.POST_WORLD
    main = "kr.enak.minecraft.plugins.micformc.MicForMC"
    commands {
        create("mfm") {}
        create("micformc") {}
    }
    excludeLibraries = listOf(
        "de.maxhenkel.voicechat"
    )
}

tasks.named("detectSpigotMain") {
    enabled = false
}

val javaVersion = 17
val versionString = JavaLanguageVersion.of(javaVersion).toString()

java {
    withSourcesJar()

    // 17 -> VERSION_17, 1.8 -> VERSION_1_8
    val version = JavaVersion.valueOf("VERSION_" + (versionString.replace(".", "_")))
    sourceCompatibility = version
    targetCompatibility = version
}

tasks.withType(JavaCompile::class.java).configureEach {
    options.release.set(javaVersion)
}

tasks.withType(KotlinCompile::class.java).configureEach {
    kotlinOptions.jvmTarget = versionString
}

val shadowJarTask = tasks.named<ShadowJar>("shadowJar") {
    minimize()
    val matches = listOf(
        "^kotlinx?/.+$",
        "^org/intellij/.+$",
        "^org/jetbrains/.+$",
        "^org/slf4j/.+$",
        "^io/ktor/.+$",
        "^io/typst/bukkit/kotlin/serialization.+$",
    ).map { it.toRegex() }
    exclude { file ->
        return@exclude matches.any { match -> file.path.matches(match) }
    }
}

listOf(
    tasks.named("prepareSpigot"),
    tasks.named("downloadPaper"),
).forEach { task ->
    task {
        onlyIf {
            !projectDir.resolve("debug/spigot/server.jar").exists()
        }
    }
}

tasks.named<Jar>("jar") {
    dependsOn("generateSpigotDescription")

    exclude(".gitkeep")

    finalizedBy(shadowJarTask)
}

tasks.whenTaskAdded {
    if (name == "kaptTestKotlin") {
        dependsOn("generateSpigotDescription")
    }
}

tasks.named("prepareSpigotPlugins") {
    dependsOn("shadowJar")
}
