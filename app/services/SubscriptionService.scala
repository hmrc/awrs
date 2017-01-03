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

package services

import connectors.{EtmpConnector, GovernmentGatewayAdminConnector}
import metrics.Metrics
import models.{ApiType, KnownFact, KnownFactsForService}
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SubscriptionService extends SubscriptionService {
  val etmpConnector: EtmpConnector = EtmpConnector
  val ggAdminConnector: GovernmentGatewayAdminConnector = GovernmentGatewayAdminConnector
  override val metrics = Metrics
}

trait SubscriptionService {
  val etmpConnector: EtmpConnector
  val ggAdminConnector: GovernmentGatewayAdminConnector
  val notFound = Json.parse( """{"Reason": "Resource not found"}""")
  val metrics: Metrics

  def subscribe(data: JsValue, safeId: String, utr: Option[String], businessType: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val timer = metrics.startTimer(ApiType.API4Subscribe)
    for {
      submitResponse <- etmpConnector.subscribe(data, safeId)
      ggResponse <- addKnownFacts(submitResponse, safeId, utr, businessType)
    } yield {
      ggResponse.status match {
        case OK =>
          timer.stop()
          metrics.incrementSuccessCounter(ApiType.API4AddKnownFacts)
          submitResponse
        case _ =>
          timer.stop()
          metrics.incrementFailedCounter(ApiType.API4AddKnownFacts)
          // Code changed to always return the etmp response even if there is a GG failure.
          // The GG failure will need to be sorted out manually and there is nothing the user can do at the time.
          // The manual process will take place after the GG failure is picked up in Splunk.
          submitResponse
      }
    }
  }

  def updateSubcription(inputJson: JsValue, awrsRefNo: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] =
    etmpConnector.updateSubscription(inputJson, awrsRefNo)

  private def addKnownFacts(response: HttpResponse, safeId: String, utr: Option[String], businessType: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] =
    response.status match {
      case OK =>
        val json = response.json
        val awrsRegistrationNumber = (json \ "awrsRegistrationNumber").as[String]
        ggAdminConnector.addKnownFacts(createKnownFacts(awrsRegistrationNumber, safeId, utr, businessType), awrsRegistrationNumber)
      case _ => Future.successful(response)
    }

  private def createKnownFacts(awrsRegistrationNumber: String, safeId: String, utr: Option[String], businessType: String) = {
    val knownFact1 = KnownFact("AWRSRefNumber", awrsRegistrationNumber)
    val knownFact2 = KnownFact("SAFEID", safeId)
    val knownFacts = utr match {
      case Some(someUtr) =>
        val knownFact3 = businessType match {
          case "SOP" => KnownFact("SAUTR", someUtr)
          case _ => KnownFact("CTUTR", someUtr)
        }
        List(knownFact1, knownFact2, knownFact3)
      case _ => List(knownFact1, knownFact2)
    }
    KnownFactsForService(knownFacts)
  }

}
