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

    "perform a withdrawal when passed valid json" in {
      when(mockEtmpConnector.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, api8SuccessfulResponseJson, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalService.withdrawal(api8RequestJson, awrsRefNo)
      val response = await(result)
      response.status shouldBe OK
      response.json shouldBe api8SuccessfulResponseJson
    }

    "respond with BadRequest, when withdrawal request fails with a Bad request" in {
      when(mockEtmpConnector.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, api8FailureResponseJson, Map.empty[String, Seq[String]])))
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
          |    "processingDateTime": "2025-09-11T10:30:00Z"
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

    "return BAD_REQUEST status when updateRequestForHip returns JsError" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())

      val invalidJson = Json.parse(
        """
          |{
          |  "withdrawalDate": "2025-07-17"
          |
          |}
    """.stripMargin)

      val result = await(TestWithdrawalService.withdrawal(invalidJson, testRefNo))

      result.status mustBe Status.BAD_REQUEST
      result.body must include("JSON transformation failed")
    }
  }


  "updateRequestForHip" must {

    "transform standard withdrawal reason correctly" in {
      val expectedJson = Json.parse(
        """
          |{
          |  "acknowledgementReference": "ABC123456789",
          |  "withdrawalDate": "2025-07-17",
          |  "withdrawalReason": "01"
          |}
        """.stripMargin)

      val result = TestWithdrawalService.updateRequestForHip(requestJson).get
      result mustBe expectedJson

    }

    "transform 'Others' reason with withdrawalReasonOthers correctly" in {

      val requestJson = Json.parse(
        """
          |{
          |  "acknowledgementReference": "ABC123456789",
          |  "withdrawalDate": "2025-07-17",
          |  "withdrawalReason": "Others",
          |  "withdrawalReasonOthers": "Other text"
          |}
        """.stripMargin)

      val expectedJson = Json.parse(
        """
          |{
          |  "acknowledgementReference": "ABC123456789",
          |  "withdrawalDate": "2025-07-17",
          |  "withdrawalReason": "99",
          |  "withdrawalReasonOthers": "Other text"
          |}
        """.stripMargin)

      val result = TestWithdrawalService.updateRequestForHip(requestJson).get
      result mustBe expectedJson

    }

    "throw an exception when withdrawalReason is invalid" in {
      val inputJson = Json.parse(
        """
          |{
          |  "acknowledgementReference": "$ackReference",
          |  "withdrawalDate": "2025-07-17",
          |  "withdrawalReason": "Invalid Reason"
          |}
        """.stripMargin)

      val exception = intercept[NoSuchElementException] {
        TestWithdrawalService.updateRequestForHip(inputJson)
      }

      exception.getMessage shouldBe "Invalid withdrawalReason received"

    }

    "throw exception when withdrawalReasonOthers is missing for 'Others'" in {
      val inputJson = Json.parse(
        """
          |{
          |  "acknowledgementReference": "$ackReference",
          |  "withdrawalDate": "2025-07-17",
          |  "withdrawalReason": "Others"
          |}
        """.stripMargin)

      val exception = intercept[RuntimeException] {
        TestWithdrawalService.updateRequestForHip(inputJson)
      }

      exception.getMessage shouldBe "Missing 'withdrawalReasonOthers'- this field is required when 'withdrawalReason' is set to 'Others'"
    }
  }

  "updateResponseForHip" must {
    "transform processingDateTime to processingDate and strip off the success node" in {
      val responseJson = Json.parse(
        """
          |{
          |  "success": {
          |    "processingDateTime": "2025-07-17T10:00:00Z"
          |  }
          |}
      """.stripMargin)

      val result = TestWithdrawalService.updateResponseForHip(responseJson)

      (result \ "processingDate").as[String] mustBe "2025-07-17T10:00:00Z"
      result.toString must not include "success"
      result.toString must not include "processingDateTime"
    }

    "throw RuntimeException when success node is missing" in {
      val responseJson = Json.parse(
        """
          |{
          |  "processingDateTime": "2025-07-17T10:00:00Z"
          |}
      """.stripMargin)

      val exception = intercept[RuntimeException] {
        TestWithdrawalService.updateResponseForHip(responseJson)
      }

      exception.getMessage must include("Received response does not contain a 'success' node.")
    }

    "throw RuntimeException when processingDateTime is missing in success node" in {
      val responseJson = Json.parse(
        """
          |{
          |  "success": {
          |    "status": "OK"
          |  }
          |}
      """.stripMargin)

      val exception = intercept[RuntimeException] {
        TestWithdrawalService.updateResponseForHip(responseJson)
      }

      exception.getMessage must include("Received response is missing the 'processingDateTime' key in the 'success' node.")
    }
  }
}
