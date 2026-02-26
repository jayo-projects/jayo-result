[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?logo=apache&style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0)
[![Version](https://img.shields.io/maven-central/v/dev.jayo/jayo-result?logo=apache-maven&color=&style=flat-square)](https://search.maven.org/artifact/dev.jayo/jayo-result)
[![Java](https://img.shields.io/badge/Java-11-ED8B00?logo=openjdk&logoColor=white&style=flat-square)](https://www.java.com/en/download/help/whatis_java.html)

# Jayo result

Since the JVM doesn't provide its own buitin Result type, Jayo result provides a Java port of the `Result<T>` type from
the Kotlin stdlib.

It is available on Maven Central.

Gradle:
```groovy
dependencies {
    implementation("dev.jayo:jayo-result:X.Y.Z")
}
```

Maven:
```xml

<dependency>
    <groupId>dev.jayo</groupId>
    <artifactId>jayo-result</artifactId>
    <version>X.Y.Z</version>
</dependency>
```

The Jayo result code is written in Java without the use of any external dependencies, to be as light as possible.

Jayo result requires Java 11 or more recent.

*Contributions are very welcome, simply clone this repo and submit a PR when your fix, new feature, or optimization is
ready!*

## License

[Apache-2.0](https://opensource.org/license/apache-2-0)

Copyright (c) 2026-present, pull-vert and Jayo contributors
