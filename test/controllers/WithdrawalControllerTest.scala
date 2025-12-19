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

package controllers

import audit.TestAudit
import metrics.AwrsMetrics
import org.mockito.ArgumentMatchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.WithdrawalService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import utils.AwrsTestJson.testRefNo
import utils.BaseSpec

import scala.concurrent.{ExecutionContext, Future}

class WithdrawalControllerTest extends BaseSpec with AnyWordSpecLike {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val mockWithdrawalService: WithdrawalService = mock[WithdrawalService]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val awrsMetrics: AwrsMetrics = app.injector.instanceOf[AwrsMetrics]
  val cc: ControllerComponents = app.injector.instanceOf[ControllerComponents]

  object TestWithdrawalController extends WithdrawalController(mockAuditConnector, awrsMetrics, mockWithdrawalService, cc, "awrs") {
    override val audit: Audit = new TestAudit(mockAuditConnector)
  }

  "For API 8, Withdrawal Controller " must {

    "check success response is transported correctly" in {
      when(mockWithdrawalService.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, api8SuccessfulResponseJson, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalController.withdrawal(testRefNo).apply(FakeRequest().withJsonBody(api8RequestJson))
      status(result) shouldBe OK
      await(result)
      contentAsJson(result) shouldBe api8SuccessfulResponseJson
    }

    "check failure response is transported correctly" in {
      when(mockWithdrawalService.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, api8FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalController.withdrawal(testRefNo).apply(FakeRequest().withJsonBody(api8RequestJson))
      status(result) shouldBe OK
      await(result)
      contentAsJson(result) shouldBe api8FailureResponseJson
    }

    "return BAD REQUEST error from HODS when passed an invalid awrs reference" in {
      when(mockWithdrawalService.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, api8FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalController.withdrawal(testRefNo).apply(FakeRequest().withJsonBody(api8RequestJson))
      status(result) shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe api8FailureResponseJson
    }

    "return NOT FOUND error from HODS when passed an invalid awrs reference" in {
      when(mockWithdrawalService.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, api8FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalController.withdrawal(testRefNo).apply(FakeRequest().withJsonBody(api8RequestJson))
      status(result) shouldBe NOT_FOUND
      contentAsJson(result) shouldBe api8FailureResponseJson
    }

    "return SERVICE UNAVAILABLE error from HODS when passed an invalid awrs reference" in {
      when(mockWithdrawalService.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, api8FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalController.withdrawal(testRefNo).apply(FakeRequest().withJsonBody(api8RequestJson))
      status(result) shouldBe SERVICE_UNAVAILABLE
      contentAsJson(result) shouldBe api8FailureResponseJson
    }

    "return INTERNAL SERVER ERROR error from HODS when passed an invalid awrs reference" in {
      when(mockWithdrawalService.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, api8FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalController.withdrawal(testRefNo).apply(FakeRequest().withJsonBody(api8RequestJson))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsJson(result) shouldBe api8FailureResponseJson
    }

    "return NOT_FOUND for 422 with error code 002" in {

      val error002 = """{
                    |  "errors": {
                    |    "processingDate": "2025-12-02T13:14:41Z",
                    |    "code": "002",
                    |    "text": "No records found"
                    |  }
                    |}
                    |  """.stripMargin
      when(mockWithdrawalService.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error002, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalController.withdrawal(testRefNo).apply(FakeRequest().withJsonBody(api8RequestJson))
      status(result) shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.parse(error002)
    }

    "return BAD_REQUEST error for 422 with error code 003" in {

      val error003 = """{
                    |  "errors": {
                    |    "processingDate": "2025-12-02T13:14:41Z",
                    |    "code": "003",
                    |    "text": "Request could not be processed"
                    |  }
                    |}
                    |  """.stripMargin

      when(mockWithdrawalService.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error003, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalController.withdrawal(testRefNo).apply(FakeRequest().withJsonBody(api8RequestJson))
      status(result) shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe Json.parse(error003)
    }

    "return BAD_REQUEST error for 422 with error code 004" in {

      val error004 = """{
                    |  "errors": {
                    |    "processingDate": "2025-12-02T13:14:41Z",
                    |    "code": "004",
                    |    "text": "Duplicate submission acknowledgment reference"
                    |  }
                    |}
                    |  """.stripMargin

      when(mockWithdrawalService.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error004, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalController.withdrawal(testRefNo).apply(FakeRequest().withJsonBody(api8RequestJson))
      status(result) shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe Json.parse(error004)
    }

    "return BAD_REQUEST error for 422 with error code 005" in {

      val error005 = """{
                    |  "errors": {
                    |    "processingDate": "2025-12-02T13:14:41Z",
                    |    "code": "005",
                    |    "text": "No Form Bundle found"
                    |  }
                    |}
                    |  """.stripMargin

      when(mockWithdrawalService.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error005, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalController.withdrawal(testRefNo).apply(FakeRequest().withJsonBody(api8RequestJson))
      status(result) shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe Json.parse(error005)
    }

    "return INTERNAL_SERVER_ERROR error for 422 with error code 999" in {

      val error999 = """{
                    |  "errors": {
                    |    "processingDate": "2025-12-02T13:14:41Z",
                    |    "code": "999",
                    |    "text": "Technical error"
                    |  }
                    |}
                    |  """.stripMargin

      when(mockWithdrawalService.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error999, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalController.withdrawal(testRefNo).apply(FakeRequest().withJsonBody(api8RequestJson))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result).contains(error999) shouldBe true
    }

    "return INTERNAL_SERVER_ERROR for 422 with undefined error code 123" in {

      val error = """{
                    |  "errors": {
                    |    "processingDate": "2025-12-02T13:14:41Z",
                    |    "code": "123",
                    |    "text": "Unknown error code"
                    |  }
                    |}
                    |  """.stripMargin

      when(mockWithdrawalService.withdrawal(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalController.withdrawal(testRefNo).apply(FakeRequest().withJsonBody(api8RequestJson))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result).contains(error) shouldBe true
      contentAsString(result).contains("Unsuccessful return of data.") shouldBe true
    }

  }
}
