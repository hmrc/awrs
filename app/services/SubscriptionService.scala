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

package services

import connectors.{EnrolmentStoreConnector, EtmpConnector, HipConnector}
import metrics.AwrsMetrics
import models._
import play.api.Logging
import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json.{JsError, JsObject, JsResult, JsSuccess, JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{AWRSFeatureSwitches, SessionUtils, Utility}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionService @Inject() (
    metrics: AwrsMetrics,
    val enrolmentStoreConnector: EnrolmentStoreConnector,
    val etmpConnector: EtmpConnector,
    val hipConnector: HipConnector
)(implicit ec: ExecutionContext) extends Logging {

  val AWRS_SERVICE_NAME = "HMRC-AWRS-ORG"
  private val acknowledgmentReference: String = "acknowledgmentReference"


  def subscribe(data: JsValue,
      safeId: String,
      utr: Option[String],
      businessType: String,
      postcode: String
  )(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val timer = metrics.startTimer(ApiType.API4Subscribe)
    for {
      submitResponse <- etmpConnector.subscribe(data, safeId)
      enrolmentResponse <- addKnownFacts(
        submitResponse,
        safeId,
        utr,
        businessType,
        postcode
      )
    } yield {
      (submitResponse.status, enrolmentResponse.status) match {
        case (OK, NO_CONTENT) =>
          timer.stop()
          metrics.incrementSuccessCounter(ApiType.API4AddKnownFacts)
          submitResponse
        case _ =>
          timer.stop()
          metrics.incrementFailedCounter(ApiType.API4AddKnownFacts)
          enrolmentResponse
      }
    }
  }

  def updateSubscription(inputJson: JsValue, awrsRefNo: String)(implicit
      headerCarrier: HeaderCarrier
  ): Future[HttpResponse] = {
    if (AWRSFeatureSwitches.hipSwitch().enabled) {
      val hipRequestJson: JsResult[JsValue] = updateRequestForHip(inputJson)
      hipRequestJson match {
        case JsSuccess(hipRequest,_) =>
          hipConnector.updateSubscription(hipRequest, awrsRefNo).map { response =>
            response.status match {
              case OK =>
                val strippedSuccessBody = Utility.stripSuccessNode(response.json)
                HttpResponse(
                  status = response.status,
                  body = strippedSuccessBody.toString(),
                  headers = response.headers
                )
              case status@_ =>
                logger.error(s"[SubscriptionService][updateSubscription] Failure response from HIP endpoint : $status")
                response
            }
          }
        case JsError(errors) => Future.successful(HttpResponse(
          status = Status.BAD_REQUEST,
          body = s"JSON transformation failed: ${JsError.toJson(errors)}"
        ))
      }
    } else { etmpConnector.updateSubscription(inputJson, awrsRefNo) }
  }

  private def addKnownFacts(
      response: HttpResponse,
      safeId: String,
      utr: Option[String],
      businessType: String,
      postcode: String
  )(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] =
    response.status match {
      case OK =>
        val json = response.json
        val awrsRegistrationNumber =
          (json \ "awrsRegistrationNumber").as[String]

        val enrolmentKey =
          s"$AWRS_SERVICE_NAME~AWRSRefNumber~$awrsRegistrationNumber"
        val enrolmentVerifiers =
          createVerifiers(safeId, utr, businessType, postcode)
        enrolmentStoreConnector.upsertEnrolment(
          enrolmentKey,
          enrolmentVerifiers
        )
      case _ => Future(response)
    }

  private def createVerifiers(
      safeId: String,
      utr: Option[String],
      businessType: String,
      postcode: String
  ) = {
    val utrTuple = businessType match {
      case "SOP" => "SAUTR" -> utr.getOrElse("")
      case _     => "CTUTR" -> utr.getOrElse("")
    }
    val verifierTuples = Seq(
      "Postcode" -> postcode,
      "SAFEID" -> safeId
    ) :+ utrTuple

    EnrolmentVerifiers(verifierTuples: _*)
  }

  def updateGrpRepRegistrationDetails(safeId: String, updateData: UpdateRegistrationDetailsRequest
                                     )(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val request = updateData.copy(acknowledgementReference =
      Some(SessionUtils.getUniqueAckNo)
    )
    etmpConnector.updateGrpRepRegistrationDetails(safeId, Json.toJson(request))
  }

  def updateRequestForHip(requestJson: JsValue): JsResult[JsObject] = {
    requestJson.validate[JsObject].map { requestJsObject =>
      requestJsObject - acknowledgmentReference
    }
  }
}
