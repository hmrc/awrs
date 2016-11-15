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

package models

import play.api.libs.json.Json
import scala.xml.Elem
case class KnownFact(`type`: String, value:String)

object KnownFact {
  implicit val format = Json.format[KnownFact]
}

case class KnownFactsForService(facts: List[KnownFact])

object KnownFactsForService {
  implicit val formats = Json.format[KnownFactsForService]
}

case class EnrolRequest(portalId: String, serviceName: String, friendlyName: String, knownFacts: Seq[String])

object EnrolRequest {
  implicit val formats = Json.format[EnrolRequest]
}

case class Identifier(`type`: String, value: String)

object Identifier {
  implicit val format = Json.format[Identifier]
}

case class GsoAdminEnrolCredentialIdentifierXmlInput(portalIdentifier : String,serviceName :String,friendlyName :String, identifiers: List[Identifier],
                                                     credIdentifier :String,activated : Boolean = true) {

  val toXml = {
    <GsoAdminEnrolCredentialIdentifierXmlInput xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                   xmlns="urn:GSO-System-Services:external:2.10:GsoAdminEnrolCredentialIdentifierXmlInput">
      <PortalIdentifier>{portalIdentifier}</PortalIdentifier>
      <ServiceName>{serviceName}</ServiceName>
      <EnrolmentFriendlyName>{friendlyName}</EnrolmentFriendlyName>
      <Identifiers>
        {
        for (identifier <- identifiers) yield getIdentifier(identifier)
        }
      </Identifiers>
      <CredentialIdentifier>{credIdentifier}</CredentialIdentifier>
      <Activated>true</Activated>
    </GsoAdminEnrolCredentialIdentifierXmlInput>
  }

  private def getIdentifier(identifier : Identifier):Elem = {
    <Identifier IdentifierType={identifier.`type`}>{identifier.value}</Identifier>
  }
}