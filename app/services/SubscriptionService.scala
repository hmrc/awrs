/*
 * Copyright 2016 HM Revenue & Customs
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
import models.{ApiType, EnrolRequest, KnownFact, KnownFactsForService}
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.GGConstants._
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
  val enrolService: EnrolService

  def subscribe(data: JsValue, safeId: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val timer = metrics.startTimer(ApiType.API4Subscribe)
    for {
      submitResponse <- etmpConnector.subscribe(data, safeId)
      _ <- addKnownFacts(submitResponse, safeId)
      ggResponse <- enrolService.enrolAWRS(submitResponse, safeId)

    } yield {
      ggResponse.status match {
        case OK =>
          timer.stop()
          metrics.incrementSuccessCounter(ApiType.API4AddKnownFacts)
          submitResponse
        case _ =>
          timer.stop()
          metrics.incrementFailedCounter(ApiType.API4AddKnownFacts)
          ggResponse
      }
    }
  }

  def updateSubcription(inputJson: JsValue, awrsRefNo: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] =
    etmpConnector.updateSubscription(inputJson, awrsRefNo)


  private def addKnownFacts(response: HttpResponse, safeId: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] =
    response.status match {
      case OK => ggAdminConnector.addKnownFacts(createKnownFacts(response, safeId))
      case _ => Future.successful(response)
    }



  private def createKnownFacts(response: HttpResponse, safeId: String) = {
    val json = response.json
    val awrsRegistrationNumber = (json \ "awrsRegistrationNumber").as[String]
    val knownFact1 = KnownFact("AWRSRefNumber", awrsRegistrationNumber)
    val knownFact2 = KnownFact("SAFEID", safeId)
    val knownFacts = List(knownFact1, knownFact2)
    KnownFactsForService(knownFacts)
  }

}
