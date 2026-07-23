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

import play.api.libs.json.*
import utils.SessionUtils

case class DeRegistration(deregistrationDate: String, deregistrationReason: String, deregReasonOther: Option[String])

sealed trait DeRegistrationResponseType

case class DeRegistrationType(response: Option[DeRegistrationResponseType])

case class DeRegistrationSuccessResponseType(processingDate: String) extends DeRegistrationResponseType

case class DeRegistrationFailureResponseType(reason: String) extends DeRegistrationResponseType

object DeRegistration {

  given writer: Writes[DeRegistration] = new Writes[DeRegistration] {

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
  given reader: Reads[DeRegistration] = Json.reads[DeRegistration]
}

object DeRegistrationSuccessResponseType {
  given etmpReader: Reads[DeRegistrationSuccessResponseType] = new Reads[DeRegistrationSuccessResponseType] {

    def reads(js: JsValue): JsResult[DeRegistrationSuccessResponseType] =
      for {
        processingDate <- (js \ "processingDate").validate[String]
      } yield {
        DeRegistrationSuccessResponseType(processingDate = processingDate)
      }

  }

  given etmpWriter: OWrites[DeRegistrationSuccessResponseType] = Json.writes[DeRegistrationSuccessResponseType]
}

object DeRegistrationFailureResponseType {
  given etmpReader: Reads[DeRegistrationFailureResponseType] = new Reads[DeRegistrationFailureResponseType] {

    def reads(js: JsValue): JsResult[DeRegistrationFailureResponseType] =
      for {
        reason <- (js \ "reason").validate[String]
      } yield {
        DeRegistrationFailureResponseType(reason = reason)
      }

  }
  given etmpWriter: OWrites[DeRegistrationFailureResponseType] = Json.writes[DeRegistrationFailureResponseType]
}


object DeRegistrationType {

  given etmpReader: Reads[DeRegistrationType] = new Reads[DeRegistrationType] {

    def reads(js: JsValue): JsResult[DeRegistrationType] = {
      for {
        successResponse <- JsSuccess(js.asOpt[DeRegistrationSuccessResponseType](DeRegistrationSuccessResponseType.etmpReader))
        failureResponse <- JsSuccess(js.asOpt[DeRegistrationFailureResponseType](DeRegistrationFailureResponseType.etmpReader))
      } yield {
        (successResponse, failureResponse) match {
          case (r@Some(_), None) => DeRegistrationType(r)
          case (None, r@Some(_)) => DeRegistrationType(r)
          case _ => DeRegistrationType(None)
        }
      }
    }
  }

  given etmpWriter: Writes[DeRegistrationType] = new Writes[DeRegistrationType] {
    def writes(info: DeRegistrationType) =
      info.response match {
        case Some(r: DeRegistrationSuccessResponseType) => DeRegistrationSuccessResponseType.etmpWriter.writes(r)
        case Some(r: DeRegistrationFailureResponseType) => DeRegistrationFailureResponseType.etmpWriter.writes(r)
        case _ => Json.obj("unknown" -> "Etmp returned invalid json")
      }
  }

}
