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

package controllers

import javax.inject.{Inject, Named}
import metrics.AwrsMetrics
import models._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.{EtmpLookupService, EtmpRegimeService, EtmpStatusService, SubscriptionService}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.LoggingUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubscriptionController @Inject()(val auditConnector: AuditConnector,
                                       val metrics: AwrsMetrics,
                                       val subscriptionService: SubscriptionService,
                                       val lookupService: EtmpLookupService,
                                       val statusService: EtmpStatusService,
                                       val regimeService: EtmpRegimeService,
                                       cc: ControllerComponents,
                                       @Named("appName") val appName: String) extends BackendController(cc) with LoggingUtils {

  private final val subscriptionTypeJSPath = "subscriptionTypeFrontEnd"

  def subscribe(): Action[AnyContent] = Action.async { implicit request =>
    val feJson = request.body.asJson.get
    val awrsModel = Json.parse(feJson.toString()).as[AWRSFEModel]
    val convertedEtmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter)
    val safeId: String = awrsModel.subscriptionTypeFrontEnd.businessCustomerDetails.fold("")(x => x.safeId)
    val userOrBusinessName: String = awrsModel.subscriptionTypeFrontEnd.businessCustomerDetails.fold("")(x => x.businessName)
    val legalEntityType: String = awrsModel.subscriptionTypeFrontEnd.legalEntity.get.legalEntity.fold("")(x => x)
    val businessReg: BusinessRegistrationDetails = awrsModel.subscriptionTypeFrontEnd.businessRegistrationDetails.get
    val businesssCustomerDetails = awrsModel.subscriptionTypeFrontEnd.businessCustomerDetails.get
    val postcode = awrsModel.subscriptionTypeFrontEnd.businessCustomerDetails.get.businessAddress.postcode.fold("")(x => x).replaceAll("\\s+", "")
    val utr = businessReg.utr
    val businessType = businessReg.legalEntity.fold("")(x => x)
    val auditMap: Map[String, String] = Map("safeId" -> safeId, "UserDetail" -> userOrBusinessName, "legal-entity" -> legalEntityType)

    subscriptionService.subscribe(convertedEtmpJson, safeId, utr, businessType, postcode).flatMap {
      registerData => registerData.status match {
        case OK =>
          warn(s"[$auditAPI4TxName - $userOrBusinessName, $legalEntityType ] - API4 Response from DES/GG  ## " + registerData.status)
          val successfulSubscriptionResponse = registerData.json.as[SuccessfulSubscriptionResponse]
          metrics.incrementSuccessCounter(ApiType.API4Subscribe)
          audit(transactionName = auditSubscribeTxName, detail = auditMap ++ Map("AWRS Reference No" -> successfulSubscriptionResponse.awrsRegistrationNumber), eventType = eventTypeSuccess)
          Future.successful(Ok(registerData.body))
        case NOT_FOUND =>
          metrics.incrementFailedCounter(ApiType.API4Subscribe)
          warn(s"[$auditAPI4TxName - $safeId ] - The remote endpoint has indicated that no data can be found")
          audit(transactionName = auditSubscribeTxName, detail = auditMap ++ Map("FailureReason" -> "Not Found"), eventType = eventTypeFailure)
          Future.successful(NotFound(registerData.body))
        case BAD_REQUEST =>
          regimeService.checkETMPApi(businesssCustomerDetails, businessType) map {
            case Some(etmpRegistrationDetails) =>
              warn(s"[$auditAPI4TxName - $userOrBusinessName, $legalEntityType ] Self Heal - API4 Response from DES/GG  ## " + registerData.status)
              val responseJson = Json.obj("regimeRefNumber" -> etmpRegistrationDetails.regimeRefNumber)
              Accepted(responseJson)
            case _ =>
              metrics.incrementFailedCounter(ApiType.API4Subscribe)
              warn(s"[$auditAPI4TxName - $safeId ] - Bad Request")
              val failureReason: String = if (registerData.body.contains("Reason")) (registerData.json \ "Reason").as[String] else "Bad Request"
              audit(transactionName = auditSubscribeTxName, detail = auditMap ++ Map("FailureReason" -> failureReason, "EtmpJson" -> convertedEtmpJson.toString()), eventType = eventTypeFailure)
              BadRequest(registerData.body)
          }
        case SERVICE_UNAVAILABLE =>
          metrics.incrementFailedCounter(ApiType.API4Subscribe)
          warn(s"[$auditAPI4TxName - $safeId ] - Dependant systems are currently not responding")
          audit(transactionName = auditSubscribeTxName, detail = auditMap ++ Map("FailureReason" -> "Service Unavailable"), eventType = eventTypeFailure)
          Future.successful(ServiceUnavailable(registerData.body))
        case INTERNAL_SERVER_ERROR =>
          metrics.incrementFailedCounter(ApiType.API4Subscribe)
          warn(s"[$auditAPI4TxName - $safeId ] - WSO2 is currently experiencing problems that require live service intervention")
          audit(transactionName = auditSubscribeTxName, detail = auditMap ++ Map("FailureReason" -> "Server error"), eventType = eventTypeFailure)
          Future.successful(InternalServerError(registerData.body))
        case status@_ =>
          metrics.incrementFailedCounter(ApiType.API4Subscribe)
          warn(s"[$auditAPI4TxName - $safeId ] - Unsuccessful return of data")
          audit(transactionName = auditSubscribeTxName, detail = auditMap ++ Map("FailureReason" -> "Other Error"), eventType = eventTypeFailure)
          Future.successful(InternalServerError(f"Unsuccessful return of data. Status code: $status"))
      }
    }
  }


  def updateSubscription(awrsRefNo: String): Action[AnyContent] = Action.async {
    implicit request =>

      val feJson = request.body.asJson.get
      val awrsModel = Json.parse(feJson.toString()).as[AWRSFEModel]
      val convertedEtmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter)

      val userOrBusinessName = (feJson \ subscriptionTypeJSPath \ "businessCustomerDetails" \ "businessName").as[String]
      val legalEntityType = (feJson \ subscriptionTypeJSPath \ "legalEntity" \ "legalEntity").as[String]
      val changeIndicators = feJson \ subscriptionTypeJSPath \ "changeIndicators"


      val auditMap: Map[String, String] = Map("AWRS Reference No" -> awrsRefNo, "UserDetail" -> userOrBusinessName, "legal-entity" -> legalEntityType, "change-flags" -> changeIndicators.toString())
      val timer = metrics.startTimer(ApiType.API6UpdateSubscription)
      subscriptionService.updateSubscription(convertedEtmpJson, awrsRefNo).map {
        updatedData =>
          timer.stop()
          warn(s"[$auditAPI6TxName - $userOrBusinessName, $legalEntityType ]")
          updatedData.status match {
            case OK =>
              metrics.incrementSuccessCounter(ApiType.API6UpdateSubscription)
              audit(transactionName = auditUpdateSubscriptionTxName, detail = auditMap, eventType = eventTypeSuccess)
              Ok(updatedData.body)
            case NOT_FOUND =>
              metrics.incrementFailedCounter(ApiType.API6UpdateSubscription)
              warn(s"[$auditAPI6TxName - $awrsRefNo ] - The remote endpoint has indicated that no data can be found")
              audit(transactionName = auditUpdateSubscriptionTxName, detail = auditMap ++ Map("FailureReason" -> "Not Found"), eventType = eventTypeFailure)
              NotFound(updatedData.body)
            case BAD_REQUEST =>
              metrics.incrementFailedCounter(ApiType.API6UpdateSubscription)
              warn(s"[$auditAPI6TxName - $awrsRefNo ] - Bad Request \n API6 Request Json to DES ## ")
              val failureReason: String = if (updatedData.body.contains("Reason")) (updatedData.json \ "Reason").as[String] else "Bad Request"
              audit(transactionName = auditUpdateSubscriptionTxName, detail = auditMap ++ Map("FailureReason" -> failureReason, "EtmpJson" -> convertedEtmpJson.toString()), eventType = eventTypeFailure)
              BadRequest(updatedData.body)
            case SERVICE_UNAVAILABLE =>
              metrics.incrementFailedCounter(ApiType.API6UpdateSubscription)
              warn(s"[$auditAPI6TxName - $awrsRefNo ] - Dependant systems are currently not responding")
              audit(transactionName = auditUpdateSubscriptionTxName, detail = auditMap ++ Map("FailureReason" -> "Service Unavailable"), eventType = eventTypeFailure)
              ServiceUnavailable(updatedData.body)
            case INTERNAL_SERVER_ERROR =>
              metrics.incrementFailedCounter(ApiType.API6UpdateSubscription)
              warn(s"[$auditAPI6TxName - $awrsRefNo ] - WSO2 is currently experiencing problems that require live service intervention")
              audit(transactionName = auditUpdateSubscriptionTxName, detail = auditMap ++ Map("FailureReason" -> "Internal Server Error"), eventType = eventTypeFailure)
              InternalServerError(updatedData.body)
            case status@_ =>
              metrics.incrementFailedCounter(ApiType.API6UpdateSubscription)
              warn(s"[$auditAPI6TxName - $awrsRefNo ] - Unsuccessful return of data")
              audit(transactionName = auditUpdateSubscriptionTxName, detail = auditMap ++ Map("FailureReason" -> "Other Error"), eventType = eventTypeFailure)
              InternalServerError(f"Unsuccessful return of data. Status code: $status")
          }
      }
  }

  def lookupApplication(awrsRef: String): Action[AnyContent] = Action.async {
    implicit request =>
      val timer = metrics.startTimer(ApiType.API5LookupSubscription)
      lookupService.lookupApplication(awrsRef) map {
        result =>
          timer.stop()
          result.status match {
            case OK =>
              metrics.incrementSuccessCounter(ApiType.API5LookupSubscription)
              warn(s"[$auditAPI5TxName - $awrsRef ] - Successful return of API5 Response")
              debug(s"${result.json}")
              val convertedJson = result.json.as[AWRSFEModel](AWRSFEModel.etmpReader)
              Ok(Json.toJson(convertedJson))
            case NOT_FOUND =>
              metrics.incrementFailedCounter(ApiType.API5LookupSubscription)
              warn(s"[$auditAPI5TxName  - $awrsRef ] - The remote endpoint has indicated that no data can be found")
              NotFound(result.body)
            case BAD_REQUEST =>
              metrics.incrementFailedCounter(ApiType.API5LookupSubscription)
              audit(auditAPI5TxName, Map("awrsRef" -> awrsRef, "DES-Response" -> result.body), eventTypeBadRequest)
              warn(s"[$auditAPI5TxName  - $awrsRef ] - Bad Request \n API5 Response from DES ##")
              BadRequest(result.body)
            case SERVICE_UNAVAILABLE =>
              metrics.incrementFailedCounter(ApiType.API5LookupSubscription)
              warn(s"[$auditAPI5TxName  - $awrsRef ] - Dependant systems are currently not responding")
              ServiceUnavailable(result.body)
            case INTERNAL_SERVER_ERROR =>
              metrics.incrementFailedCounter(ApiType.API5LookupSubscription)
              warn(s"[$auditAPI5TxName  - $awrsRef ] - WSO2 is currently experiencing problems that require live service intervention")
              InternalServerError(result.body)
            case status@_ =>
              metrics.incrementFailedCounter(ApiType.API5LookupSubscription)
              warn(s"[$auditAPI5TxName  - $awrsRef ] - Unsuccessful return of data")
              InternalServerError(f"Unsuccessful return of data. Status code: $status")
          }
      }
  }

  def checkStatus(awrsRef: String): Action[AnyContent] = Action.async {
    implicit request =>
      val timer = metrics.startTimer(ApiType.API9UpdateSubscription)
      statusService.checkStatus(awrsRef) map {
        result =>
          timer.stop()
          result.status match {
            case OK =>
              metrics.incrementSuccessCounter(ApiType.API9UpdateSubscription)
              warn(s"[$auditAPI9TxName - $awrsRef ] - Successful return of data \n ## API9 Response from DES  ## " + result.json)
              val convertedJson = result.json.as[SubscriptionStatusType](SubscriptionStatusType.reader)
              Ok(Json.toJson(convertedJson))
            case NOT_FOUND =>
              metrics.incrementFailedCounter(ApiType.API9UpdateSubscription)
              warn(s"[$auditAPI9TxName  - $awrsRef ] - The remote endpoint has indicated that no data can be found")
              NotFound(result.body)
            case BAD_REQUEST =>
              metrics.incrementFailedCounter(ApiType.API9UpdateSubscription)
              warn(s"[$auditAPI9TxName - $awrsRef ] - The request has not passed validation")
              BadRequest(result.body)
            case SERVICE_UNAVAILABLE =>
              metrics.incrementFailedCounter(ApiType.API9UpdateSubscription)
              warn(s"[$auditAPI9TxName - $awrsRef ] - Dependant systems are currently not responding")
              ServiceUnavailable(result.body)
            case INTERNAL_SERVER_ERROR =>
              metrics.incrementFailedCounter(ApiType.API9UpdateSubscription)
              warn(s"[$auditAPI9TxName - $awrsRef ] - WSO2 is currently experiencing problems that require live service intervention")
              InternalServerError(result.body)
            case status@_ =>
              metrics.incrementFailedCounter(ApiType.API9UpdateSubscription)
              warn(s"[$auditAPI9TxName - $awrsRef ] - Unsuccessful return of data")
              InternalServerError(f"Unsuccessful return of data. Status code: $status")
          }
      }
  }

  def updateGrpRegistrationDetails(awrsRefNo: String, safeId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      val updatedData = request.body.as[UpdateRegistrationDetailsRequest]
      subscriptionService.updateGrpRepRegistrationDetails(safeId, updatedData) map {
        responseReceived =>
          responseReceived.status match {
            case OK => Ok(responseReceived.body)
            case NOT_FOUND => NotFound(responseReceived.body)
            case BAD_REQUEST => BadRequest(responseReceived.body)
            case SERVICE_UNAVAILABLE => ServiceUnavailable(responseReceived.body)
            case _ => InternalServerError(responseReceived.body)
          }
      }
  }
}
