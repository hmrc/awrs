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

package controllers.spike

import config.MicroserviceAuditConnector
import metrics.Metrics
import models.{AWRSFEModel, ApiType, SubscriptionStatusType}
import play.api.libs.json.Json
import play.api.mvc.Action
import services.{TestSpikeSubscriptionService, EtmpLookupService, EtmpStatusService, SubscriptionService}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.microservice.controller.BaseController
import utils.LoggingUtils

import scala.concurrent.ExecutionContext.Implicits.global

object TestSpikeOrgSubscriptionController extends TestSpikeSubscriptionController {
  override val appName: String = AppName.appName
  override val audit: Audit = new Audit(AppName.appName, MicroserviceAuditConnector)
  val subscriptionService: TestSpikeSubscriptionService = TestSpikeSubscriptionService
  override val metrics = Metrics
}

object TestSpikeSaSubscriptionController extends TestSpikeSubscriptionController with LoggingUtils {
  override val appName: String = AppName.appName
  override val audit: Audit = new Audit(AppName.appName, MicroserviceAuditConnector)
  val subscriptionService: TestSpikeSubscriptionService = TestSpikeSubscriptionService
  override val metrics = Metrics
}

trait TestSpikeSubscriptionController extends BaseController with LoggingUtils {
  val subscriptionService: TestSpikeSubscriptionService
  val metrics: Metrics

  private final val subscriptionTypeJSPath = "subscriptionTypeFrontEnd"

  def subscribe(ref: String) = Action.async {
    implicit request =>

      val feJson = request.body.asJson.get

      val safeId = (feJson \ subscriptionTypeJSPath \ "businessCustomerDetails" \ "safeId").as[String]
      val userOrBusinessName = (feJson \ subscriptionTypeJSPath \ "businessCustomerDetails" \ "businessName").as[String]
      val legalEntityType = (feJson \ subscriptionTypeJSPath \ "legalEntity" \ "legalEntity").as[String]

      val auditMap: Map[String,String] = Map("safeId" -> safeId, "UserDetail" -> userOrBusinessName, "legal-entity" -> legalEntityType)

      val awrsModel = Json.parse(feJson.toString()).as[AWRSFEModel]
      val convertedEtmpJson = Json.toJson(awrsModel)(AWRSFEModel.etmpWriter)

      subscriptionService.subscribe(convertedEtmpJson,safeId).map {
        registerData =>
          warn(s"[$auditAPI4TxName - $userOrBusinessName, $legalEntityType ] - API4 Response from DES/GG  ## " +  registerData.body)
          registerData.status match {
            case OK =>
              metrics.incrementSuccessCounter(ApiType.API4Subscribe)
              Ok(registerData.body)
            case NOT_FOUND =>
              metrics.incrementFailedCounter(ApiType.API4Subscribe)
              warn(s"[$auditAPI4TxName - $safeId ] - The remote endpoint has indicated that no data can be found")
              audit(transactionName = auditSubscribeTxName, detail = auditMap ++ Map("FailureReason" -> "Not Found"), eventType = eventTypeFailure)
              NotFound(registerData.body)
            case BAD_REQUEST =>
              metrics.incrementFailedCounter(ApiType.API4Subscribe)
              warn(s"[$auditAPI4TxName - $safeId ] - Bad Request \n API4 Request Json to DES ## ")
              val failureReason: String = if(registerData.body.contains("Reason")) (registerData.json \ "Reason").as[String] else "Bad Request"
              audit(transactionName = auditSubscribeTxName, detail = auditMap ++ Map("FailureReason" -> failureReason, "EtmpJson" -> convertedEtmpJson.toString()), eventType = eventTypeFailure)
              BadRequest(registerData.body)
            case SERVICE_UNAVAILABLE =>
              metrics.incrementFailedCounter(ApiType.API4Subscribe)
              warn(s"[$auditAPI4TxName - $safeId ] - Dependant systems are currently not responding")
              audit(transactionName = auditSubscribeTxName, detail = auditMap ++ Map("FailureReason" -> "Service Unavailable"), eventType = eventTypeFailure)
              ServiceUnavailable(registerData.body)
            case INTERNAL_SERVER_ERROR =>
              metrics.incrementFailedCounter(ApiType.API4Subscribe)
              warn(s"[$auditAPI4TxName - $safeId ] - WSO2 is currently experiencing problems that require live service intervention")
              audit(transactionName = auditSubscribeTxName, detail = auditMap ++ Map("FailureReason" -> "Server error"), eventType = eventTypeFailure)
              InternalServerError(registerData.body)
            case status@_ =>
              metrics.incrementFailedCounter(ApiType.API4Subscribe)
              warn(s"[$auditAPI4TxName - $safeId ] - Unsuccessful return of data")
              audit(transactionName = auditSubscribeTxName, detail = auditMap ++ Map("FailureReason" -> "Other Error"), eventType = eventTypeFailure)
              InternalServerError(f"Unsuccessful return of data. Status code: $status")
          }
      }
  }



}
