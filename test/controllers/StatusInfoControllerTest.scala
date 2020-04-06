/*
 * Copyright 2020 HM Revenue & Customs
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
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.EtmpStatusInfoService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import utils.AwrsTestJson.testRefNo
import utils.BaseSpec

import scala.concurrent.Future


class StatusInfoControllerTest extends BaseSpec {

  val mockEtmpStatusInfoService: EtmpStatusInfoService = mock[EtmpStatusInfoService]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val awrsMetrics: AwrsMetrics = app.injector.instanceOf[AwrsMetrics]
  val cc: ControllerComponents = app.injector.instanceOf[ControllerComponents]

  object TestStatusInfoControllerTest extends StatusInfoController(mockAuditConnector, awrsMetrics, mockEtmpStatusInfoService, cc, "awrs") {
    override val audit: Audit = new TestAudit(mockAuditConnector)
  }

  "For API 11, Status Info Controller " should {

    "check success response is transported correctly" in {
      when(mockEtmpStatusInfoService.getStatusInfo(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(api11SuccessfulCDATAEncodedResponseJson))))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890").apply(FakeRequest())
      status(result) shouldBe OK
      await(result)
      val r = contentAsString(result)
      r shouldBe api11SuccessfulResponseJson.toString()
    }

    "check failure response is transported correctly" in {
      when(mockEtmpStatusInfoService.getStatusInfo(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(api11FailureResponseJson))))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890").apply(FakeRequest())
      status(result) shouldBe OK
      await(result)
      val r = contentAsString(result)
      r shouldBe api11FailureResponseJson.toString()
    }

    "check corrupt etmp response is not passed as OK" in {
      val failureResponse = Json.parse("false")
      when(mockEtmpStatusInfoService.getStatusInfo(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(failureResponse))))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890").apply(FakeRequest())
      status(result) shouldBe INTERNAL_SERVER_ERROR
      await(result)
      val r = contentAsString(result)
      r shouldBe failureResponse.toString()
    }

    "return BAD REQUEST error from HODS when passed an invalid awrs reference" in {
      when(mockEtmpStatusInfoService.getStatusInfo(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(api11FailureResponseJson))))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890").apply(FakeRequest())
      status(result) shouldBe BAD_REQUEST
    }

    "return NOT FOUND error from HODS when passed an invalid awrs reference" in {
      when(mockEtmpStatusInfoService.getStatusInfo(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(api11FailureResponseJson))))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890").apply(FakeRequest())
      status(result) shouldBe NOT_FOUND
    }

    "return SERVICE UNAVAILABLE error from HODS when passed an invalid awrs reference" in {
      when(mockEtmpStatusInfoService.getStatusInfo(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(api11FailureResponseJson))))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890").apply(FakeRequest())
      status(result) shouldBe SERVICE_UNAVAILABLE
    }

    "return INTERNAL SERVER ERROR error from HODS when passed an invalid awrs reference" in {
      when(mockEtmpStatusInfoService.getStatusInfo(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(api11FailureResponseJson))))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890").apply(FakeRequest())
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

  }
}
