plugins {
    id("java")
}

group = "org.pvlov"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        setUrl("https://m2.dv8tion.net/releases")
    }
    maven {
        setUrl("https://jitpack.io")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation ("org.javacord:javacord:3.8.0")
    implementation ("dev.arbjerg:lavaplayer:2.0.2")
    implementation ("org.javatuples:javatuples:1.2")
}

tasks.test {
    useJUnitPlatform()
}