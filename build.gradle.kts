plugins {
    application
    checkstyle
    war
    id("com.github.ben-manes.versions") version "0.52.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
//    id("org.gretty") version "5.0.2"
//        id("se.patrikerdes.use-latest-versions") version "0.2.18"
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
//    compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")
//    implementation("org.gretty:gretty:5.0.2")
//    implementation("org.gretty:org.gretty.gradle.plugin:5.0.2")

    // Log
//    implementation("org.slf4j:slf4j-simple:$2.1.0-alpha1")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")

    // DB section
//    implementation("com.zaxxer:HikariCP:6.3.0") // HikariCP
    implementation("org.xerial:sqlite-jdbc:3.51.3.0")  //SQLite JDBC driver
//    implementation("org.xerial:sqlite-jdbc:3.51.3.0") // Используйте актуальную версию


//    implementation("org.slf4j:slf4j-simple:2.0.17")

    // LOMBOK
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    testCompileOnly("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")

    // Jackson Databind
    implementation("tools.jackson.core:jackson-databind:3.1.1")


//    implementation("com.fasterxml.jackson.core:jackson-databind:3.1.0")


    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

//tasks.withType(JavaCompile) {
//    options.compilerArgs << "-Xlint:unchecked" << "-Xlint:deprecation"
//    options.encoding = 'UTF-8'
//}

checkstyle {
    toolVersion = "10.26.1"
    configFile = file("config/checkstyle/checkstyle.xml")
}

//gretty {
//    httpPort = 8081
//  contextPath = '/MyWebApp'
//    integrationTestTask = "test"
//    contextPath = '/'
//    servletContainer = "tomcat10"
//}


tasks.test {
    useJUnitPlatform()
}

tasks.war {
    webAppDirectory = file("src/main/webapp")
//    from("src/rootContent") // adds a file-set to the root of the archive
//    webInf { from("src/additionalWebInf") } // adds a file-set to the WEB-INF dir.
//    webXml = file("src/someWeb.xml") // copies a file to WEB-INF/web.xml
}

