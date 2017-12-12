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

package connectors

import config.WSHttp
import httpparsers.UpsertEnrolmentResponseHttpParser.UpsertEnrolmentResponse
import models.{EnrolmentKey, EnrolmentVerifiers}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost, HttpPut}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future

trait EnrolmentStoreConnector extends ServicesConfig {
  lazy val enrolmentStore = baseUrl("enrolment-store-proxy")

  val http: HttpGet with HttpPut = WSHttp

  def upsertEnrolment(enrolmentKey: EnrolmentKey,
                      verifiers: EnrolmentVerifiers
                     )(implicit hc: HeaderCarrier): Future[UpsertEnrolmentResponse] = {
    val url = s"$enrolmentStore/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey"
    http.PUT[EnrolmentVerifiers, UpsertEnrolmentResponse](url, verifiers)
  }

//  def allocateEnrolment(groupId: String,
//                        enrolmentKey: EnrolmentKey,
//                        enrolmentRequest: EmacEnrolmentRequest
//                       )(implicit hc: HeaderCarrier): Future[AllocateEnrolmentResponse] = {
//    val url = appConfig.allocateEnrolmentUrl(groupId, enrolmentKey.asString)
//    httpClient.POST[EmacEnrolmentRequest, AllocateEnrolmentResponse](url, enrolmentRequest)
//  }
}

object EnrolmentStoreConnector extends EnrolmentStoreConnector
