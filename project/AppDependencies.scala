/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt.*

object AppDependencies {
  import play.sbt.PlayImport.*

  private val domainVersion = "11.0.0"
  private val json4sJacksonVersion = "4.0.7"
  private val jsonSchemaValidatorVersion = "2.2.14"
  private val json4sNativeVersion = "4.0.7"
  private val mockitoScalatestVersion = "2.0.0"
  private val microserviceBootstrapVersion = "10.4.0"
  private val jsoupVersion = "1.21.2"
  private val jacksonModuleVersion = "2.20.1"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"                  %% "bootstrap-backend-play-30" % microserviceBootstrapVersion,
    "uk.gov.hmrc"                  %% "domain-play-30"            % domainVersion,
    "org.json4s"                   %% "json4s-jackson"            % json4sJacksonVersion,
    "com.github.java-json-tools"   %  "json-schema-validator"     % jsonSchemaValidatorVersion,
    "org.json4s"                   %% "json4s-native"             % json4sNativeVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala"      % jacksonModuleVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30"  % microserviceBootstrapVersion % Test,
    "org.jsoup"   %  "jsoup"                   % jsoupVersion                 % Test,
    "org.mockito" %% "mockito-scala-scalatest" % mockitoScalatestVersion      % Test
  )

  val itDependencies: Seq[ModuleID] = Seq()

  def apply(): Seq[ModuleID] = compile ++ test
}
