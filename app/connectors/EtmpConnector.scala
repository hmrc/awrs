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

package connectors

import config.WSHttp
import play.api.libs.json.{JsValue, Writes}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.Authorization
import utils.LoggingUtils

import scala.concurrent.Future

trait EtmpConnector extends ServicesConfig with RawResponseReads with LoggingUtils {

  lazy val serviceURL = baseUrl("etmp-hod")
  val baseURI = "/alcohol-wholesaler-register"
  val subscriptionURI = "/subscription/"
  val UpdateGrpRepRegistrationURI = "/registration/safeid/"
  val secureCommsURI = "/secure-comms/"
  val statusURI = "/status"
  val regNumberURI = "reg-number/"
  val contactNumberURI = "/contact-number/"
  val withdrawalURI = "/withdrawal"
  val deRegistrationURI = "/deregistration"
  val urlHeaderEnvironment: String
  val urlHeaderAuthorization: String

  val http: HttpGet with HttpPost with HttpPut = WSHttp

  @inline def cPOST[I, O](url: String, body: I, headers: Seq[(String, String)] = Seq.empty)(implicit wts: Writes[I], rds: HttpReads[O], hc: HeaderCarrier) =
    http.POST[I, O](url, body, headers)(wts = wts, rds = rds, hc = createHeaderCarrier(hc))

  @inline def cGET[A](url: String)(implicit rds: HttpReads[A], hc: HeaderCarrier) =
    http.GET[A](url)(rds, hc = createHeaderCarrier(hc))

  @inline def cPUT[I, O](url: String, body: I)(implicit wts: Writes[I], rds: HttpReads[O], hc: HeaderCarrier) =
    http.PUT[I, O](url, body)(wts, rds, hc = createHeaderCarrier(hc))

  def subscribe(registerData: JsValue, safeId: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    cPOST( s"""$serviceURL$baseURI$subscriptionURI$safeId""", registerData)
  }

  def createHeaderCarrier(headerCarrier: HeaderCarrier): HeaderCarrier = {
    headerCarrier.withExtraHeaders("Environment" -> urlHeaderEnvironment).copy(authorization = Some(Authorization(urlHeaderAuthorization)))
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

object EtmpConnector extends EtmpConnector {
  override val urlHeaderEnvironment: String = config("etmp-hod").getString("environment").getOrElse("")
  override val urlHeaderAuthorization: String = s"Bearer ${config("etmp-hod").getString("authorization-token").getOrElse("")}"
}
