/* Copyright 2016 HM Revenue & Customs
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License. */

import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "awrs"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val microserviceBootstrapVersion = "10.4.0"
  private val domainVersion = "5.3.0"
  private val hmrcTestVersion = "3.8.0-play-26"
  private val scalaTestplusPlayVersion = "3.1.2"
  private val pegdownVersion = "1.6.0"
  private val json4sJacksonVersion = "3.6.5"
  private val jsonSchemaValidatorVersion = "2.2.6"
  private val json4sNativeVersion = "3.6.5"
  private val mockitoAllVersion = "1.10.19"
  private val webbitServerVersion = "0.4.15"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "0.39.0",
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "org.json4s" %% "json4s-jackson" % json4sJacksonVersion,
    "com.github.fge" % "json-schema-validator" % jsonSchemaValidatorVersion,
    "org.json4s" %% "json4s-native" %json4sNativeVersion,
    "com.typesafe.play" %% "play-json-joda" % "2.6.13"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestplusPlayVersion % scope,
        "org.jsoup" % "jsoup" % "1.11.3" % scope,
        "org.json4s" %% "json4s-jackson" % json4sJacksonVersion,
        "com.github.fge" % "json-schema-validator" % jsonSchemaValidatorVersion,
        "org.json4s" %% "json4s-native" % json4sNativeVersion,
        "org.mockito" % "mockito-all" % mockitoAllVersion,
        "org.webbitserver" % "webbit" % webbitServerVersion
      )
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestplusPlayVersion % scope
      )
    }.test
  }

  def apply() = compile ++ Test() ++ IntegrationTest()
}

