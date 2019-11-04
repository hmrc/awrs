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

package controllers

import audit.TestAudit
import controllers.DeRegistrationController
import metrics.AwrsMetrics
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.EtmpDeRegistrationService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import utils.AwrsTestJson.testRefNo
import utils.BaseSpec

import scala.concurrent.Future


class DeRegistrationControllerTest extends BaseSpec {
  val mockEtmpDeRegistrationService: EtmpDeRegistrationService = mock[EtmpDeRegistrationService]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val awrsMetrics: AwrsMetrics = app.injector.instanceOf[AwrsMetrics]
  val cc: ControllerComponents = app.injector.instanceOf[ControllerComponents]

  object TestDeRegistrationControllerTest extends DeRegistrationController(mockAuditConnector, awrsMetrics, mockEtmpDeRegistrationService, cc, "awrs") {
    override val audit: Audit = new TestAudit(mockAuditConnector)
  }

  "For API 10, Status Info Controller " should {

    "check success response is transported correctly" in {
      when(mockEtmpDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(api10SuccessfulResponseJson))))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo, "ignore", "ignore").apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe OK
      await(result)
      val r = contentAsString(result)
      r shouldBe api10SuccessfulResponseJson.toString()
    }

    "check failure response is transported correctly" in {
      when(mockEtmpDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(api10FailureResponseJson))))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo, "ignore", "ignore").apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe OK
      await(result)
      val r = contentAsString(result)
      r shouldBe api10FailureResponseJson.toString()
    }

    "check corrupt etmp response is not passed as OK" in {
      val failureResponse = Json.parse("false")
      when(mockEtmpDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(failureResponse))))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo, "ignore", "ignore").apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      await(result)
      val r = contentAsString(result)
      r shouldBe failureResponse.toString()
    }

    "return BAD REQUEST error from HODS when passed an invalid awrs reference" in {
      when(mockEtmpDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(api10FailureResponseJson))))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo, "ignore", "ignore").apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe BAD_REQUEST
    }

    "return NOT FOUND error from HODS when passed an invalid awrs reference" in {
      when(mockEtmpDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(api10FailureResponseJson))))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo, "ignore", "ignore").apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe NOT_FOUND
    }

    "return SERVICE UNAVAILABLE error from HODS when passed an invalid awrs reference" in {
      when(mockEtmpDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(api10FailureResponseJson))))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo, "ignore", "ignore").apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe SERVICE_UNAVAILABLE
    }

    "return INTERNAL SERVER ERROR error from HODS when passed an invalid awrs reference" in {
      when(mockEtmpDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(api10FailureResponseJson))))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo, "ignore", "ignore").apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

  }
}
