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

package connectors

import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.LoggingUtils

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class EtmpConnector @Inject()(http: HttpClientV2,
                              val auditConnector: AuditConnector,
                              config: ServicesConfig,
                              @Named("appName") val appName: String)(implicit ec: ExecutionContext) extends RawResponseReads with LoggingUtils {

  lazy val serviceURL: String = config.baseUrl("etmp-hod")
  val baseURI = "/alcohol-wholesaler-register"
  val subscriptionURI = "/subscription/"
  val UpdateGrpRepRegistrationURI = "/registration/safeid/"
  val regimeURI = "/registration/details"
  val secureCommsURI = "/secure-comms/"
  val statusURI = "/status"
  val regNumberURI = "reg-number/"
  val contactNumberURI = "/contact-number/"
  val withdrawalURI = "/withdrawal"
  val deRegistrationURI = "/deregistration"
  val urlHeaderEnvironment: String = config.getConfString("etmp-hod.environment", "")
  val urlHeaderAuthorization: String = s"Bearer ${config.getConfString("etmp-hod.authorization-token", "")}"

  val headers: Seq[(String, String)] = Seq(
    "Environment" -> urlHeaderEnvironment,
    "Authorization" -> urlHeaderAuthorization
  )

  @inline def cPOST[I, O](url: String, body: I)(implicit wts: Writes[I], rds: HttpReads[O], hc: HeaderCarrier): Future[O] =
    http.post(url"$url").withBody(Json.toJson(body)).setHeader(headers: _*).execute[O]

  @inline def cGET[A](url: String)(implicit rds: HttpReads[A], hc: HeaderCarrier): Future[A] =
  http.get(url"$url").setHeader(headers: _*).execute[A]

  @inline def cPUT[I, O](url: String, body: I)(implicit wts: Writes[I], rds: HttpReads[O], hc: HeaderCarrier): Future[O] =
  http.put(url"$url").withBody(Json.toJson(body)).setHeader(headers: _*).execute[O]

  def subscribe(registerData: JsValue, safeId: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    cPOST( s"""$serviceURL$baseURI$subscriptionURI$safeId""", registerData)
  }

  def awrsRegime(safeId: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    cGET(s"""$serviceURL$regimeURI?safeid=$safeId&regime=AWRS""")
  }

  def lookup(awrsRef: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    cGET( s"""$serviceURL$baseURI$subscriptionURI$awrsRef""")
  }

  def updateSubscription(updateData: JsValue, awrsRefNo: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    cPUT(s"""$serviceURL$baseURI$subscriptionURI$awrsRefNo""", updateData)
  }

  def updateGrpRepRegistrationDetails(safeId: String, updateData: JsValue)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    cPUT(s"""$serviceURL$UpdateGrpRepRegistrationURI$safeId""", updateData)
  }

  def checkStatus(awrsRef: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    cGET( s"""$serviceURL$baseURI$subscriptionURI$awrsRef$statusURI""")

  }

  def getStatusInfo(awrsRef: String, contactNumber: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    debug(f"getStatusInfo: $awrsRef - $contactNumber")
    cGET( s"""$serviceURL$baseURI$secureCommsURI$regNumberURI$awrsRef$contactNumberURI$contactNumber""")
  }

  def deRegister(awrsRefNo: String, deRegDetails: JsValue)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    cPOST(s"""$serviceURL$baseURI$subscriptionURI$awrsRefNo$deRegistrationURI""", deRegDetails)
  }

  def withdrawal(awrsRefNo: String, withdrawalData: JsValue)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    cPOST( s"""$serviceURL$baseURI$subscriptionURI$awrsRefNo$withdrawalURI""", withdrawalData)
  }
}
