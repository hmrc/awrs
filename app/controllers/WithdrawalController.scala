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
import models.{ApiType, WithdrawalRequest}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.LoggingUtils

import scala.concurrent.ExecutionContext.Implicits.global


class WithdrawalController @Inject()(val auditConnector: AuditConnector,
                                     metrics: AwrsMetrics,
                                     val withdrawalService: WithdrawalService,
                                     cc: ControllerComponents,
                                     @Named("appName") val appName: String) extends BackendController(cc) with LoggingUtils {

  def withdrawal(awrsRefNo: String): Action[AnyContent] = Action.async {
    implicit request =>
      info(s"[$auditAPI8TxName - $awrsRefNo ] - hit withdrawal controller ")
      val apiType: ApiType.Value = ApiType.API8Withdrawal

      val auditWithdrawalTxName: String = "AWRS ETMP Withdrawal"
      val auditMap: Map[String, String] = Map("AWRS Reference No" -> awrsRefNo)

      val timer = metrics.startTimer(apiType)

      val frontEndJson = request.body.asJson.get
      val withdrawalRequest = Json.parse(frontEndJson.toString()).as[WithdrawalRequest]
      val etmpWithdrawalJson = Json.toJson(withdrawalRequest)

      withdrawalService.withdrawal(etmpWithdrawalJson, awrsRefNo) map {
        result =>
          timer.stop()
          result.status match {
            case OK =>
              metrics.incrementSuccessCounter(ApiType.API8Withdrawal)
              warn(s"[$auditAPI8TxName - $awrsRefNo ] - Success")
              Ok(result.body)
            case NOT_FOUND =>
              metrics.incrementFailedCounter(ApiType.API8Withdrawal)
              warn(s"[$auditAPI8TxName - $awrsRefNo ] - The remote endpoint has indicated that no data can be found")
              audit(transactionName = auditWithdrawalTxName, detail = auditMap ++ Map("FailureReason" -> "Not Found"), eventType = eventTypeFailure)
              NotFound(result.body)
            case BAD_REQUEST =>
              metrics.incrementFailedCounter(ApiType.API8Withdrawal)
              warn(s"[$auditAPI8TxName - $awrsRefNo ] - Bad Request")
              audit(transactionName = auditWithdrawalTxName, detail = auditMap ++ Map("FailureReason" -> "Bad Request", "EtmpJson" -> etmpWithdrawalJson.toString()), eventType = eventTypeFailure)
              BadRequest(result.body)
            case SERVICE_UNAVAILABLE =>
              metrics.incrementFailedCounter(ApiType.API8Withdrawal)
              warn(s"[$auditAPI8TxName - $awrsRefNo ] - Dependant systems are currently not responding")
              audit(transactionName = auditWithdrawalTxName, detail = auditMap ++ Map("FailureReason" -> "Service Unavailable"), eventType = eventTypeFailure)
              ServiceUnavailable(result.body)
            case INTERNAL_SERVER_ERROR =>
              metrics.incrementFailedCounter(ApiType.API8Withdrawal)
              warn(s"[$auditAPI8TxName - $awrsRefNo ] - WSO2 is currently experiencing problems that require live service intervention")
              audit(transactionName = auditWithdrawalTxName, detail = auditMap ++ Map("FailureReason" -> "Internal Server Error"), eventType = eventTypeFailure)
              InternalServerError(result.body)
            case status@_ =>
              metrics.incrementFailedCounter(ApiType.API8Withdrawal)
              warn(s"[$auditAPI8TxName - $awrsRefNo ] - Unsuccessful return of data. Status code: $status")
              audit(transactionName = auditWithdrawalTxName, detail = auditMap ++ Map("FailureReason" -> "Other Error"), eventType = eventTypeFailure)
              InternalServerError(f"Unsuccessful return of data. Status code: $status")
          }
      }
  }
}
