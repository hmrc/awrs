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

import javax.inject.{Inject, Named}
import models.{ES0NoContentResponse, ES0SuccessResponse, EnrolmentVerifiers}
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import utils.LoggingUtils
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.play.bootstrap.backend.http.ErrorResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnrolmentStoreConnector @Inject()(val auditConnector: AuditConnector,
                                        http: HttpClient,
                                        config: ServicesConfig,
                                        @Named("appName") val appName: String) extends LoggingUtils {
  val retryLimit = 7
  val retryWait = 1000 // milliseconds
  val enrolmentKeyPrefix = "HMRC-AWRS-ORG~AWRSRefNumber"

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

  def checkEnrolmentsConnector(awrsRef: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val awrsRef = "XAAW00000120001"
    val url = s"$enrolmentStore/enrolment-store-proxy/enrolment-store/enrolments/HMRC-AWRS-ORG~AWRSRefNumber~$awrsRef/users"
    //         TODO check if need two separate URLs. Probably not Rename this to checking credIDs
    http.GET[HttpResponse](url, Seq.empty).map {
      response =>
        response.status match {
          case OK =>
            info(s"""[EnrolmentStoreConnector][checkEnrolments]: Was successful""")
            println(s"""Case OK ${response.body} for $awrsRef""")
            val x = response.json.validate[ES0SuccessResponse]
            println(s"""JSON body is ${x} for $awrsRef""")
            response
          case NO_CONTENT =>
            info(s"""[EnrolmentStoreConnector][checkEnrolments]: Returned nothing for $awrsRef""")
            println(s"""Case NO_CONTENT ${response.body} for $awrsRef""")
            response
          case BAD_REQUEST =>
            info(s"""[EnrolmentStoreConnector][checkEnrolments]: Received bad request for ES0 call ${response.status} : ${response.body}""")
            println(s"""Case BAD REQUEST $response for $awrsRef""")
            response
          case _ =>
            logger.warn(s"[EnrolmentsStoreConnector][checkEnrolments] - status: ${response.status}")
            println(s"""Case _ ${response.body} ---- with status ${response.status} for $awrsRef""")
            response
        }
    }
  }

  //        def handleFailure(response: HttpResponse): Future[HttpResponse] = {
  //          audit(enrolmentStoreTxName, Map("awrsReferenceNumber" -> awrsRef, "FailureStatusCode" -> response.status.toString), eventTypeFailure)
  //          warn("checkEnrolments failed - retry limit exceeded")
  //          Future(response)
  //        }
}
