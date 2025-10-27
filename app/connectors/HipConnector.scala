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

package connectors

import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.LoggingUtils

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import java.util.{Base64, UUID}
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class HipConnector @Inject() (http: HttpClientV2,
                              val auditConnector: AuditConnector,
                              config: ServicesConfig,
                              @Named("appName") val appName: String)
                             (implicit ec: ExecutionContext)
  extends RawResponseReads with LoggingUtils {

  lazy val serviceURL: String = config.baseUrl("hip")
  val baseURI: String = "/etmp/RESTadapter/awrs"
  val subscriptionURI: String = "/subscription"
  private val displayURI: String = "/display/"
  val withdrawalURI = "/withdrawal/"
  val deRegistrationURI: String = "/deregistration/"
  private val transmittingSystem = "HIP"

  private val clientId: String = config.getConfString("hip.clientId", "")
  private val clientSecret: String = config.getConfString("hip.clientSecret", "")
  private val authorizationToken: String = Base64.getEncoder.encodeToString(s"$clientId:$clientSecret".getBytes("UTF-8"))

  val headers: Seq[(String, String)] = Seq(
    "correlationid" -> UUID.randomUUID().toString,
    "X-Originating-System" -> appName,
    "X-Receipt-Date" -> retrieveCurrentUkTimestamp,
    "X-Transmitting-System" -> transmittingSystem,
    "Authorization" -> s"Basic $authorizationToken"
  )

  private def retrieveCurrentUkTimestamp: String = {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    formatter.format(ZonedDateTime.now(ZoneId.of("Europe/London")))
  }

  def deRegister(awrsRefNo: String, deRegDetails: JsValue)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    cPOST(s"""$serviceURL$baseURI$subscriptionURI$deRegistrationURI$awrsRefNo""", deRegDetails)
  }

  def withdrawal(awrsRefNo: String, withdrawalData: JsValue)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    cPOST( s"""$serviceURL$baseURI$subscriptionURI$withdrawalURI$awrsRefNo""", withdrawalData)
  }

  def lookup(awrsRef: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    cGET( s"""$serviceURL$baseURI$subscriptionURI$displayURI$awrsRef""")
  }

  @inline def cPOST[I, O](url: String, body: I)(implicit wts: Writes[I], rds: HttpReads[O], hc: HeaderCarrier): Future[O] = {
    http.post(url"$url").withBody(Json.toJson(body)).setHeader(headers: _*).execute[O]
  }

  @inline def cGET[A](url: String)(implicit rds: HttpReads[A], hc: HeaderCarrier): Future[A] =
    http.get(url"$url").setHeader(headers: _*).execute[A]

}
