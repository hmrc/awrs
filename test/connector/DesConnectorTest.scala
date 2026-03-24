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

package connector

import connectors.DesConnector
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.AwrsTestJson.testRefNo
import utils.BaseSpec

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class DesConnectorTest extends BaseSpec with AnyWordSpecLike {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val mockAuditConnector: AuditConnector = mock[AuditConnector]

  trait Setup extends ConnectorTest {
    object TestDesConnector$ extends DesConnector(mockHttpClient, mockAuditConnector, config, "awrs")
  }

  val config: ServicesConfig = app.injector.instanceOf[ServicesConfig]

  "DesConnector" must {

    "get the status info of an application with a valid reference number and contact number " in new Setup {
      val awrsRefNo = "XAAW0000010001"
      val contactNumber = "0123456789"
      val expectedURL: String = s"/alcohol-wholesaler-register/secure-comms/reg-number/$awrsRefNo/contact-number/$contactNumber"
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(executeGet[HttpResponse](expectedURL)).thenReturn(Future.successful(HttpResponse(200, api11SuccessfulResponseJson, Map.empty[String, Seq[String]])))

      val result: Future[HttpResponse] = TestDesConnector$.getStatusInfo(awrsRefNo, contactNumber)
      await(result).json shouldBe api11SuccessfulResponseJson // if the URL is correct then getStatusInfoSuccess should be returned
    }

    "for a successful API3 submission, return processing date in ETMP response" in new Setup {
      val updateSuccessResponse: JsValue = Json.parse( """{"processingDate":"2015-12-17T09:30:47Z"}""")
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(executePutNoBody[HttpResponse]).thenReturn(Future.successful(HttpResponse(200, updateSuccessResponse, Map.empty[String, Seq[String]])))

      val result: Future[HttpResponse] = TestDesConnector$.updateGrpRepRegistrationDetails(testRefNo, api3FrontendJson)
      await(result).json should be(updateSuccessResponse)
    }
  }

}
