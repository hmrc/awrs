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

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{AWRSFeatureSwitches, Utility}

import scala.collection.immutable.ListMap
import scala.util.{Failure, Success, Try}

private object StatusService {
  // HIP: response field
  private val processingDateTime = "processingDateTime"
  private val deregistrationDate = "deregistrationDate"
  private val businessContactNumber = "businessContactNumber"
  private val safeid = "safeid"

  // DES: response field
  private val processingDate = "processingDate"
  private val safeId = "safeId"
}

class StatusService @Inject()(etmpConnector: EtmpConnector, hipConnector: HipConnector)(implicit ec: ExecutionContext) extends Logging {

  import StatusService._

  def checkStatus(awrsRefNo: String)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    if (AWRSFeatureSwitches.hipSwitch().enabled) {
      hipConnector.status(awrsRefNo) map {
        case HttpResponse(Status.OK, body, headers) =>
          Try(updateResponseForHip(Json.parse(body))) match {
            case Success(value) =>
              HttpResponse(
                status = Status.OK,
                body = Json.stringify(value),
                headers = headers
              )
            case Failure(ex) =>
              HttpResponse(
                status = Status.BAD_REQUEST,
                body = s"JSON transformation failed: ${ex.toString}"
              )
          }
        case HttpResponse(failedStatusCode, body, headers) =>
          logger.error(s"[DeRegistrationService][deRegistration] Failure response from HIP endpoint: $failedStatusCode")
          HttpResponse(
            status = failedStatusCode,
            body = body,
            headers = headers
          )
      }
    } else {
      etmpConnector.checkStatus(awrsRefNo) map {
        response =>
          response.status match {
            case _ => response
          }
      }
    }
  }

  private def updateResponseForHip(responseJson: JsValue): JsValue = {
    val successJsObject = Utility.stripSuccessNode(responseJson)
    val updatedJsObject = Utility.alterFieldKeys(ListMap(processingDateTime -> processingDate, safeid -> safeId), successJsObject)
    Utility.removeFields(List(deregistrationDate, businessContactNumber), updatedJsObject)
  }

}
