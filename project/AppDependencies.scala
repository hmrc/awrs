import sbt._

object AppDependencies {
  import play.sbt.PlayImport._

  private val domainVersion = "9.0.0"
  private val json4sJacksonVersion = "4.0.7"
  private val jsonSchemaValidatorVersion = "2.2.14"
  private val json4sNativeVersion = "4.0.7"
  private val mockitoScalatestVersion = "1.17.30"
  private val microserviceBootstrapVersion = "8.4.0"
  private val jsoupVersion = "1.17.2"
  private val wiremockVersion = "3.3.1"
  private val jacksonModuleVersion = "2.16.1"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-30"   % microserviceBootstrapVersion,
    "uk.gov.hmrc"                   %% "domain-play-30"              % domainVersion,
    "org.json4s"                    %% "json4s-jackson"              % json4sJacksonVersion,
    "com.github.java-json-tools"    %  "json-schema-validator"       % jsonSchemaValidatorVersion,
    "org.json4s"                    %% "json4s-native"               % json4sNativeVersion,
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"        % jacksonModuleVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30"  % microserviceBootstrapVersion % "test",
    "org.jsoup"   %  "jsoup"                   % jsoupVersion                 % "test",
    "org.mockito" %% "mockito-scala-scalatest" % mockitoScalatestVersion      % "test"
  )

  val itDependencies: Seq[ModuleID] = Seq(
    "org.wiremock" % "wiremock" % wiremockVersion % Test
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
