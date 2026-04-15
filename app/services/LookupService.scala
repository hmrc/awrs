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

import connectors.HipConnector
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.Utility.logger
import utils.Utility

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LookupService @Inject()(hipConnector: HipConnector)
                             (implicit ec: ExecutionContext) {

  def lookupApplication(awrsRefNo: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {

    hipConnector.lookup(awrsRefNo) map {
      response: HttpResponse =>
        response.status match {
          case Status.OK =>
            val responseBody = Utility.mapCrnForResponseModel(response.json)
            HttpResponse(
              status = Status.OK,
              body = Json.stringify(Utility.stripSuccessNode(responseBody)),
              headers = response.headers
            )

          case failedStatusCode =>
            logger.error(s"[LookupService][lookupApplication] Failure response from HIP endpoint: $failedStatusCode, body=${response.body}")
            HttpResponse(
              status = failedStatusCode,
              body = response.body,
              headers = response.headers
            )
        }
    }
  }
}
