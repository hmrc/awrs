/*
 * Copyright 2017 HM Revenue & Customs
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

import org.joda.time.LocalDate
import play.api.libs.json._
import utils.SessionUtils


case class DeRegistration(deregistrationDate: String, deregistrationReason: String, deregReasonOther: Option[String])

sealed trait DeRegistrationResponseType

case class DeRegistrationType(response: Option[DeRegistrationResponseType])

case class DeRegistrationSuccessResponseType(processingDate: String) extends DeRegistrationResponseType

case class DeRegistrationFailureResponseType(reason: String) extends DeRegistrationResponseType

object DeRegistration {

  implicit val writer = new Writes[DeRegistration] {

    def writes(feModel: DeRegistration): JsValue = {
      val returnJson =
        Json.obj()
          .++(Json.obj("acknowledgementReference" -> SessionUtils.getUniqueAckNo))
          .++(Json.obj("deregistrationDate" -> feModel.deregistrationDate))
          .++(Json.obj("deregistrationReason" -> feModel.deregistrationReason))
          .++(feModel.deregReasonOther.fold(Json.obj())(x => Json.obj("deregReasonOther" -> x)))

      returnJson
    }
  }
  implicit val reader = Json.reads[DeRegistration]
}

object DeRegistrationSuccessResponseType {
  implicit val etmpReader = new Reads[DeRegistrationSuccessResponseType] {

    def reads(js: JsValue): JsResult[DeRegistrationSuccessResponseType] =
      for {
        processingDate <- (js \ "processingDate").validate[String]
      } yield {
        DeRegistrationSuccessResponseType(processingDate = processingDate)
      }

  }

  implicit val etmpWriter = Json.writes[DeRegistrationSuccessResponseType]
}

object DeRegistrationFailureResponseType {
  implicit val etmpReader = new Reads[DeRegistrationFailureResponseType] {

    def reads(js: JsValue): JsResult[DeRegistrationFailureResponseType] =
      for {
        reason <- (js \ "reason").validate[String]
      } yield {
        DeRegistrationFailureResponseType(reason = reason)
      }

  }
  implicit val etmpWriter = Json.writes[DeRegistrationFailureResponseType]
}


object DeRegistrationType {

  implicit val etmpReader = new Reads[DeRegistrationType] {

    def reads(js: JsValue): JsResult[DeRegistrationType] = {
      for {
        successResponse <- js.validateOpt[DeRegistrationSuccessResponseType]
        failureResponse <- js.validateOpt[DeRegistrationFailureResponseType]
      } yield {
        (successResponse, failureResponse) match {
          case (r@Some(_), None) => DeRegistrationType(r)
          case (None, r@Some(_)) => DeRegistrationType(r)
          case _ => DeRegistrationType(None)
        }
      }
    }
  }

  implicit val etmpWriter = new Writes[DeRegistrationType] {
    def writes(info: DeRegistrationType) =
      info.response match {
        case Some(r: DeRegistrationSuccessResponseType) => DeRegistrationSuccessResponseType.etmpWriter.writes(r)
        case Some(r: DeRegistrationFailureResponseType) => DeRegistrationFailureResponseType.etmpWriter.writes(r)
        case _ => Json.obj("unknown" -> "Etmp returned invalid json")
      }
  }

}
