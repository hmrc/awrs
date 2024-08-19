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

package connectors

import models.{AwrsUsers, EnrolmentVerifiers}
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.LoggingUtils

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentStoreConnector @Inject()(val auditConnector: AuditConnector,
                                        http: HttpClientV2,
                                        config: ServicesConfig,
                                        @Named("appName") val appName: String)(implicit ec: ExecutionContext) extends LoggingUtils {
  val retryLimit = 7
  val retryWait = 1000 // milliseconds

  lazy val enrolmentStore: String = config.baseUrl("enrolment-store-proxy")

  def upsertEnrolment(enrolmentKey: String,
                      verifiers: EnrolmentVerifiers
                     )(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val url = s"$enrolmentStore/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey"

    def trySend(tries: Int): Future[HttpResponse] = {
      http.put(url"$url").withBody(Json.toJson(verifiers)).execute[HttpResponse].flatMap {
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

  def getAWRSUsers(awrsRef: String)(implicit hc: HeaderCarrier): Future[Either[Int, AwrsUsers]] = {

    val url = s"$enrolmentStore/enrolment-store-proxy/enrolment-store/enrolments/HMRC-AWRS-ORG~AWRSRefNumber~$awrsRef/users"
    http.get(url"$url").execute[HttpResponse].map {
      response =>
        response.status match {
          case OK =>
            logger.info(s"""[EnrolmentStoreConnector][getAWRSUsers]: ES0 Was successful for $awrsRef""")
            response.json.validate[AwrsUsers].fold(_ => Left(INTERNAL_SERVER_ERROR), users => Right(users))
          case NO_CONTENT =>
            logger.info(s"""[EnrolmentStoreConnector][getAWRSUsers]: ES0 Returned nothing for $awrsRef""")
            Right(AwrsUsers(Nil, Nil))
          case BAD_REQUEST =>
            logger.warn(s"""[EnrolmentStoreConnector][getAWRSUsers]: Received bad request for ES0 call ${response.status} : ${response.body}""")
            Left(BAD_REQUEST)
          case _ =>
            logger.error(s"[EnrolmentsStoreConnector][getAWRSUsers]: ES0 returned: ${response.status} with body ${response.body}")
            Left(response.status)
        }
    }
  }
}
