/*
 * Copyright 2025 HM Revenue & Customs
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

import javax.inject.Inject
import metrics.AwrsMetrics
import models.{EnrolmentVerifiers, _}
import play.api.Logging
import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{AWRSFeatureSwitches, SessionUtils, Utility}

import scala.collection.immutable.ListMap
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

private object SubscriptionService {
  private val AWRS_SERVICE_NAME = "HMRC-AWRS-ORG"

  private def transformationError(ex: Throwable) = s"JSON transformation failed: ${ex.toString}"

}

class SubscriptionService @Inject()(metrics: AwrsMetrics,
                                    val enrolmentStoreConnector: EnrolmentStoreConnector,
                                    val etmpConnector: EtmpConnector,
                                    val hipConnector: HipConnector)(implicit ec: ExecutionContext) extends Logging {

  import SubscriptionService._

  def subscribe(data: JsValue,
                safeId: String,
                utr: Option[String],
                businessType: String,
                postcode: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val timer = metrics.startTimer(ApiType.API4Subscribe)
    for {
      submitResponse <- if (AWRSFeatureSwitches.hipSwitch().enabled) {
        Try(updateCreateRequestForHip(data)) match {
          case Success(value) =>
            hipConnector.create(safeId, value) map {
              case HttpResponse(Status.CREATED, body, headers) =>
                HttpResponse(
                  status = Status.OK,
                  body = Json.stringify(updateCreateResponseForHip(Json.parse(body))),
                  headers = headers
                )
              case HttpResponse(failedStatusCode, body, headers) =>
                logger.error(s"[DeRegistrationService][deRegistration] Failure response from HIP endpoint: $failedStatusCode")
                HttpResponse(
                  status = failedStatusCode,
                  body = body,
                  headers = headers
                )
            }
          case Failure(ex) =>
            Future.successful(HttpResponse(
              status = Status.BAD_REQUEST,
              body = transformationError(ex)
            ))
        }
      } else {
        etmpConnector.subscribe(data, safeId)
      }
      enrolmentResponse <- addKnownFacts(submitResponse, safeId, utr, businessType, postcode)
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

  def updateSubscription(inputJson: JsValue, awrsRefNo: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] =
    if (AWRSFeatureSwitches.hipSwitch().enabled) {
      Try(updateUpdateRequestForHip(inputJson)) match {
        case Success(value) => hipConnector.updateSubscription(value, awrsRefNo) map {
          case HttpResponse(Status.OK, body, headers) =>
            HttpResponse(
              status = Status.OK,
              body = Json.stringify(updateUpdateResponseForHip(Json.parse(body))),
              headers = headers
            )
          case HttpResponse(failedStatusCode, body, headers) =>
            logger.error(s"[DeRegistrationService][deRegistration] Failure response from HIP endpoint: $failedStatusCode")
            HttpResponse(
              status = failedStatusCode,
              body = body,
              headers = headers
            )
        }
        case Failure(ex) =>
          Future.successful(HttpResponse(
            status = Status.BAD_REQUEST,
            body = transformationError(ex)
          ))
      }
    } else {
      etmpConnector.updateSubscription(inputJson, awrsRefNo)
    }

  private def addKnownFacts(response: HttpResponse,
                            safeId: String,
                            utr: Option[String],
                            businessType: String,
                            postcode: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] =
    response.status match {
      case OK =>
        val json = response.json
        val awrsRegistrationNumber = (json \ "awrsRegistrationNumber").as[String]

        val enrolmentKey = s"$AWRS_SERVICE_NAME~AWRSRefNumber~$awrsRegistrationNumber"
        val enrolmentVerifiers = createVerifiers(safeId, utr, businessType, postcode)
        enrolmentStoreConnector.upsertEnrolment(enrolmentKey, enrolmentVerifiers)
      case _ => Future(response)
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

  def updateGrpRepRegistrationDetails(safeId: String, updateData: UpdateRegistrationDetailsRequest)
                                     (implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val request = updateData.copy(acknowledgementReference = Some(SessionUtils.getUniqueAckNo))
    etmpConnector.updateGrpRepRegistrationDetails(safeId, Json.toJson(request))
  }

  // subscription/create
  private def updateCreateRequestForHip(data: JsValue): JsObject = {
    // remove fields
    val jsObjFieldsRemoved = Utility.removeFields(List(
      "acknowledgmentReference",
      "subscriptionType.businessDetails.soleProprietor.identification.doYouHaveANINO"
    ), data.as[JsObject])

    // update fields
    Utility.alterFieldKeys(ListMap(
      "subscriptionType.businessDetails.soleProprietor.identification.doYouHaveVRN" -> "subscriptionType.businessDetails.soleProprietor.identification.doYouHaveAVRN",
      "subscriptionType.businessDetails.partnership.numberOfPartners" -> "subscriptionType.businessDetails.partnership.noOfPartners",
      "subscriptionType.businessAddressForAwrs.currentAddress" -> "subscriptionType.busAddressForAWRS.currAddress",
      "subscriptionType.businessAddressForAwrs.communicationDetails" -> "subscriptionType.businessAddressForAwrs.commDetails",
      "subscriptionType.businessAddressForAwrs.differentOperatingAddresslnLast3Years" -> "subscriptionType.businessAddressForAwrs.diffOpAddrInLast3Years",
      "subscriptionType.businessAddressForAwrs" -> "subscriptionType.busAddressForAWRS",
      "contactDetails.useAlternateContactAddress" -> "contactDetails.useAltContactAddress",
      "contactDetails.communicationDetails" -> "contactDetails.commDetails",
    ), jsObjFieldsRemoved)
  }

  // subscription/create
  private def updateCreateResponseForHip(responseJson: JsValue): JsValue = {
    val successJsObject = Utility.stripSuccessNode(responseJson)

    val updated = Utility.alterFieldKeys(ListMap(
      "processingDateTime" -> "processingDate"
    ), successJsObject)

    Utility.removeFields(List("awrsRegNumber"), updated)
  }

  // subscription/update
  private def updateUpdateRequestForHip(data: JsValue): JsObject = {
    // remove fields
    val removed = Utility.removeFields(List("acknowledgmentReference"), data.as[JsObject])

    // alter fields
    Utility.alterFieldKeys(ListMap(
      "changeIndicators.businessDetailsChanged" -> "changeIndicators.businesDetailsChanged",
      "changeIndicators.businessAddressChanged" -> "changeIndicators.busAddressChanged",
      "changeIndicators.additionalBusinessInfoChanged" -> "changeIndicators.addBusInfoChanged",
      "subscriptionType.businessDetails.soleProprietor.identification.doYouHaveVRN" -> "subscriptionType.businessDetails.soleProprietor.identification.doYouHaveAVRN",
      "subscriptionType.businessDetails.soleProprietor.identification.doYouHaveNino" -> "subscriptionType.businessDetails.soleProprietor.identification.doYouHaveANINO",
      "subscriptionType.businessAddressForAwrs.currentAddress" -> "subscriptionType.businessAddressForAwrs.currAddress",
      "subscriptionType.businessAddressForAwrs.communicationDetails" -> "subscriptionType.businessAddressForAwrs.commDetails",
      "subscriptionType.businessAddressForAwrs.differentOperatingAddresslnLast3Years" -> "subscriptionType.businessAddressForAwrs.diffOpAddrInLast3Years",
      "subscriptionType.businessAddressForAwrs" -> "subscriptionType.busAddressForAWRS",
      "subscriptionType.contactDetails.useAlternateContactAddress" -> "subscriptionType.contactDetails.useAltContactAddress",
      "subscriptionType.contactDetails.communicationDetails" -> "subscriptionType.contactDetails.commDetails"
    ), removed)
  }

  // subscription/update
  private def updateUpdateResponseForHip(responseJson: JsValue): JsValue = {
    val successJsObject = Utility.stripSuccessNode(responseJson)

    Utility.alterFieldKeys(ListMap(
      "processingDateTime" -> "processingDate"
    ), successJsObject)
  }
}
