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

package connectors

import models.GsoAdminEnrolCredentialIdentifierXmlInput
import play.api.http.HeaderNames._
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import play.api.Logger
import play.api.http.Status._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import config.WSHttp
import play.api.http.ContentTypes.XML
import uk.gov.hmrc.play.http.HttpReads

trait TestSpikeAuthConnector extends ServicesConfig with RawResponseReads {

  // $COVERAGE-OFF$
  def serviceUrl:String = baseUrl("auth")
  def http: HttpGet with HttpPost with HttpPut
  val authorityUri: String = "auth/authority"
  lazy val serviceURL = baseUrl("government-gateway-admin")
  lazy val ggpServiceUrl: String = baseUrl("government-gateway-proxy")

  val ggUri = "government-gateway-proxy"

  val addKnownFactsURI = "known-facts"

  val url = s"""$serviceURL/government-gateway-admin/service"""

  def getAuthority()(implicit hc: HeaderCarrier): Future[JsValue] = {

    val getUrl = s"""$serviceUrl/$authorityUri"""
    Logger.info(s"[AuthConnector][agentReferenceNo] - GET $getUrl")
    http.GET[HttpResponse](getUrl) map { response =>
      Logger.info(s"[AuthConnector][agentReferenceNo] - RESPONSE status: ${response.status}, body: ${response.body}")
      response.status match {
        case OK => response.json
        case status => throw new RuntimeException("No authority found")
      }
    }
  }

  def enrol(gsoAdminEnrolCredentialIdentifierXmlInput: GsoAdminEnrolCredentialIdentifierXmlInput)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = enrolAwrs(gsoAdminEnrolCredentialIdentifierXmlInput)

  def enrolAwrs(gsoAdminEnrolCredentialIdentifierXmlInput: GsoAdminEnrolCredentialIdentifierXmlInput)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse]  = {
    val ggEnrolUrl = ggpServiceUrl + s"/$ggUri/api/admin/GsoAdminEnrolCredentialIdentifier"

    println("gsoAdminEnrolCredentialIdentifierXmlInput.toXml" + gsoAdminEnrolCredentialIdentifierXmlInput.toXml)
    http.POSTString(ggEnrolUrl, gsoAdminEnrolCredentialIdentifierXmlInput.toXml.toString, Seq(CONTENT_TYPE -> XML))
  }
  // $COVERAGE-ON$
}

object TestSpikeAuthConnector extends TestSpikeAuthConnector {
  // $COVERAGE-OFF$
  val http: HttpGet with HttpPost with HttpPut = WSHttp
  // $COVERAGE-ON$
}