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

package services

import connectors.{EtmpConnector, HipConnector}
import metrics.AwrsMetrics
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.mvc.Http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId}
import utils.AwrsTestJson.testRefNo
import utils._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class WithdrawalServiceTest extends BaseSpec with AnyWordSpecLike {
  val mockEtmpConnector: EtmpConnector = mock[EtmpConnector]
  val mockHipConnector: HipConnector = mock[HipConnector]
  val requestJson: JsValue = api8ValidRequestJson
  val ackReference: String = SessionUtils.getUniqueAckNo
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  object TestWithdrawalService extends WithdrawalService(app.injector.instanceOf[AwrsMetrics], mockEtmpConnector, mockHipConnector)

  "Withdrawal Service" must {
    val awrsRefNo = "XAAW0000010001"
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

    "DES connector: returns OK when feature flag is off and request is valid" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.hipSwitch())
      when(mockEtmpConnector.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, api8SuccessfulResponseJson, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalService.withdrawal(api8RequestJson, awrsRefNo)
      val response = await(result)
      response.status shouldBe OK
      response.json shouldBe api8SuccessfulResponseJson
    }

    "DES connector: returns BAD_REQUEST when feature flag is off and request is invalid" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.hipSwitch())
      when(mockEtmpConnector.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, api8FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalService.withdrawal(api8RequestJson, awrsRefNo)
      val response = await(result)
      response.status shouldBe BAD_REQUEST
      response.json shouldBe api8FailureResponseJson
    }

    "DES connector: returns OK when feature flag is on and request is valid" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())
      when(mockHipConnector.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, api8SuccessfulResponseJson, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalService.withdrawal(api8RequestJson, awrsRefNo)
      val response = await(result)
      response.status shouldBe OK
      response.json shouldBe api8SuccessfulResponseJson
    }

    "HIP connector: returns BAD_REQUEST when feature flag is on and request is invalid" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())
      when(mockHipConnector.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, api8FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalService.withdrawal(api8RequestJson, awrsRefNo)
      val response = await(result)
      response.status shouldBe BAD_REQUEST
      response.json shouldBe api8FailureResponseJson
    }

    "successfully process the withdrawal request" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())

      val responseJson: JsValue = Json.parse(
        """
          |{
          |  "success": {
          |    "processingDate": "2025-09-11T10:30:00Z"
          |  }
          |}
          |""".stripMargin)

      when(mockHipConnector.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(Status.CREATED, responseJson, Map.empty[String, Seq[String]])))

      val result = TestWithdrawalService.withdrawal(requestJson, testRefNo)
      await(result).status shouldBe Status.OK
    }

    "respond with appropriate failure status code for a withdrawal request" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())

      val responseJson = Json.parse(
        """
          |{
          |  "error": {
          |    "code": "500",
          |    "message": "Internal Server Error",
          |    "logID": "24B56DEABD748EB11C66897AB601D222"
          |  }
          |}
        """.stripMargin)

      val mockResponse = HttpResponse(Status.INTERNAL_SERVER_ERROR, responseJson, Map("correlationid" -> Seq("123e4567-e89b-12d3-a456-426614174000")))

      when(mockHipConnector.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(mockResponse))

      val result = await(TestWithdrawalService.withdrawal(requestJson, testRefNo))

      result.status mustBe Status.INTERNAL_SERVER_ERROR
      Json.parse(result.body) mustBe responseJson
    }
  }


  "updateRequestForHip" must {

    "transform standard withdrawal reason correctly" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())
      val expectedJson = Json.parse(
        """
          |{
          |  "withdrawalDate": "2025-07-17",
          |  "withdrawalReason": "Applied in error"
          |}
        """.stripMargin)

      val result = TestWithdrawalService.updateRequestForHip(requestJson).get
      result mustBe expectedJson

    }
  }

  "updateResponseForHip" must {
    "remove the success node" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())
      val responseJson = Json.parse(
        """
          |{
          |  "success": {
          |    "processingDate": "2025-07-17T10:00:00Z"
          |  }
          |}
      """.stripMargin)

      val result = Utility.stripSuccessNode(responseJson)

      (result \ "processingDate").as[String] mustBe "2025-07-17T10:00:00Z"
      result.toString must not include "success"
    }
  }
}
