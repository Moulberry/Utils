plugins {
	id("java")
	id("java-library")
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

allprojects {
    group = "net.moulberry.utils"
    version = "1.0.0"
}

repositories {
	mavenCentral()
}

dependencies {
	compileOnlyApi("org.jetbrains:annotations:22.0.0")

	testImplementation("com.google.guava:guava-testlib:31.0.1-jre")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
	testImplementation("com.google.truth:truth:1.1.3")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
