plugins {
    `java-library`
    pklConfigs
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

pklConfigs {
    secret(".*password.*")
    secret(".*secret.*")
    generators {
        register("spring-3.3.0") {
            dependency("org.springframework.boot:spring-boot-autoconfigure:3.3.0")
            dependency("org.springframework.boot:spring-boot:3.3.0")
            dependency("org.springframework.session:spring-session-core:3.3.0")
            dependency("org.springframework:spring-tx:6.1.6")
            dependency("org.springframework:spring-web:6.1.8")
            dependency("org.springframework:spring-core:6.1.8")
            dependency("io.r2dbc:r2dbc-spi:1.0.0.RELEASE")
        }
        register("spring-2.7.0") {
            dependency("org.springframework.boot:spring-boot-autoconfigure:2.7.0")
            dependency("org.springframework.boot:spring-boot:2.7.0")
            dependency("org.springframework.session:spring-session-core:2.7.0")
            dependency("org.springframework:spring-tx:5.3.20")
            dependency("org.springframework:spring-web:5.3.20")
            dependency("org.springframework:spring-core:5.3.20")
            dependency("io.r2dbc:r2dbc-spi:1.0.0.RELEASE")
        }
        register("spring-grpc-client-2.13.1") {
            dependency("net.devh:grpc-client-spring-boot-autoconfigure:2.13.1.RELEASE")
            dependency("org.springframework:spring-core:5.3.20")
        }
        register("spring-grpc-server-2.13.1") {
            dependency("net.devh:grpc-server-spring-boot-autoconfigure:2.13.1.RELEASE")
        }
        register("spring-grpc-client-server-2.13.1") {
            dependency("net.devh:grpc-client-spring-boot-autoconfigure:2.13.1.RELEASE")
            dependency("org.springframework:spring-core:5.3.20")
            dependency("net.devh:grpc-server-spring-boot-autoconfigure:2.13.1.RELEASE")
        }
        register("spring-grpc-server-3.1.0") {
            dependency("net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE")
        }
        register("spring-grpc-client-3.1.0") {
            dependency("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE")
            dependency("org.springframework:spring-core:6.1.6")
        }
        register("spring-grpc-client-server-3.1.0") {
            dependency("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE")
            dependency("org.springframework:spring-core:6.1.6")
            dependency("net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE")
        }
        register("sentry-6.28.0") {
            dependency("io.sentry:sentry-spring-boot:6.28.0")
        }
    }
}