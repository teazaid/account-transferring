name := "account-transfering"

version := "1.0"

scalaVersion := "2.11.8"


val CirceVersion = "0.8.0"
val AkkaCirceVersion = "1.17.0"
val AkkaHttpVersion = "10.0.9"
val H2Version = "1.4.196"
val ScalikeJdbcVersion = "3.0.2"
val LogbackVersion = "1.2.3"
val ScalaLogginVersion = "3.7.2"
val MockitoVersion = "1.9.5"

libraryDependencies ++= List(
  "de.heikoseeberger" %% "akka-http-circe" % AkkaCirceVersion,
  "org.scalikejdbc" %% "scalikejdbc-config" % ScalikeJdbcVersion,
  "io.circe" % "circe-core_2.11" % CirceVersion,
  "io.circe" % "circe-generic_2.11" % CirceVersion,
  "io.circe" % "circe-parser_2.11" % CirceVersion,
  "com.typesafe.akka" % "akka-http_2.11" % AkkaHttpVersion,
  "org.scalatest" % "scalatest_2.11" % "3.2.0-SNAP7" % "test",
  "com.h2database" % "h2" % H2Version,
  "org.scalikejdbc" % "scalikejdbc_2.11" % ScalikeJdbcVersion,
  "ch.qos.logback" % "logback-classic" % LogbackVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % ScalaLogginVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % AkkaHttpVersion % "test",
  "org.mockito" % "mockito-all" % MockitoVersion % "test")

