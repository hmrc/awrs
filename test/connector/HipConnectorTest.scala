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
    object TestHipConnector extends HipConnector (mockHttpClient, mockAuditConnector, config)
    val awrsRefNo = "XAAW0000010001"
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "HipConnector" must {

    "post the deregistration request to the correct URL" in new Setup {
      val testJson: JsValue = Json.parse(
        """
          |{
          |  "deregistrationReason": "05",
          |  "deregistrationDate": "2025-09-11",
          |  "deregistrationOther": "other reason"
          |}
          |""".stripMargin)
      val expectedURL: String = s"/etmp/RESTAdapter/awrs/subscription/deregistration/$awrsRefNo"

      when(executePost[HttpResponse](Some(expectedURL), testJson))
        .thenReturn(Future.successful(HttpResponse(Status.NOT_FOUND, "{}", Map.empty)))
      TestHipConnector.deRegister(awrsRefNo, testJson)
      Mockito.verify(mockHttpClient, times(1)).post(URI.create(s"http://localhost:9912$expectedURL").toURL)(hc)
    }

    "post the withdrawal request to the correct URL and receive a successful response" in new Setup {
      val withdrawalJson: JsValue = Json.parse(
        """
          |{
          |  "withdrawalReason": "99",
          |  "withdrawalDate": "2025-09-22",
          |  "withdrawalReasonOthers": "other reason"
          |}
          |""".stripMargin)
      val expectedURL: String = s"/etmp/RESTAdapter/awrs/subscription/withdrawal/$awrsRefNo"
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

      val result: HttpResponse = await(TestHipConnector.withdrawal(awrsRefNo, withdrawalJson))
      result.status mustBe Status.OK
      result.body must include("Success")
      Mockito.verify(mockHttpClient, times(1)).post(URI.create(s"http://localhost:9912$expectedURL").toURL)(hc)
    }

    "post the Subscription.update request to the correct URL" in new Setup {
      val expectedURL: String = s"/etmp/RESTAdapter/awrs/subscription/update/$awrsRefNo"
      when(executePutNoBody[HttpResponse]).thenReturn(Future.successful(HttpResponse(200, api6SuccessResponseJson, Map.empty[String, Seq[String]])))

      TestHipConnector.updateSubscription(api6FrontendLTDJson, awrsRefNo)
      Mockito.verify(mockHttpClient, times(1)).put(URI.create(s"http://localhost:9912$expectedURL").toURL)(hc)
    }

    "check status of an application with a valid reference number" in new Setup {
      val expectedURL: String = s"/etmp/RESTAdapter/awrs/subscription/status/$awrsRefNo"

      when(executeGet[HttpResponse](expectedURL))
        .thenReturn(Future.successful(HttpResponse(200, api9SuccessfulResponseJson, Map.empty[String, Seq[String]])))

      val result: Future[HttpResponse] = TestHipConnector.checkStatus(awrsRefNo)
      await(result).json mustBe api9SuccessfulResponseJson
      Mockito.verify(mockHttpClient, times(1)).get(URI.create(s"http://localhost:9912$expectedURL").toURL)(hc)
    }

    "retrieve a subscription display record via lookup" in new Setup {
      val expectedURL: String = s"/etmp/RESTAdapter/awrs/subscription/display/$awrsRefNo"

      when(executeGet[HttpResponse](expectedURL)).thenReturn(Future.successful(HttpResponse(Status.OK, api5EtmpLPJson, Map.empty)))

      val result = TestHipConnector.lookup(awrsRefNo)

      await(result).json mustBe api5EtmpLPJson
      Mockito.verify(mockHttpClient, times(1))
        .get(URI.create(s"http://localhost:9912$expectedURL").toURL)(hc)
    }

    "post a new subscription request to the correct URL" in new Setup {
      val safeId = "SAFE123456"
      val expectedURL: String = s"/etmp/RESTAdapter/awrs/subscription/create/$safeId"
      val responseJson: JsValue = Json.parse(
        """
          |{
          |  "Success": {
          |    "processingDateTime": "2025-09-11T10:30:00Z",
          |     "etmpFormBundleNumber": "123456789012",
          |     "awrsRegistrationNumber": "XAAW0000010001"
          |   }
          |}
          |""".stripMargin)

      when(executePost[HttpResponse](Some(expectedURL), api4EtmpLTDNewBusinessJson)).thenReturn(Future.successful(HttpResponse(Status.OK, responseJson, Map.empty[String, Seq[String]])))

      val result = await(TestHipConnector.subscribe(api4EtmpLTDNewBusinessJson, safeId))
      result.status mustBe Status.OK
      result.json mustBe responseJson
      Mockito.verify(mockHttpClient, times(1)).post(URI.create(s"http://localhost:9912$expectedURL").toURL)(hc)
    }

    "construct required HIP headers" in new Setup {
      val hipHeaders: Map[String, String] = TestHipConnector.headers.toMap

      val expectedOriginatingSystem = config.getConfString("hip.originatingSystem", "")
      hipHeaders.keySet must contain ("X-Originating-System")
      hipHeaders("X-Originating-System") mustBe expectedOriginatingSystem

      hipHeaders.keySet must contain ("X-Transmitting-System")
      hipHeaders("X-Transmitting-System") mustBe "HIP"

      hipHeaders.keySet must contain ("Authorization")
      hipHeaders("Authorization") must startWith ("Basic ")

      hipHeaders.keySet must contain ("correlationid")
      hipHeaders("correlationid").trim.length must be > 0

      hipHeaders.keySet must contain ("X-Receipt-Date")
      hipHeaders("X-Receipt-Date") must fullyMatch regex """\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z"""
    }
  }
}
