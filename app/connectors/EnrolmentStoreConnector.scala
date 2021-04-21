/*
 * Copyright 2021 HM Revenue & Customs
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

import javax.inject.{Inject, Named}
import models.EnrolmentVerifiers
import play.api.http.Status.{NO_CONTENT, BAD_REQUEST}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import utils.LoggingUtils
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentStoreConnector @Inject()(val auditConnector: AuditConnector,
                                        http: HttpClient,
                                        config: ServicesConfig,
                                        @Named("appName") val appName: String) extends LoggingUtils {
  val retryLimit = 7
  val retryWait = 1000 // milliseconds

  lazy val enrolmentStore: String = config.baseUrl("enrolment-store-proxy")

  def upsertEnrolment(enrolmentKey: String,
                      verifiers: EnrolmentVerifiers
                     )(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val url = s"$enrolmentStore/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey"

    def trySend(tries: Int): Future[HttpResponse] = {
      http.PUT(url, verifiers)(implicitly, readRaw, implicitly, implicitly).flatMap {
        response =>
          response.status match {
            case NO_CONTENT => Future(response)
            case BAD_REQUEST => handleFailure(response)
            case _ if tries < retryLimit => Future {
              warn(s"Retrying upsertEnrolment - call number: $tries")
              Thread.sleep(retryWait)
            }.flatMap(_ => trySend(tries + 1))
            case _ =>
              handleFailure(response)
          }
      }
    }

    def handleFailure(response: HttpResponse): Future[HttpResponse] = {
      audit(enrolmentStoreTxName, Map("enrolmentKey" -> enrolmentKey, "FailureStatusCode" -> response.status.toString), eventTypeFailure)
      warn("upsertEnrolment failed - retry limit exceeded")
      Future(response)
    }

    trySend(0)
  }
}
