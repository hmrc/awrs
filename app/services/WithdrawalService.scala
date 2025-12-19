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

class WithdrawalService @Inject()(metrics: AwrsMetrics, etmpConnector: EtmpConnector, hipConnector: HipConnector)
                                 (implicit ec: ExecutionContext) extends Logging {

  def withdrawal(withdrawalData: JsValue, awrsRefNo: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    if (AWRSFeatureSwitches.hipSwitch().enabled) {
      metrics.startTimer(ApiType.API8Withdrawal)
      val hipRequestJson: JsResult[JsValue] = updateRequestForHip(withdrawalData)

      hipRequestJson match {
        case JsSuccess(updatedRequestJson, _) =>
          hipConnector.withdrawal(awrsRefNo, updatedRequestJson) map {
            response =>
              response.status match {
                case Status.CREATED =>
                  HttpResponse(
                    status = Status.OK,
                    body = Json.stringify(Utility.stripSuccessNode(Json.parse(response.body))),
                    headers = response.headers
                  )

                case failedStatusCode =>
                  logger.error(s"[WithdrawalService][withdrawal] Failure response from HIP endpoint: $failedStatusCode")
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
      metrics.startTimer(ApiType.API8Withdrawal)
      etmpConnector.withdrawal(awrsRefNo, withdrawalData)
    }
  }

  def updateRequestForHip(withdrawalData: JsValue): JsResult[JsObject] = {
      withdrawalData.validate[JsObject].map { requestJsObject =>
        val updatedRequest: JsObject = requestJsObject - "acknowledgementReference"
        updatedRequest
      }
    }
}
