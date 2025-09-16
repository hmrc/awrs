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

package connector

import connectors.HipConnector
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsValue, Json}
import play.mvc.Http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.BaseSpec

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class HipConnectorTest extends BaseSpec with AnyWordSpecLike {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val config: ServicesConfig = app.injector.instanceOf[ServicesConfig]

  trait Setup extends ConnectorTest {
    object TestHipConnector extends HipConnector (mockHttpClient, mockAuditConnector, config, "awrs")
  }

  "HipConnector" must {

    "successfully process HIP deregistration POST request" in new Setup {
      val awrsRefNo = "XAAW0000010001"
      val testJson: JsValue = Json.parse(
        """
          |{
          |  "deregistrationReason": "05",
          |  "deregistrationDate": "2025-09-11",
          |  "deregistrationOther": "other reason"
          |}
          |""".stripMargin)
      val expectedURL: Option[String] = Some(s"/etmp/RESTadapter/awrs/subscription/deregistration/$awrsRefNo")

      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

      val responseJson: JsValue = Json.parse(
        """
          |{
          |  "success": {
          |    "processingDateTime": "2025-09-11T10:30:00Z"
          |  }
          |}
          |""".stripMargin)

      when(executePost[HttpResponse](expectedURL, testJson))
        .thenReturn(Future.successful(HttpResponse(Status.CREATED, responseJson, Map.empty)))

      val result: Future[HttpResponse] = TestHipConnector.deRegister(awrsRefNo, testJson)
      await(result).json shouldBe responseJson
    }
  }
}
