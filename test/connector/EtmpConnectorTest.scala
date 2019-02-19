/*
 * Copyright 2019 HM Revenue & Customs
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

import java.util.UUID

import config.WSHttp
import connectors.EtmpConnector
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.Mode.Mode
import play.api.libs.json.{JsValue, Json}
import play.api.{Configuration, Play}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.microservice.config.LoadAuditingConfig
import utils.AwrsTestJson.testRefNo
import utils.BaseSpec

import scala.concurrent.Future

class EtmpConnectorTest extends BaseSpec {

  object TestAuditConnector extends AuditConnector {
    override lazy val auditingConfig = LoadAuditingConfig("auditing")
  }

  abstract class MockHttp extends WSHttp  {
//    override val hooks = Seq(AuditingHook)

    override val auditConnector = TestAuditConnector

    override def appName = Play.configuration.getString("appName").getOrElse("awrs")
  }

  val mockWSHttp = mock[MockHttp]

  object TestEtmpConnector extends EtmpConnector {
    override val http = mockWSHttp
    override val urlHeaderEnvironment: String = config("etmp-hod").getString("environment").getOrElse("")
    override val urlHeaderAuthorization: String = s"Bearer ${config("etmp-hod").getString("authorization-token").getOrElse("")}"

    override protected def mode: Mode = Play.current.mode

    override protected def runModeConfiguration: Configuration = Play.current.configuration
  }

  before {
    reset(mockWSHttp)
  }

  "EtmpConnector" should {

    "create correct AWRS subscription POST request" in {
      val safeId = "XA0001234567890"
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val expectedPostRequest = "http://localhost:9912/alcohol-wholesaler-register/subscription/XA0001234567890"
      val createdPostRequest = TestEtmpConnector.serviceURL + TestEtmpConnector.baseURI + TestEtmpConnector.subscriptionURI + safeId
      createdPostRequest should be(expectedPostRequest)
    }

    "for a successful submission, return AWRS Registration Number response" in {
      val api4Json = api4EtmpSOPJson
      val safeId = "XA0001234567890"
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, responseJson = Some(api4SuccessResponse))))
      val result = TestEtmpConnector.subscribe(api4Json, safeId)
      await(result).json should be(api4SuccessResponse)
    }

    "lookup an application with a valid reference number " in {
      val lookupSuccess = Json.parse( """{"Reason": "All ok"}""")
      val awrsRefNo = "XAAW0000012345"
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, responseJson = Some(lookupSuccess))))
      val result = TestEtmpConnector.lookup(awrsRefNo)
      await(result).json shouldBe lookupSuccess
    }

    "create correct AWRS subscription PUT request" in {
      val awrsRefNo = "XAAW000003457890"
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      val expectedPutRequest = "http://localhost:9912/alcohol-wholesaler-register/subscription/XAAW000003457890"
      val createdPutRequest = TestEtmpConnector.serviceURL + TestEtmpConnector.baseURI + TestEtmpConnector.subscriptionURI + awrsRefNo
      createdPutRequest should be(expectedPutRequest)
    }

    "for a successful API6 submission, return etmp processing date and form bundle number in response" in {
      val awrsRefNo = "XAAW000003457890"
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.PUT[JsValue, HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, responseJson = Some(api6SuccessResponseJson))))
      val result = TestEtmpConnector.updateSubscription(api6FrontendLTDJson, awrsRefNo)
      await(result).json should be(api6SuccessResponseJson)
    }

    "check status of an application with a valid reference number " in {
      val awrsRefNo = "XAAW0000010001"
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.GET[HttpResponse](Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, responseJson = Some(api9SuccessfulResponseJson))))
      val result = TestEtmpConnector.checkStatus(awrsRefNo)
      await(result).json shouldBe api9SuccessfulResponseJson
    }

    "get the status info of an application with a valid reference number and contact number " in {
      val awrsRefNo = "XAAW0000010001"
      val contactNumber = "0123456789"
      val expectedURL: String = s"/alcohol-wholesaler-register/secure-comms/reg-number/$awrsRefNo/contact-number/$contactNumber"
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.GET[HttpResponse](Matchers.endsWith(expectedURL))(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, responseJson = Some(api11SuccessfulResponseJson))))
      val result = TestEtmpConnector.getStatusInfo(awrsRefNo, contactNumber)
      await(result).json shouldBe api11SuccessfulResponseJson // if the URL is correct then getStatusInfoSuccess should be returned
    }

    "correct de-registation path" in {
      val awrsRefNo = "XAAW0000010001"
      val dummyData: JsValue = Json.parse("true") // dummy data does not matter since the call is mocked
      val expectedURL: String = s"/alcohol-wholesaler-register/subscription/$awrsRefNo/deregistration"
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.endsWith(expectedURL), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, responseJson = Some(api10SuccessfulResponseJson))))
      val result = TestEtmpConnector.deRegister(awrsRefNo, dummyData)
      await(result).json shouldBe api10SuccessfulResponseJson // if the URL is correct then deRegisterSuccess should be returned
    }

    "for a successful withdrawal, return a success response" in {
      val awrsRefNo = "XAAW0000010001"
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(),Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(200, responseJson = Some(api8SuccessfulResponseJson))))
      val result = TestEtmpConnector.withdrawal(awrsRefNo, api8RequestJson)
      await(result).json should be(api8SuccessfulResponseJson)
    }

    "for a successful API3 submission, return processing date in ETMP response" in {
      val updateSuccessResponse = Json.parse( """{"processingDate":"2015-12-17T09:30:47Z"}""")
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.PUT[JsValue, HttpResponse](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any(),Matchers.any())).thenReturn(Future.successful(HttpResponse(200, responseJson = Some(updateSuccessResponse))))
      val result = TestEtmpConnector.updateGrpRepRegistrationDetails(testRefNo,api3FrontendJson)
      await(result).json should be(updateSuccessResponse)
    }
  }

}
