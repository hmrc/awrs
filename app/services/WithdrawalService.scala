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

import javax.inject.Inject
import metrics.AwrsMetrics
import models.ApiType
import play.api.Logging
import play.api.http.Status
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{AWRSFeatureSwitches, Utility}

private object WithdrawalService {

  // HIP: response fields
  private val processingDateTime: String = "processingDateTime"

  // DES: request fields
  private val withdrawalReason = "withdrawalReason"
  private val acknowledgementReference = "acknowledgementReference"

  // DES: response field
  private val processingDate: String = "processingDate"

  private val withdrawalReasonCodes = Map(
    "Applied in error" -> "01",
    "No Longer trading" -> "02",
    "Duplicate Application" -> "03",
    "Joined AWRS Group" -> "04",
    "Others" -> "99"
  )
}

class WithdrawalService @Inject()(metrics: AwrsMetrics,
                                  etmpConnector: EtmpConnector,
                                  hipConnector: HipConnector)(implicit ec: ExecutionContext) extends Logging {

  import WithdrawalService._

  def withdrawal(data: JsValue, awrsRefNo: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    metrics.startTimer(ApiType.API8Withdrawal)
    if (AWRSFeatureSwitches.hipSwitch().enabled) {
      val hipRequestJson: JsResult[JsValue] = updateRequestForHip(data)

      hipRequestJson match {
        case JsSuccess(transformedRequestJson, _) =>
          hipConnector.withdrawal(awrsRefNo, transformedRequestJson) map {
            case HttpResponse(Status.CREATED, body, headers) =>
              HttpResponse(status = Status.OK, body = Json.stringify(updateResponseForHip(Json.parse(body))), headers = headers)
            case HttpResponse(failedStatusCode, body, headers) =>
              logger.error(s"[DeRegistrationService][deRegistration] Failure response from HIP endpoint: $failedStatusCode")
              HttpResponse(
                status = failedStatusCode,
                body = body,
                headers = headers
              )
          }
        case JsError(errors) =>
          Future.successful(HttpResponse(
            status = Status.BAD_REQUEST,
            body = s"JSON transformation failed: ${JsError.toJson(errors)}"
          ))
      }

    } else {
      etmpConnector.withdrawal(awrsRefNo, data)
    }
  }

  private def updateRequestForHip(data: JsValue): JsResult[JsObject] =
    (data \ withdrawalReason).validate[String].flatMap { reasonString =>
      val reasonCode = withdrawalReasonCodes.getOrElse(reasonString, throw new NoSuchElementException("Invalid deregistration code received"))

      data.validate[JsObject].map { requestJsObject =>
        requestJsObject + (withdrawalReason -> JsString(reasonCode)) - acknowledgementReference
      }
    }

  private def updateResponseForHip(responseJson: JsValue): JsValue = {
    val successJsObject: JsObject = Utility.stripSuccessNode(responseJson)

    (successJsObject \ processingDateTime).toOption match {
      case Some(processingDateTimeValue) => (successJsObject + (processingDate -> processingDateTimeValue)) - processingDateTime
      case None => throw new RuntimeException(s"Received response is missing the '$processingDateTime' key in the 'success' node.")
    }
  }

}
