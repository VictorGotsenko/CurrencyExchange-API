plugins {
    application
    checkstyle
    war
    id("com.github.ben-manes.versions") version "0.52.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
//    gretty
    id("org.gretty") version "5.0.2"
}

group = "currencyexchange.code"
version = "1.0-SNAPSHOT"

application {
    mainClass = "currencyexchange.Main"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("jakarta.servlet:jakarta.servlet-api:6.1.0")

    // Log
    implementation("org.slf4j:slf4j-simple:2.0.17")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")

    // DB section
    implementation("org.xerial:sqlite-jdbc:3.51.3.0")  //SQLite JDBC driver
    implementation("com.zaxxer:HikariCP:7.0.2")        // Pool

    // LOMBOK
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    testCompileOnly("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")

    // Jackson Databind
    implementation("tools.jackson.core:jackson-databind:3.1.1")

    //Gretty
    implementation("org.gretty:gretty-runner-jetty:5.0.2")
}

checkstyle {
    toolVersion = "10.26.1"
    configFile = file("config/checkstyle/checkstyle.xml")

}

// Gretty configuration block
gretty {
    managedClassReload = false

    // Port to run the server on (default is 8080)
//    httpPort = 8080

    // The web application context path (e.g., http://localhost:8081/myapp)
    contextPath = "/"

    // Choose your servlet container: "jetty9", "jetty10", "tomcat9", etc.
    servletContainer = "tomcat11"

    // Automatic reloading when files change
//    managedClassReload = true
}



tasks.test {
    useJUnitPlatform()
}

tasks.war {
    webAppDirectory = file("src/main/webapp")
}

