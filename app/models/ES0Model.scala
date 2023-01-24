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

package models

import play.api.libs.json.{JsResult, JsValue, Json, OFormat, Reads, Writes}

case class ES0SuccessResponse(principalUserIDList: List[String], delegatedUserIDList: List[String]) extends ES0Response

case class ES0NoContentResponse(noContentResponse: String) extends ES0Response

case class ES0FailureResponse(status: String, reason: String) extends ES0Response

sealed trait ES0Response

object ES0SuccessResponse {
  implicit val format: OFormat[ES0SuccessResponse] = Json.format[ES0SuccessResponse]
}

object ES0FailureResponse {

  implicit val reader: Reads[ES0FailureResponse] = new Reads[ES0FailureResponse] {

    def reads(js: JsValue): JsResult[ES0FailureResponse] = {
      for {
        code <- (js \ "code").validate[String]
        message <- (js \ "message").validate[String]
      } yield {
        ES0FailureResponse(code, message)
      }
    }
  }
  implicit val formats: OFormat[ES0FailureResponse] = Json.format[ES0FailureResponse]
}