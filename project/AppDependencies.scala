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

object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val domainVersion = "5.9.0-play-26"
  private val hmrcTestVersion = "3.9.0-play-26"
  private val scalaTestplusPlayVersion = "4.0.3"
  private val pegdownVersion = "1.6.0"
  private val json4sJacksonVersion = "3.6.9"
  private val jsonSchemaValidatorVersion = "2.2.6"
  private val json4sNativeVersion = "3.6.9"
  private val mockitoCoreVersion = "3.3.3"
  private val webbitServerVersion = "0.4.15"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-27" % "2.23.0",
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "org.json4s" %% "json4s-jackson" % json4sJacksonVersion,
    "com.github.fge" % "json-schema-validator" % jsonSchemaValidatorVersion,
    "org.json4s" %% "json4s-native" %json4sNativeVersion,
    "com.typesafe.play" %% "play-json-joda" % "2.6.14"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestplusPlayVersion % scope,
        "org.scalatest" %% "scalatest" % "3.0.8" % scope,
        "org.scalacheck" %% "scalacheck" % "1.14.3" % scope,
        "org.jsoup" % "jsoup" % "1.11.3" % scope,
        "org.json4s" %% "json4s-jackson" % json4sJacksonVersion,
        "com.github.fge" % "json-schema-validator" % jsonSchemaValidatorVersion,
        "org.json4s" %% "json4s-native" % json4sNativeVersion,
        "org.mockito" % "mockito-core" % mockitoCoreVersion,
        "org.webbitserver" % "webbit" % webbitServerVersion
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestplusPlayVersion % scope,
        "com.github.tomakehurst" % "wiremock-jre8" % "2.23.2" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}

