import sbt._

object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val domainVersion = "8.1.0-play-28"
  private val json4sJacksonVersion = "4.0.5"
  private val jsonSchemaValidatorVersion = "2.2.14"
  private val json4sNativeVersion = "4.0.5"
  private val mockitoScalatestVersion = "1.17.7"
  private val microserviceBootstrapVersion = "7.19.0"
  private val playJsonJodaVersion = "2.9.2"
  private val jsoupVersion = "1.15.1"
  private val wiremockJre8Version = "2.33.2"
  private val jacksonModuleVersion = "2.13.3"

  val compile = Seq(
    ws,
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-28"   % microserviceBootstrapVersion,
    "uk.gov.hmrc"                   %% "domain"                      % domainVersion,
    "org.json4s"                    %% "json4s-jackson"              % json4sJacksonVersion,
    "com.github.java-json-tools"    %  "json-schema-validator"       % jsonSchemaValidatorVersion,
    "org.json4s"                    %% "json4s-native"               % json4sNativeVersion,
    "com.typesafe.play"             %% "play-json-joda"              % playJsonJodaVersion,
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"        % jacksonModuleVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[ModuleID] = Seq(
        "uk.gov.hmrc"                   %% "bootstrap-test-play-28"   % microserviceBootstrapVersion    % scope,
        "org.jsoup"                     %  "jsoup"                    % jsoupVersion                    % scope,
        "org.mockito"                   %% "mockito-scala-scalatest"  % mockitoScalatestVersion         % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val scope: String = "it"
      override lazy val test = Seq(
        "uk.gov.hmrc"                   %% "bootstrap-test-play-28"   % microserviceBootstrapVersion    % scope,
        "com.github.tomakehurst"        %  "wiremock-jre8"            % wiremockJre8Version             % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
