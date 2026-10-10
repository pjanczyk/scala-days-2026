//> using scala 3.9.0
//> using jvm 25
//> using options -deprecation -feature -Wunused:privates,locals
//> using resourceDir src/main/resources
//> using test.resourceDir src/test/resources
//> using javaOptions --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow

// HTTP server + OpenAPI docs
//> using dep com.softwaremill.sttp.tapir::tapir-netty-server-sync:1.13.33
//> using dep com.softwaremill.sttp.tapir::tapir-jsoniter-scala:1.13.33
//> using dep com.softwaremill.sttp.tapir::tapir-swagger-ui-bundle:1.13.33
// Concurrency, resilience, streaming
//> using dep com.softwaremill.ox::core:1.0.9
// HTTP client
//> using dep com.softwaremill.sttp.client4::core:4.0.27
//> using dep com.softwaremill.sttp.client4::jsoniter:4.0.27
//> using dep com.softwaremill.sttp.client4::slf4j-backend:4.0.27
//> using dep com.softwaremill.sttp.client4::prometheus-backend:4.0.27
// JSON
//> using dep com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core:2.41.3
//> using dep com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:2.41.3
// SQL + migrations
//> using dep ma.chinespirit::parlance:0.1.0
//> using dep com.h2database:h2:2.3.232
//> using dep org.flywaydb:flyway-core:13.10.0
// Logging
//> using dep ch.qos.logback:logback-classic:1.6.5
//> using dep com.typesafe.scala-logging::scala-logging:3.9.6
// Observability: Prometheus metrics, OpenTelemetry tracing (trace ids in logs, propagated across threads and services)
//> using dep com.softwaremill.sttp.tapir::tapir-prometheus-metrics:1.13.33
//> using dep com.softwaremill.sttp.tapir::tapir-opentelemetry-tracing:1.13.33
//> using dep com.softwaremill.sttp.client4::opentelemetry-backend:4.0.27
//> using dep com.softwaremill.ox::otel-context:1.0.9
//> using dep io.opentelemetry:opentelemetry-sdk:1.66.0
//> using dep io.opentelemetry.instrumentation:opentelemetry-logback-mdc-1.0:2.32.0-alpha

//> using test.dep org.scalameta::munit:1.3.6
//> using test.dep com.softwaremill.sttp.tapir::tapir-sttp-stub4-server:1.13.33
