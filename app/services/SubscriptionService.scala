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

import connectors.{EnrolmentStoreConnector, EtmpConnector, GovernmentGatewayAdminConnector}
import metrics.AwrsMetrics
import models.{EnrolmentVerifiers, KnownFactsForService, _}
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.RunMode
import utils.SessionUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SubscriptionService extends SubscriptionService {
  val etmpConnector: EtmpConnector = EtmpConnector
  val ggAdminConnector: GovernmentGatewayAdminConnector = GovernmentGatewayAdminConnector
  val enrolmentStoreConnector: EnrolmentStoreConnector = EnrolmentStoreConnector
  override val metrics = AwrsMetrics
}

trait SubscriptionService extends RunMode {
  val AWRS_SERVICE_NAME = "HMRC-AWRS-ORG"
  val etmpConnector: EtmpConnector
  val ggAdminConnector: GovernmentGatewayAdminConnector
  val enrolmentStoreConnector: EnrolmentStoreConnector
  val notFound = Json.parse( """{"Reason": "Resource not found"}""")
  val metrics: AwrsMetrics
  val isEmacFeatureToggle = runModeConfiguration.getBoolean("emacsFeatureToggle").getOrElse(true)

  def subscribe(data: JsValue,
                safeId: String,
                utr: Option[String],
                businessType: String,
                postcode: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val timer = metrics.startTimer(ApiType.API4Subscribe)
    for {
      submitResponse <- etmpConnector.subscribe(data, safeId)
      ggResponse <- addKnownFacts(submitResponse, safeId, utr, businessType, postcode)
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

  private def addKnownFacts(response: HttpResponse,
                            safeId: String,
                            utr: Option[String],
                            businessType: String,
                            postcode: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] =
    response.status match {
      case OK =>
        val json = response.json
        val awrsRegistrationNumber = (json \ "awrsRegistrationNumber").as[String]

        if (isEmacFeatureToggle) {
          Logger.info( "EMACS is switched ON" )
          val enrolmentKey = s"$AWRS_SERVICE_NAME~AWRSRefNumber~$awrsRegistrationNumber"
          val enrolmentVerifiers = createVerifiers(safeId, utr, businessType, postcode)
          enrolmentStoreConnector.upsertEnrolment(enrolmentKey, enrolmentVerifiers)
        } else {
          Logger.info( "EMACS is switched OFF so using GG" )
          ggAdminConnector
            .addKnownFacts(
              createKnownFacts(awrsRegistrationNumber, safeId, utr, businessType, postcode), awrsRegistrationNumber)
        }
      case _ => Future.successful(response)
    }

  private def createKnownFacts(awrsRegistrationNumber: String, safeId: String, utr: Option[String], businessType: String, postcode: String) = {
    val knownFact1 = KnownFact("AWRSRefNumber", awrsRegistrationNumber)
    val knownFact2 = KnownFact("POSTCODE", postcode)
    val knownFacts = utr match {
      case Some(someUtr) =>
        businessType match {
          case "SOP" => List(knownFact1, KnownFact("SAUTR", someUtr), knownFact2)
          case _ => List(knownFact1, KnownFact("CTUTR", someUtr), knownFact2)
        }
      case _ => List(knownFact1, knownFact2)
    }
    KnownFactsForService(knownFacts)
  }

  private def createVerifiers(safeId: String, utr: Option[String], businessType: String, postcode: String) = {
    val utrTuple = businessType match {
      case "SOP" => "SAUTR" -> utr.getOrElse("")
      case _ => "CTUTR" -> utr.getOrElse("")
    }
    val verifierTuples = Seq(
      "Postcode" -> postcode,
      "SAFEID" -> safeId
    ) :+ utrTuple

    EnrolmentVerifiers(verifierTuples: _*)
  }

  def updateGrpRepRegistrationDetails(awrsRefNo: String, safeId: String, updateData: UpdateRegistrationDetailsRequest)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val request = updateData.copy(acknowledgementReference = Some(SessionUtils.getUniqueAckNo))
    etmpConnector.updateGrpRepRegistrationDetails(safeId, Json.toJson(request))
  }

}
