plugins {
    `java-library`
}

group = "org.nycfirst"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Processing core, provided by Processing at runtime (path set on Day 1)
    // compileOnly(files("/Applications/Processing.app/.../core.jar"))
}
