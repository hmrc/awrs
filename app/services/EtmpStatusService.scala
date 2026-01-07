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
import play.api.libs.json.Json

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{AWRSFeatureSwitches, Utility}

class EtmpStatusService @Inject()(etmpConnector: EtmpConnector, hipConnector: HipConnector) extends Logging {

  def checkStatus(awrsRefNo: String)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
    if (AWRSFeatureSwitches.hipSwitch().enabled) {
      hipConnector.checkStatus(awrsRefNo) map {
        response =>
          response.status match {
            case Status.OK =>
              HttpResponse(
                status = Status.OK,
                body = Json.stringify(Utility.stripSuccessNode(Json.parse(response.body))),
                headers = response.headers
              )
            case failedStatusCode =>
              logger.error(s"[EtmpStatusService][checkStatus] Failure response from HIP endpoint: $failedStatusCode, body=${response.body}")
              HttpResponse(
                status = failedStatusCode,
                body = response.body,
                headers = response.headers
              )
          }
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
