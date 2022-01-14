/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.WithdrawalService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import utils.AwrsTestJson.testRefNo
import utils.BaseSpec

import scala.concurrent.Future

class WithdrawalControllerTest extends BaseSpec with AnyWordSpecLike {
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

  }
}
