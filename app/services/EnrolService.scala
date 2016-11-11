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

package services

import connectors.{AuthConnector, GovernmentGatewayAdminConnector}
import models.{EnrolRequest, GsoAdminEnrolCredentialIdentifierXmlInput, Identifier}
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import utils.GGConstants._

import scala.concurrent.Future

/**
  * Created by user on 09/11/16.
  */
trait EnrolService {
  val ggAdminConnector: GovernmentGatewayAdminConnector

  val authConnector: AuthConnector

  def enrolAWRS(response: HttpResponse, safeId: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] =
    createEnrolment(response, safeId) flatMap ggAdminConnector.enrol

  private def createEnrolment(response: HttpResponse, safeId: String)(implicit headerCarrier: HeaderCarrier): Future[GsoAdminEnrolCredentialIdentifierXmlInput] = {

    val json = response.json
    val awrsRefNumber = (json \ "awrsRegistrationNumber").as[String]

    getCredIdentifier map {
      credIdentifier =>
        GsoAdminEnrolCredentialIdentifierXmlInput(portalIdentifier = mdtp,
          serviceName = service,
          friendlyName = friendly,
          identifiers = List(Identifier("AWRSRefNumber", awrsRefNumber),Identifier("SAFEID", safeId)),
          credIdentifier = credIdentifier)
    }
  }

  def getCredId(authorityJson: JsValue): String = (authorityJson \ "credentials" \ "gatewayId").as[String]

  def getCredIdentifier: Future[String] = {
    authConnector.getAuthority().map {
      authority => getCredId(authority)
    }
  }
}

  object EnrolService extends EnrolService {
    val ggAdminConnector = GovernmentGatewayAdminConnector
    val authConnector = AuthConnector
  }
