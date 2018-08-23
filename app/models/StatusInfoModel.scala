/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.Play
import play.api.libs.json.Reads._
import play.api.libs.json.{Json, _}
import uk.gov.hmrc.play.config.RunMode
import utils.DecodeText._
import utils.StripDataTags._
import utils.ReplaceNewlineCharacters._

sealed trait StatusInfoResponseType

case class StatusInfoType(response: Option[StatusInfoResponseType])

case class StatusInfoSuccessResponseType(processingDate: String, secureCommText: String) extends StatusInfoResponseType

case class StatusInfoFailureResponseType(reason: String) extends StatusInfoResponseType


object StatusInfoSuccessResponseType extends RunMode {

  implicit val reader = new Reads[StatusInfoSuccessResponseType] {

    def reads(js: JsValue): JsResult[StatusInfoSuccessResponseType] = {
      for {
        processingDate <- (js \ "processingDate").validate[String]
        ecodedSecureCommText <- (js \ "secureCommText").validate[String]
      } yield {
        val secureCommText = stripCData(stripOtherCharacters(replaceNewlineWithHtmlBr(decodeBase64Text(ecodedSecureCommText))))

        StatusInfoSuccessResponseType(processingDate = processingDate, secureCommText = secureCommText)
      }
    }
  }

  implicit val writer = Json.writes[StatusInfoSuccessResponseType]
}

object StatusInfoFailureResponseType {

  implicit val reader = new Reads[StatusInfoFailureResponseType] {

    def reads(js: JsValue): JsResult[StatusInfoFailureResponseType] = {
      for {
        reason <- (js \ "reason").validate[String]
      } yield {
        StatusInfoFailureResponseType(reason = reason)
      }
    }
  }
  implicit val writer = Json.writes[StatusInfoFailureResponseType]
}


object StatusInfoType {

  implicit val reader = new Reads[StatusInfoType] {

    def reads(js: JsValue): JsResult[StatusInfoType] = {
      for {
        successResponse <- JsSuccess(js.asOpt[StatusInfoSuccessResponseType](StatusInfoSuccessResponseType.reader))
        failureResponse <- JsSuccess(js.asOpt[StatusInfoFailureResponseType](StatusInfoFailureResponseType.reader))
      } yield {
        (successResponse, failureResponse) match {
          case (r@Some(_), None) => StatusInfoType(r)
          case (None, r@Some(_)) => StatusInfoType(r)
          case _ => StatusInfoType(None)
        }
      }
    }
  }

  implicit val writer = new Writes[StatusInfoType] {
    def writes(info: StatusInfoType) =
      info.response match {
        case Some(r: StatusInfoSuccessResponseType) => StatusInfoSuccessResponseType.writer.writes(r)
        case Some(r: StatusInfoFailureResponseType) => StatusInfoFailureResponseType.writer.writes(r)
        case _ => Json.obj("unknown" -> "Etmp returned invalid json")
      }
  }

}
