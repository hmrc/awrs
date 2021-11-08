import sbt._

object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val domainVersion = "6.2.0-play-28"
  private val scalaTestplusPlayVersion = "5.1.0"
  private val pegdownVersion = "1.6.0"
  private val json4sJacksonVersion = "4.0.3"
  private val jsonSchemaValidatorVersion = "2.2.14"
  private val json4sNativeVersion = "4.0.3"
  private val mockitoCoreVersion = "4.0.0"
  private val mockitoScalatestVersion = "1.16.46"
  private val mockito312Version = "3.2.10.0"
  private val microserviceBootstrapVersion = "5.16.0"
  private val playJsonJodaVersion = "2.9.2"
  private val jsoupVersion = "1.14.3"
  private val scalaCheckVersion = "1.15.4"
  private val wiremockJre8Version = "2.31.0"
  private val jacksonModuleVersion = "2.13.0"
  private val flexmarkVersion = "0.35.10"

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
        "org.pegdown"                   %  "pegdown"                  % pegdownVersion                  % scope,
        "com.typesafe.play"             %% "play-test"                % PlayVersion.current             % scope,
        "org.scalatestplus.play"        %% "scalatestplus-play"       % scalaTestplusPlayVersion        % scope,
        "org.scalacheck"                %% "scalacheck"               % scalaCheckVersion               % scope,
        "org.jsoup"                     %  "jsoup"                    % jsoupVersion                    % scope,
        "org.json4s"                    %% "json4s-jackson"           % json4sJacksonVersion            % scope,
        "org.json4s"                    %% "json4s-native"            % json4sNativeVersion             % scope,
        "org.mockito"                   %  "mockito-core"             % mockitoCoreVersion              % scope,
        "org.mockito"                   %% "mockito-scala-scalatest"  % mockitoScalatestVersion         % scope,
        "org.scalatestplus"             %% "mockito-3-12"             % mockito312Version               % scope,
        "com.vladsch.flexmark"          %  "flexmark-all"             % flexmarkVersion                 % scope


      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "org.pegdown"                   %  "pegdown"                  % pegdownVersion                  % scope,
        "com.typesafe.play"             %% "play-test"                % PlayVersion.current             % scope,
        "org.scalatestplus.play"        %% "scalatestplus-play"       % scalaTestplusPlayVersion        % scope,
        "com.github.tomakehurst"        %  "wiremock-jre8"            % wiremockJre8Version             % scope,
        "org.scalatestplus"             %% "mockito-3-12"             % mockito312Version               % scope,
        "com.vladsch.flexmark"          %  "flexmark-all"             % flexmarkVersion                 % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
