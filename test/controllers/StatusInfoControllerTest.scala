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
import models.{AwrsUsers, EtmpRegistrationDetails}
import org.mockito.ArgumentMatchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{EnrolmentService, EtmpRegimeService, EtmpStatusInfoService}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import utils.AwrsTestJson.testRefNo
import utils.BaseSpec

import scala.concurrent.{ExecutionContext, Future}


class StatusInfoControllerTest extends BaseSpec with AnyWordSpecLike {

 implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val mockEtmpStatusInfoService: EtmpStatusInfoService = mock[EtmpStatusInfoService]
  val mockEnrolementService: EnrolmentService = mock[EnrolmentService]
  val mockRegimeService: EtmpRegimeService = mock[EtmpRegimeService]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val awrsMetrics: AwrsMetrics = app.injector.instanceOf[AwrsMetrics]
  val cc: ControllerComponents = app.injector.instanceOf[ControllerComponents]

  object TestStatusInfoControllerTest extends StatusInfoController(mockAuditConnector, awrsMetrics, mockEtmpStatusInfoService, mockRegimeService, mockEnrolementService, cc, "awrs") {
    override val audit: Audit = new TestAudit(mockAuditConnector)
  }

  "For enrolledUsers, Status Info Controller " must {
    "return a OK response containing true if a reference number exists" in {
      val testSafeId = "safeId123"
      val testBusinessDetails = EtmpRegistrationDetails(
        Some("testOrganisation"), "test123", "safe123",
        Some(true), "regime-ref-number-123", Some("agent-ref-number-123"), Some("testFirstName"), Some("testLastName"))
      val awrsUsers = AwrsUsers(List("awrs-user", "principal-user-two"), List("delegated-user-one", "delegated-user-two"))
      when(mockRegimeService.getEtmpBusinessDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(testBusinessDetails)))
      when(mockEnrolementService.awrsUsers(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(Right(awrsUsers)))

      val result = TestStatusInfoControllerTest.enrolledUsers(testSafeId).apply(FakeRequest())
      status(result) shouldBe OK

      contentAsJson(result) shouldBe Json.toJson(Some(awrsUsers))
    }

    "return None if a reference number doesnt exist" in {
      val testSafeId = "safeId123"
      when(mockRegimeService.getEtmpBusinessDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))

      val result = TestStatusInfoControllerTest.enrolledUsers(testSafeId).apply(FakeRequest())
      status(result) shouldBe NOT_FOUND
    }
  }

  "For API 11, Status Info Controller " must {

    "check success response is transported correctly" in {
      when(mockEtmpStatusInfoService.getStatusInfo(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, api11SuccessfulCDATAEncodedResponseJson, Map.empty[String, Seq[String]])))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890").apply(FakeRequest())
      status(result) shouldBe OK
      await(result)
      val r = contentAsString(result)
      r shouldBe api11SuccessfulResponseJson.toString()
    }

    "check failure response is transported correctly" in {
      when(mockEtmpStatusInfoService.getStatusInfo(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, api11FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890").apply(FakeRequest())
      status(result) shouldBe OK
      await(result)
      val r = contentAsString(result)
      r shouldBe api11FailureResponseJson.toString()
    }

    "check corrupt etmp response is not passed as OK" in {
      val failureResponse = Json.parse("false")
      when(mockEtmpStatusInfoService.getStatusInfo(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, failureResponse, Map.empty[String, Seq[String]])))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890").apply(FakeRequest())
      status(result) shouldBe INTERNAL_SERVER_ERROR
      await(result)
      val r = contentAsString(result)
      r shouldBe failureResponse.toString()
    }

    "return BAD REQUEST error from HODS when passed an invalid awrs reference" in {
      when(mockEtmpStatusInfoService.getStatusInfo(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, api11FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890").apply(FakeRequest())
      status(result) shouldBe BAD_REQUEST
    }

    "return NOT FOUND error from HODS when passed an invalid awrs reference" in {
      when(mockEtmpStatusInfoService.getStatusInfo(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, api11FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890").apply(FakeRequest())
      status(result) shouldBe NOT_FOUND
    }

    "return SERVICE UNAVAILABLE error from HODS when passed an invalid awrs reference" in {
      when(mockEtmpStatusInfoService.getStatusInfo(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, api11FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890").apply(FakeRequest())
      status(result) shouldBe SERVICE_UNAVAILABLE
    }

    "return INTERNAL SERVER ERROR error from HODS when passed an invalid awrs reference" in {
      when(mockEtmpStatusInfoService.getStatusInfo(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, api11FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890").apply(FakeRequest())
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

  }
}
