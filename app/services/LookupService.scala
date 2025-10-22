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
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.AWRSFeatureSwitches

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LookupService @Inject()(etmpConnector: EtmpConnector, hipConnector: HipConnector)(implicit ec: ExecutionContext) {

  val awrsRegistrationNumber = "awrsRegistrationNumber"

  def lookupApplication(awrsRefNo: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {

    if (AWRSFeatureSwitches.hipSwitch().enabled) {
      hipConnector.lookup(awrsRefNo) map {
        response: HttpResponse =>
          response.status match {
            case _: Int => response
          }
      }
    } else {
      etmpConnector.lookup(awrsRefNo) map {
        response =>
          response.status match {
            case _ => response
          }
      }
    }
  }

}
