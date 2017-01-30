/*
 * Copyright 2017 HM Revenue & Customs
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

package Controllers

import audit.TestAudit
import controllers.StatusInfoController
import metrics.AwrsMetrics
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.EtmpStatusInfoService
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import utils.AwrsTestJson

import scala.concurrent.Future
import utils.AwrsTestJson.testRefNo


class StatusInfoControllerTest extends UnitSpec with OneServerPerSuite with MockitoSugar with AwrsTestJson {
  val mockEtmpStatusInfoService: EtmpStatusInfoService = mock[EtmpStatusInfoService]

  object TestStatusInfoControllerTest extends StatusInfoController {
    override val appName: String = "awrs"
    val statusInfoService: EtmpStatusInfoService = mockEtmpStatusInfoService
    override val audit: Audit = new TestAudit
    override val metrics = AwrsMetrics
  }

  "For API 11, Status Info Controller " should {

    "use the correct status info service" in {
      StatusInfoController.statusInfoService shouldBe EtmpStatusInfoService
    }

    "check success response is transported correctly" in {
      when(mockEtmpStatusInfoService.getStatusInfo(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(api11SuccessfulResponseJson))))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890", "ignore", "ignore").apply(FakeRequest())
      status(result) shouldBe OK
      await(result)
      val r = contentAsString(result)
      r shouldBe api11SuccessfulResponseJson.toString()
    }

    "check failure response is transported correctly" in {
      when(mockEtmpStatusInfoService.getStatusInfo(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(api11FailureResponseJson))))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890", "ignore", "ignore").apply(FakeRequest())
      status(result) shouldBe OK
      await(result)
      val r = contentAsString(result)
      r shouldBe api11FailureResponseJson.toString()
    }

    "check corrupt etmp response is not passed as OK" in {
      val failureResponse = Json.parse("false")
      when(mockEtmpStatusInfoService.getStatusInfo(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(failureResponse))))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890", "ignore", "ignore").apply(FakeRequest())
      status(result) shouldBe INTERNAL_SERVER_ERROR
      await(result)
      val r = contentAsString(result)
      r shouldBe failureResponse.toString()
    }

    "return BAD REQUEST error from HODS when passed an invalid awrs reference" in {
      when(mockEtmpStatusInfoService.getStatusInfo(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(api11FailureResponseJson))))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890", "ignore", "ignore").apply(FakeRequest())
      status(result) shouldBe BAD_REQUEST
    }

    "return NOT FOUND error from HODS when passed an invalid awrs reference" in {
      when(mockEtmpStatusInfoService.getStatusInfo(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(api11FailureResponseJson))))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890", "ignore", "ignore").apply(FakeRequest())
      status(result) shouldBe NOT_FOUND
    }

    "return SERVICE UNAVAILABLE error from HODS when passed an invalid awrs reference" in {
      when(mockEtmpStatusInfoService.getStatusInfo(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(api11FailureResponseJson))))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890", "ignore", "ignore").apply(FakeRequest())
      status(result) shouldBe SERVICE_UNAVAILABLE
    }

    "return INTERNAL SERVER ERROR error from HODS when passed an invalid awrs reference" in {
      when(mockEtmpStatusInfoService.getStatusInfo(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(api11FailureResponseJson))))
      val result = TestStatusInfoControllerTest.getStatusInfo(testRefNo, "01234567890", "ignore", "ignore").apply(FakeRequest())
      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

  }
}
