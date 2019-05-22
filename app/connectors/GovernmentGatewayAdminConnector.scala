/*
 * Copyright 2019 HM Revenue & Customs
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
import models.KnownFactsForService
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import utils.LoggingUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class GovernmentGatewayAdminConnector @Inject()(http: DefaultHttpClient,
                                                val auditConnector: AuditConnector,
                                                config: ServicesConfig,
                                                @Named("appName") val appName: String) extends RawResponseReads with LoggingUtils {

  lazy val serviceURL: String = config.baseUrl("government-gateway-admin")

  val addKnownFactsURI = "known-facts"

  val retryLimit = 7
  val retryWait = 1000 // milliseconds

  val url = s"""$serviceURL/government-gateway-admin/service"""

  def addKnownFacts(knownFacts: KnownFactsForService, awrsRegistrationNumber: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = postKnownFact(knownFacts, addKnownFactsURI, awrsRegistrationNumber)

  def postKnownFact(knownFacts: KnownFactsForService, destination: String, awrsRegistrationNumber: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val AWRS = "HMRC-AWRS-ORG"
    val jsonData = Json.toJson(knownFacts)
    val postUrl = s"""$url/$AWRS/$destination"""

    def trySend(tries: Int): Future[HttpResponse] = {
      http.POST[JsValue, HttpResponse](postUrl, jsonData).flatMap {
        response =>
          response.status match {
            case OK => Future.successful(response)
            case _ if tries < retryLimit => Future {
              warn(s"Retrying GG Admin Add Known Facts - call number: $tries")
              Thread.sleep(retryWait)
            }.flatMap(_ => trySend(tries + 1))
            case status@_ =>
              // The GG failure will need to be sorted out manually until an automated service is introduced (currently in the pipeline).
              // The manual process will take place after the GG failure is picked up in Splunk.
              audit(ggAdminTxName, Map("awrsRef" -> awrsRegistrationNumber, "FailureStatusCode" -> status.toString), eventTypeFailure)
              warn(s"Retrying GG Admin Add Known Facts - retry limit exceeded")
              Future.successful(response)
          }
      }
    }

    trySend(0)
  }
}
