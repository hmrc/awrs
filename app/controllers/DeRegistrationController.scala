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

package controllers

import javax.inject.{Inject, Named}
import metrics.AwrsMetrics
import models.{ApiType, DeRegistration, DeRegistrationType}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.LoggingUtils

import scala.concurrent.ExecutionContext


class DeRegistrationController @Inject()(val auditConnector: AuditConnector,
                                         metrics: AwrsMetrics,
                                         deRegistrationService: DeRegistrationService,
                                         cc: ControllerComponents,
                                         @Named("appName") val appName: String)(implicit ec: ExecutionContext) extends BackendController(cc) with LoggingUtils {

  def deRegistration(awrsRef: String): Action[AnyContent] = Action.async {
    implicit request =>
      info(s"[API10 - $awrsRef ] - hit deRegistration controller ")

      val feJson: JsValue = request.body.asJson.get
      val deRegistration: DeRegistration = Json.parse(feJson.toString()).as[DeRegistration]

      val apiType: ApiType.Value = ApiType.API10DeRegistration
      val timer = metrics.startTimer(apiType)
      val deRegistrationJsValue = Json.toJson(deRegistration)
      deRegistrationService.deRegistration(awrsRef, deRegistrationJsValue) map {
        result =>
          timer.stop()
          result.status match {
            case OK =>
              val convertedJson = result.json.as[DeRegistrationType](DeRegistrationType.etmpReader)
              convertedJson match {
                case DeRegistrationType(Some(_)) =>
                  metrics.incrementSuccessCounter(apiType)
                  warn(s"[$auditAPI10TxName - $awrsRef ] - Successful return of data")
                  Ok(Json.toJson(convertedJson))
                // this case should never happen, since at least one of the response types should be returned
                case DeRegistrationType(None) =>
                  metrics.incrementFailedCounter(apiType)
                  audit(auditAPI10TxName, Map("awrsRef" -> awrsRef.toString, "Corrupt-ETMP-Data" -> result.json.toString()), eventTypeInternalServerError)
                  err(s"[$auditAPI10TxName - $awrsRef ] - corrupt etmp data")
                  InternalServerError(result.body)
              }
            case NOT_FOUND =>
              metrics.incrementFailedCounter(apiType)
              err(s"[$auditAPI10TxName - $awrsRef ] - The remote endpoint has indicated that no data can be found")
              NotFound(result.body)
            case BAD_REQUEST =>
              metrics.incrementFailedCounter(apiType)
              err(s"[$auditAPI10TxName - $awrsRef ] - The request has not passed validation")
              BadRequest(result.body)
            case SERVICE_UNAVAILABLE =>
              metrics.incrementFailedCounter(apiType)
              err(s"[$auditAPI10TxName - $awrsRef ] - Dependant systems are currently not responding")
              ServiceUnavailable(result.body)
            case INTERNAL_SERVER_ERROR =>
              metrics.incrementFailedCounter(apiType)
              err(s"[$auditAPI10TxName - $awrsRef ] - WSO2 is currently experiencing problems that require live service intervention")
              InternalServerError(result.body)
            case status@_ =>
              metrics.incrementFailedCounter(apiType)
              err(s"[$auditAPI10TxName - $awrsRef ] - Unsuccessful return of data")
              InternalServerError(f"Unsuccessful return of data. Status code: $status")
          }
      }
  }
}
