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
import org.mockito.Mockito
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsValue, Json}
import play.mvc.Http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.BaseSpec

import java.net.URI
import scala.concurrent.{ExecutionContext, Future}

class HipConnectorTest extends BaseSpec with AnyWordSpecLike {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val config: ServicesConfig = app.injector.instanceOf[ServicesConfig]

  trait Setup extends ConnectorTest {
    object TestHipConnector extends HipConnector (mockHttpClient, mockAuditConnector, config, "awrs")
  }

  "HipConnector" must {

    "post the deregistration request to the correct URL" in new Setup {
      val awrsRefNo = "XAAW0000010001"
      val testJson: JsValue = Json.parse(
        """
          |{
          |  "deregistrationReason": "05",
          |  "deregistrationDate": "2025-09-11",
          |  "deregistrationOther": "other reason"
          |}
          |""".stripMargin)
      val expectedURL: String = s"/etmp/RESTadapter/awrs/subscription/deregistration/$awrsRefNo"

      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(executePost[HttpResponse](Some(expectedURL), testJson))
        .thenReturn(Future.successful(HttpResponse(Status.NOT_FOUND, "{}", Map.empty)))
      TestHipConnector.deRegister(awrsRefNo, testJson)
      Mockito.verify(mockHttpClient, times(1)).post(URI.create(s"http://localhost:9912$expectedURL").toURL)(hc)
    }

    "post the withdrawal request to the correct URL and receive a successful response" in new Setup {
      val awrsRefNo = "XAAW0000010001"
      val withdrawalJson: JsValue = Json.parse(
        """
          |{
          |  "withdrawalReason": "99",
          |  "withdrawalDate": "2025-09-22",
          |  "withdrawalReasonOthers": "other reason"
          |}
          |""".stripMargin)
      val expectedURL: String = s"/etmp/RESTadapter/awrs/subscription/withdrawal/$awrsRefNo"
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val responseJson: JsValue = Json.parse(
        """
          |{
          |  "Success": {
          |    "processingDateTime": "2025-09-11T10:30:00Z"
          |   }
          |}
          |""".stripMargin)
      val mockResponse = HttpResponse(Status.OK, responseJson, Map.empty)

      when(executePost[HttpResponse](Some(expectedURL), withdrawalJson))
        .thenReturn(Future.successful(mockResponse))
      TestHipConnector.withdrawal(awrsRefNo, withdrawalJson)

      Mockito.verify(mockHttpClient, times(1)).post(URI.create(s"http://localhost:9912$expectedURL").toURL)(hc)
      val result: HttpResponse = await(TestHipConnector.withdrawal(awrsRefNo, withdrawalJson))
      result.status mustBe Status.OK
      result.body must include("Success")
    }
  }
}
