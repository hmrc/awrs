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
import models.{ApiType, StatusInfoType}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.LoggingUtils

import scala.concurrent.{ExecutionContext, Future}

class StatusInfoController @Inject()(val auditConnector: AuditConnector,
                                     metrics: AwrsMetrics,
                                     val statusInfoService: EtmpStatusInfoService,
                                     val etmpRegimeService: EtmpRegimeService,
                                     val enrolmentService: EnrolmentService,
                                     cc: ControllerComponents,
                                     @Named("appName") val appName: String)(implicit ec: ExecutionContext) extends BackendController(cc) with LoggingUtils {

  def getStatusInfo(awrsRef: String, contactNumber: String): Action[AnyContent] = Action.async {
    implicit request =>
      info(s"[$auditAPI11TxName - $awrsRef ] - hit getStatusInfo controller ")
      val apiType: ApiType.Value = ApiType.API11GetStatusInfo
      val timer = metrics.startTimer(apiType)
      statusInfoService.getStatusInfo(awrsRef, contactNumber) map {
        result =>
          timer.stop()
          result.status match {
            case OK =>
              val convertedJson: StatusInfoType = result.json.as[StatusInfoType](StatusInfoType.reader)
              convertedJson match {
                case StatusInfoType(Some(_)) =>
                  metrics.incrementSuccessCounter(apiType)
                  Ok(Json.toJson(convertedJson))
                // this case should never happen, since at least one of the response types should be returned
                case StatusInfoType(None) =>
                  metrics.incrementFailedCounter(apiType)
                  audit(auditAPI11TxName, Map("awrsRef" -> awrsRef.toString, "corrupt-etmp-data" -> result.json.toString()), eventTypeInternalServerError)
                  warn(s"[$auditAPI11TxName - $awrsRef ] - corrupt etmp data")
                  InternalServerError(result.body)
              }
            case NOT_FOUND =>
              metrics.incrementFailedCounter(apiType)
              warn(s"[$auditAPI11TxName - $awrsRef ] - The remote endpoint has indicated that no data can be found")
              NotFound(result.body)
            case BAD_REQUEST =>
              metrics.incrementFailedCounter(apiType)
              warn(s"[$auditAPI11TxName - $awrsRef ] - The request has not passed validation")
              BadRequest(result.body)
            case SERVICE_UNAVAILABLE =>
              metrics.incrementFailedCounter(apiType)
              warn(s"[$auditAPI11TxName - $awrsRef ] - Dependant systems are currently not responding")
              ServiceUnavailable(result.body)
            case INTERNAL_SERVER_ERROR =>
              metrics.incrementFailedCounter(apiType)
              warn(s"[$auditAPI11TxName - $awrsRef ] - WSO2 is currently experiencing problems that require live service intervention")
              InternalServerError(result.body)
            case status@_ =>
              metrics.incrementFailedCounter(apiType)
              warn(s"[$auditAPI11TxName - $awrsRef ] - Unsuccessful return of data. Status code: $status")
              InternalServerError(f"Unsuccessful return of data. Status code: $status")
          }
      }
  }

  def enrolledUsers(safeID: String): Action[AnyContent] = Action.async { implicit request =>
    etmpRegimeService.getEtmpBusinessDetails(safeID).flatMap {
      case Some(details) =>
        enrolmentService.awrsUsers(details.regimeRefNumber).map {
          case Right(users) => Ok(Json.toJson(users))
          case Left(err) =>
            warn(s"[AWRS][enrolledUsers for safeId] - returned ${Status(err)} $err when retrieving awrs users")
            Status(err)(s"""Error when checking enrolment store for ${details.regimeRefNumber}""")
        }
      case None =>
        warn(s"""[AWRS][enrolledUsers for safeID] - No business details found for safeID $safeID""")
        Future.successful(NotFound(s"""AWRS enrolled Business Details not found for $safeID"""))
    }
  }

}