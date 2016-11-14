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

import models.KnownFactsForService
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import config.WSHttp
import uk.gov.hmrc.play.http._

import scala.concurrent.Future


trait GovernmentGatewayAdminConnector extends ServicesConfig with RawResponseReads{

  lazy val serviceURL = baseUrl("government-gateway-admin")

  val addKnownFactsURI = "known-facts"

  val url = s"""$serviceURL/government-gateway-admin/service"""

  val http: HttpGet with HttpPost = WSHttp

  def addKnownFacts(knownFacts : KnownFactsForService)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = postKnownFact(knownFacts, addKnownFactsURI)

  def postKnownFact(knownFacts: KnownFactsForService, destination: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    val AWRS = "HMRC-AWRS-ORG"
    val jsonData = Json.toJson(knownFacts)
    val postUrl = s"""$url/$AWRS/$destination"""
    http.POST[JsValue, HttpResponse](postUrl, jsonData)
  }
}

object GovernmentGatewayAdminConnector extends GovernmentGatewayAdminConnector
