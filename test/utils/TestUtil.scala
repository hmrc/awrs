/*
 * Copyright 2023 HM Revenue & Customs
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

package utils

import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchemaFactory
import play.api.libs.json._
import utils.AwrsTestJson._

import scala.io.Source

object TestUtil extends TestUtil

trait TestUtil {

  lazy val dummyDataMap = Map(f"$$nino" -> testNino, f"$$utr" -> testUtr, f"$$grpJoinDate" -> testGrpJoinDate, f"$$testRefNo" -> testRefNo)

  def load(path: String): String = {
    Source.fromURL(getClass.getResource(path)).mkString
  }

  def loadWithDummy(path: String): String = loadAndReplace(path, dummyDataMap)

  def loadAndParseJson(path: String): JsValue = {
    Json.parse(load(path))
  }

  def loadAndParseJsonWithDummyData(path: String): JsValue = {
    Json.parse(loadAndReplace(path, dummyDataMap))
  }

  def loadAndReplace(path: String, replaceMap: Map[String, String]): String = {
    var jsonString = Source.fromURL(getClass.getResource(path)).mkString
    for ((key, value) <- replaceMap) {
      jsonString = jsonString.replace(key, value)
    }
    jsonString
  }

  def loadReplaceAndParseJson(path: String, replaceMap: Map[String, String]): JsValue = {
    Json.parse(loadAndReplace(path, replaceMap))
  }

  def updateJson(updatesToJson: JsObject, actualJson: String): String = {
    val awrsModelJson = Json.parse(actualJson)

    val jsonTransformer = (__).json.update(__.read[JsObject].map {
      o => o.deepMerge(updatesToJson)
    })
    val invalidJson = awrsModelJson.validate(jsonTransformer).get
    invalidJson.toString()
  }

  def deleteFromJson(pathToDelete: JsPath, actualJson: String): String = {
    val awrsModelJson = Json.parse(actualJson)

    val jsonTransformer = pathToDelete.json.prune
    val invalidJson = awrsModelJson.validate(jsonTransformer).get
    invalidJson.toString()
  }

  def validateJson(schemaPath: String, jsonInstance: String): Boolean = {
    val json: JsonNode = JsonLoader.fromString(jsonInstance)
    val factory = JsonSchemaFactory.byDefault.getJsonSchema(JsonLoader.fromResource(schemaPath))
    factory.validate(json).isSuccess
  }
}
