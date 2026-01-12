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
import services.DeRegistrationService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import utils.AwrsTestJson.testRefNo
import utils.BaseSpec

import scala.concurrent.{ExecutionContext, Future}


class DeRegistrationControllerTest extends BaseSpec with AnyWordSpecLike {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val mockDeRegistrationService: DeRegistrationService = mock[DeRegistrationService]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val awrsMetrics: AwrsMetrics = app.injector.instanceOf[AwrsMetrics]
  val cc: ControllerComponents = app.injector.instanceOf[ControllerComponents]

  object TestDeRegistrationControllerTest extends DeRegistrationController(mockAuditConnector, awrsMetrics, mockDeRegistrationService, cc, "awrs") {
    override val audit: Audit = new TestAudit(mockAuditConnector)
  }

  "For API 10, Status Info Controller " must {

    "check success response is transported correctly" in {
      when(mockDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, api10SuccessfulResponseJson, Map.empty[String, Seq[String]])))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo).apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe OK
      await(result)
      val r = contentAsString(result)
      r shouldBe api10SuccessfulResponseJson.toString()
    }

    "check failure response is transported correctly" in {
      when(mockDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, api10FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo).apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe OK
      await(result)
      val r = contentAsString(result)
      r shouldBe api10FailureResponseJson.toString()
    }

    "check corrupt etmp response is not passed as OK" in {
      val failureResponse = Json.parse("false")
      when(mockDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, failureResponse, Map.empty[String, Seq[String]])))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo).apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe INTERNAL_SERVER_ERROR
      await(result)
      val r = contentAsString(result)
      r shouldBe failureResponse.toString()
    }

    "return BAD REQUEST error from HODS when passed an invalid awrs reference" in {
      when(mockDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, api10FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo).apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe BAD_REQUEST
    }

    "return NOT FOUND error from HODS when passed an invalid awrs reference" in {
      when(mockDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, api10FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo).apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe NOT_FOUND
    }

    "return SERVICE UNAVAILABLE error from HODS when passed an invalid awrs reference" in {
      when(mockDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, api10FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo).apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe SERVICE_UNAVAILABLE
    }

    "return INTERNAL SERVER ERROR error from HODS when passed an invalid awrs reference" in {
      when(mockDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, api10FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo).apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return NOT_FOUND for 422 with HIP code 002" in {

      val error002 = """{
                       |  "errors": {
                       |    "processingDate": "2025-12-02T13:14:41Z",
                       |    "code": "002",
                       |    "text": "No records found"
                       |  }
                       |}
                       |  """.stripMargin

      when(mockDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error002, Map.empty[String, Seq[String]])))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo).apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe NOT_FOUND
      contentAsJson(result) shouldBe Json.parse(error002)
    }

    "return BAD_REQUEST for 422 with HIP code 003" in {

      val error003 = """{
                       |  "errors": {
                       |    "processingDate": "2025-12-02T13:14:41Z",
                       |    "code": "003",
                       |    "text": "Invalid deregistration reason"
                       |  }
                       |}
                       |  """.stripMargin

      when(mockDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error003, Map.empty[String, Seq[String]])))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo).apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe Json.parse(error003)
    }

    "return BAD_REQUEST for 422 with HIP code 004" in {

      val error004 = """{
                       |  "errors": {
                       |    "processingDate": "2025-12-02T13:14:41Z",
                       |    "code": "004",
                       |    "text": "Deregistration date is in the future"
                       |  }
                       |}
                       |  """.stripMargin

      when(mockDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error004, Map.empty[String, Seq[String]])))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo).apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe Json.parse(error004)
    }

    "return BAD_REQUEST for 422 with HIP code 005" in {

      val error005 =
        """{
          |  "errors": {
          |    "processingDate": "2025-12-02T13:14:41Z",
          |    "code": "005",
          |    "text": "Deregistration date is before registration date"
          |  }
          |}
          |  """.stripMargin

      when(mockDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error005, Map.empty[String, Seq[String]])))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo).apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe Json.parse(error005)
    }

    "return INTERNAL_SERVER_ERROR for 422 with HIP code 999" in {

      val error999 =
        """{
          |  "errors": {
          |    "processingDate": "2025-12-02T13:14:41Z",
          |    "code": "999",
          |    "text": "Internal server error"
          |  }
          |}
          |  """.stripMargin

      when(mockDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error999, Map.empty[String, Seq[String]])))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo).apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return INTERNAL_SERVER_ERROR for 422 with unknown HIP code" in {

      val errorUnknown =
        """{
          |  "errors": {
          |    "processingDate": "2025-12-02T13:14:41Z",
          |    "code": "123",
          |    "text": "Unknown error"
          |  }
          |}
          |  """.stripMargin

      when(mockDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, errorUnknown, Map.empty[String, Seq[String]])))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo).apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return INTERNAL_SERVER_ERROR for 422 with missing hip code" in {

      val errorMissingCode =
        """{
          |  "errors": {
          |    "processingDate": "2025-12-02T13:14:41Z",
          |    "text": "Missing code"
          |  }
          |}
          |  """.stripMargin

      when(mockDeRegistrationService.deRegistration(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, errorMissingCode, Map.empty[String, Seq[String]])))
      val result = TestDeRegistrationControllerTest.deRegistration(testRefNo).apply(FakeRequest().withJsonBody(api10RequestJson))
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
