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

package services

import connectors.{EtmpConnector, HipConnector}
import play.api.Logging
import play.api.http.Status
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{AWRSFeatureSwitches, Utility}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeRegistrationService @Inject()(etmpConnector: EtmpConnector,
                                      hipConnector: HipConnector)
                                     (implicit ec: ExecutionContext)
  extends Logging {

  private val acknowledgementReference: String = "acknowledgementReference"

  def deRegistration(awrsRefNo: String,
                     deRegistration: JsValue)
                    (implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {

    if (AWRSFeatureSwitches.hipSwitch().enabled) {
      val hipRequestJson: JsResult[JsValue] = updateRequestForHip(deRegistration)
      hipRequestJson match {
        case JsSuccess(transformedRequestJson, _) =>
          hipConnector.deRegister(awrsRefNo, transformedRequestJson) map {
            response =>
              response.status match {
                case Status.CREATED =>
                  HttpResponse(
                    status = Status.OK,
                    body = Json.stringify(Utility.stripSuccessNode(Json.parse(response.body))),
                    headers = response.headers
                  )

                case failedStatusCode =>
                  logger.error(s"[DeRegistrationService][deRegistration] Failure response from HIP endpoint: $failedStatusCode")
                  HttpResponse(
                    status = failedStatusCode,
                    body = response.body,
                    headers = response.headers
                  )
              }
          }
        case JsError(errors) =>
          Future.successful(HttpResponse(
            status = Status.BAD_REQUEST,
            body = s"JSON transformation failed: ${JsError.toJson(errors)}"
          ))
      }
    } else {
      etmpConnector.deRegister(awrsRefNo, deRegistration) map {
        response =>
          response.status match {
            case _ => response
          }
      }
    }
  }

  def updateRequestForHip(deRegistration: JsValue): JsResult[JsObject] = {
    deRegistration.validate[JsObject].map { requestJsObject =>
      val updatedRequest: JsObject = requestJsObject - acknowledgementReference
      updatedRequest
    }
  }
}
