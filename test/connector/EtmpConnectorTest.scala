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

import connectors.EtmpConnector
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

class EtmpConnectorTest extends BaseSpec with AnyWordSpecLike {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val mockAuditConnector: AuditConnector = mock[AuditConnector]

  trait Setup extends ConnectorTest {
    object TestEtmpConnector extends EtmpConnector(mockHttpClient, mockAuditConnector, config, "awrs")
  }

  val config: ServicesConfig = app.injector.instanceOf[ServicesConfig]

  "EtmpConnector" must {

    "create correct AWRS subscription POST request" in new Setup {
      val safeId = "XA0001234567890"
      val expectedPostRequest = "http://localhost:9912/alcohol-wholesaler-register/subscription/XA0001234567890"
      val createdPostRequest: String = TestEtmpConnector.serviceURL + TestEtmpConnector.baseURI + TestEtmpConnector.subscriptionURI + safeId
      createdPostRequest should be(expectedPostRequest)
    }

    "for a successful submission, return AWRS Registration Number response" in new Setup {
      val api4Json: JsValue = api4EtmpSOPJson
      val safeId = "XA0001234567890"
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(executePostNoBody[HttpResponse]()).thenReturn(Future.successful(HttpResponse(200, api4SuccessResponse, Map.empty[String, Seq[String]])))

      val result: Future[HttpResponse] = TestEtmpConnector.subscribe(api4Json, safeId)
      await(result).json should be(api4SuccessResponse)
    }

    "lookup an application with a valid reference number " in new Setup {
      val lookupSuccess: JsValue = Json.parse( """{"Reason": "All ok"}""")
      val awrsRefNo = "XAAW0000012345"
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(executeGet[HttpResponse]).thenReturn(Future.successful(HttpResponse(200, lookupSuccess, Map.empty[String, Seq[String]])))

      val result: Future[HttpResponse] = TestEtmpConnector.lookup(awrsRefNo)
      await(result).json shouldBe lookupSuccess
    }

    "create correct AWRS subscription PUT request" in new Setup {
      val awrsRefNo = "XAAW000003457890"
      val expectedPutRequest = "http://localhost:9912/alcohol-wholesaler-register/subscription/XAAW000003457890"
      val createdPutRequest: String = TestEtmpConnector.serviceURL + TestEtmpConnector.baseURI + TestEtmpConnector.subscriptionURI + awrsRefNo
      createdPutRequest should be(expectedPutRequest)
    }

    "for a successful API6 submission, return etmp processing date and form bundle number in response" in new Setup {
      val awrsRefNo = "XAAW000003457890"
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(executePutNoBody[HttpResponse]).thenReturn(Future.successful(HttpResponse(200, api6SuccessResponseJson, Map.empty[String, Seq[String]])))

      val result: Future[HttpResponse] = TestEtmpConnector.updateSubscription(api6FrontendLTDJson, awrsRefNo)
      await(result).json should be(api6SuccessResponseJson)
    }

    "check status of an application with a valid reference number " in new Setup {
      val awrsRefNo = "XAAW0000010001"
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(executeGet[HttpResponse]).thenReturn(Future.successful(HttpResponse(200, api9SuccessfulResponseJson, Map.empty[String, Seq[String]])))

      val result: Future[HttpResponse] = TestEtmpConnector.checkStatus(awrsRefNo)
      await(result).json shouldBe api9SuccessfulResponseJson
    }

    "get the status info of an application with a valid reference number and contact number " in new Setup {
      val awrsRefNo = "XAAW0000010001"
      val contactNumber = "0123456789"
      val expectedURL: String = s"/alcohol-wholesaler-register/secure-comms/reg-number/$awrsRefNo/contact-number/$contactNumber"
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(executeGet[HttpResponse](expectedURL)).thenReturn(Future.successful(HttpResponse(200, api11SuccessfulResponseJson, Map.empty[String, Seq[String]])))

      val result: Future[HttpResponse] = TestEtmpConnector.getStatusInfo(awrsRefNo, contactNumber)
      await(result).json shouldBe api11SuccessfulResponseJson // if the URL is correct then getStatusInfoSuccess should be returned
    }

    "correct de-registation path" in new Setup {
      val awrsRefNo = "XAAW0000010001"
      val dummyData: JsValue = Json.parse("true") // dummy data does not matter since the call is mocked
      val expectedURL: String = s"/alcohol-wholesaler-register/subscription/$awrsRefNo/deregistration"
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(executePostNoBody[HttpResponse](Some(expectedURL))).thenReturn(Future.successful(HttpResponse(200, api10SuccessfulResponseJson, Map.empty[String, Seq[String]])))

      val result: Future[HttpResponse] = TestEtmpConnector.deRegister(awrsRefNo, dummyData)
      await(result).json shouldBe api10SuccessfulResponseJson // if the URL is correct then deRegisterSuccess should be returned
    }

    "for a successful withdrawal, return a success response" in new Setup {
      val awrsRefNo = "XAAW0000010001"
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(executePostNoBody[HttpResponse]()).thenReturn(Future.successful(HttpResponse(200, api8SuccessfulResponseJson, Map.empty[String, Seq[String]])))

      val result: Future[HttpResponse] = TestEtmpConnector.withdrawal(awrsRefNo, api8RequestJson)
      await(result).json should be(api8SuccessfulResponseJson)
    }

    "for a successful API3 submission, return processing date in ETMP response" in new Setup {
      val updateSuccessResponse: JsValue = Json.parse( """{"processingDate":"2015-12-17T09:30:47Z"}""")
      implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(executePutNoBody[HttpResponse]).thenReturn(Future.successful(HttpResponse(200, updateSuccessResponse, Map.empty[String, Seq[String]])))

      val result: Future[HttpResponse] = TestEtmpConnector.updateGrpRepRegistrationDetails(testRefNo, api3FrontendJson)
      await(result).json should be(updateSuccessResponse)
    }
  }

}
