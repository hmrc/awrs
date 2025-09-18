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

import play.api.libs.json._

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.collection.immutable.ListMap
import scala.language.implicitConversions

object Utility {

  private val success: String = "success"

  def awrsToEtmpDateFormatter(stringDate: String): String = toAndFromDateFormat(stringDate, "yyyy-MM-dd", "dd/MM/yyyy")

  def etmpToAwrsDateFormatter(date: String): String = toAndFromDateFormat(date, "dd/MM/yyyy", "yyyy-MM-dd")

  def formatOptionalDate(optionalDate: Option[String])(f: String => String): Option[String] = optionalDate.map(f)

  def toAndFromDateFormat(date: String, toFormat: String, fromFormat: String): String =
    LocalDate.parse(date, DateTimeFormatter.ofPattern(fromFormat)).format(DateTimeFormatter.ofPattern(toFormat))

  def etmpToAwrsDateFormatterOrNone(date: Option[String]): Option[String] = date.map(etmpToAwrsDateFormatter)

  def booleanToString: Boolean => Some[String] = (s: Boolean) => s match {
    case true => Some("Yes")
    case false => Some("No")
  }

  def trueToNoOrFalseToYes: Boolean => Some[String] = (s: Boolean) => s match {
    case true => Some("No")
    case false => Some("Yes")
  }

  def stripSuccessNode(hipResponsePayload: JsValue): JsObject = {
    val successNodeLookup: JsLookupResult = hipResponsePayload \ success
    if (successNodeLookup.isDefined) {
      successNodeLookup.as[JsObject]
    } else {
      throw new RuntimeException(s"Received response does not contain a '$success' node.")
    }
  }

  //  def removeFields(fields: List[String], parentObject: JsObject): JsObject = fields
  //    .foldLeft(parentObject) { (jsObj, field) =>
  //      (jsObj \ field).toOption match {
  //        case Some(_) => jsObj - field
  //        case None => throw new RuntimeException(s"Received response is missing the '$field' key in the 'success' node.")
  //      }
  //    }

  def removeFields(fields: List[String], parentObject: JsObject): JsObject = fields
    .foldLeft(parentObject) { (jsObj, field) =>
      val targetPath = parseJsPath(field)

      jsObj.validate(targetPath.json.prune) match {
        case JsSuccess(result, _) => result
        case JsError(errors) => throw new RuntimeException(s"Failed to delete field '$field': $errors")
      }
    }

  // Map contains the before and after value of the key
  //  def alterFields(fields: Map[String, String], parentObject: JsObject): JsObject = fields
  //    .foldLeft(parentObject) { (jsObj, field) =>
  //      (jsObj \ field._1).toOption match {
  //        case Some(value) => (jsObj + (field._2 -> value)) - field._1
  //        case None => throw new RuntimeException(s"Received response is missing the '${field._1}' key in the 'success' node.")
  //      }
  //    }

  def alterFieldKeys(fieldKeys: ListMap[String, String], parentObject: JsObject): JsObject = {
    fieldKeys
      .foldLeft(parentObject) { (jsObj, fieldKey) =>
        val sourcePath = parseJsPath(fieldKey._1)
        val targetPath = parseJsPath(fieldKey._2)

        (for {
          picked <- jsObj.validate(sourcePath.json.pick)
          transformed <- jsObj.transform(__.json.update(targetPath.json.put(picked)))
          pruned <- transformed.validate(sourcePath.json.prune)
        } yield pruned) match {
          case JsSuccess(result, _) => result
          case JsError(errors) => throw new RuntimeException(s"Failed to alter field '${fieldKey._1}' to '${fieldKey._2}': $errors")
        }
      }
  }

  private def parseJsPath(pathStr: String): JsPath = pathStr.split("\\.").foldLeft(JsPath())(_ \ _)

}

case class Bool(b: Boolean) {
  def ?[X](t: => X) = new {
    def |(f: => X) = if (b) t else f
  }
}

object Bool {
  implicit def BooleanBool(b: Boolean): Bool = Bool(b)
}
