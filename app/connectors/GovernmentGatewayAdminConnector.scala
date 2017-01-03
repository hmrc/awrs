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

import models.KnownFactsForService
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import config.WSHttp
import uk.gov.hmrc.play.http._
import utils.LoggingUtils
import play.api.http.Status._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait GovernmentGatewayAdminConnector extends ServicesConfig with RawResponseReads with LoggingUtils {

  lazy val serviceURL = baseUrl("government-gateway-admin")

  val addKnownFactsURI = "known-facts"

  val retryLimit = 7
  val retryWait = 1000 // milliseconds

  val url = s"""$serviceURL/government-gateway-admin/service"""

  val http: HttpGet with HttpPost = WSHttp

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
            case _ =>
              // The GG failure will need to be sorted out manually until an automated service is introduced (currently in the pipeline).
              // The manual process will take place after the GG failure is picked up in Splunk.
              audit(ggAdminTxName, Map("awrsRef" -> awrsRegistrationNumber), eventTypeFailure)
              warn(s"Retrying GG Admin Add Known Facts - retry limit exceeded")
              Future.successful(response)
          }
      }
    }
    trySend(0)
  }
}

object GovernmentGatewayAdminConnector extends GovernmentGatewayAdminConnector
