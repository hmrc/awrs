/*
 * Copyright 2018 HM Revenue & Customs
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
import models.{DeRegistered, FormBundleStatus, Pending, SubscriptionStatusType}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import services.{EtmpLookupService, EtmpStatusService, SubscriptionService}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.test.UnitSpec
import utils.AwrsTestJson

import scala.concurrent.Future
import utils.AwrsTestJson.testRefNo
import uk.gov.hmrc.http.HttpResponse

class SubscriptionControllerTest extends UnitSpec with OneServerPerSuite with MockitoSugar with AwrsTestJson {
  val mockSubcriptionService: SubscriptionService = mock[SubscriptionService]
  val mockEtmpLookupService: EtmpLookupService = mock[EtmpLookupService]
  val mockEtmpStatusService: EtmpStatusService = mock[EtmpStatusService]
  val utr = "testUtr"
  val lookupSuccess = Json.parse( """{"reason": "All ok"}""")
  val lookupFailure = Json.parse( """{"reason": "Resource not found"}""")
  val statusFailure = Json.parse( """{"reason": "Resource not found"}""")

  val badRequestJson = Json.parse("""{"Reason" : "Bad Request"}""")
  val serviceUnavailable = Json.parse("""{"Reason" : "Service unavailable"}""")
  val serverError = Json.parse("""{"Reason" : "Internal server error"}""")

  object TestSubscriptionController extends SubscriptionController {
    override val appName: String = "awrs"
    val subscriptionService: SubscriptionService = mockSubcriptionService
    val lookupService: EtmpLookupService = mockEtmpLookupService
    val statusService: EtmpStatusService = mockEtmpStatusService
    override val audit: Audit = new TestAudit
    override val metrics = AwrsMetrics
  }

  "SubscriptionController" must {
    "use the correct Subscription Service" in {
      OrgSubscriptionController.subscriptionService shouldBe SubscriptionService
      SaSubscriptionController.subscriptionService shouldBe SubscriptionService
    }

    "subscribe" must {
      val successResponse = Json.parse( s"""{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345","awrsRegistrationNumber": "$testRefNo"}""")
      val registerSuccessResponse = HttpResponse(OK, responseJson = Some(successResponse))
      val matchFailure = Json.parse( """{"Reason": "Resource not found"}""")
      val matchFailureResponse = HttpResponse(NOT_FOUND, responseJson = Some(matchFailure))

      "respond with OK" in {
        when(mockSubcriptionService.subscribe(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(registerSuccessResponse))
        val result = TestSubscriptionController.subscribe("").apply(FakeRequest().withJsonBody(api4FrontendLTDJson))
        status(result) shouldBe OK
      }

      "return text/plain" in {
        when(mockSubcriptionService.subscribe(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(registerSuccessResponse))
        val result = TestSubscriptionController.subscribe("").apply(FakeRequest().withJsonBody(api4FrontendLTDJson))
        contentType(result).get shouldBe "text/plain"
      }

      "return Businessdetails for successful register" in {
        when(mockSubcriptionService.subscribe(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(registerSuccessResponse))
        val result = TestSubscriptionController.subscribe("").apply(FakeRequest().withJsonBody(api4FrontendLTDJson))
        contentAsJson(result) shouldBe successResponse
      }

      "for an unsuccessful match return Not found" in {
        when(mockSubcriptionService.subscribe(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(matchFailureResponse))
        val result = TestSubscriptionController.subscribe("").apply(FakeRequest().withJsonBody(api4FrontendLTDJson))
        status(result) shouldBe NOT_FOUND
        contentAsJson(result) shouldBe matchFailureResponse.json
      }

      "for a bad request, return BadRequest" in {
        when(mockSubcriptionService.subscribe(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(badRequestJson))))
        val result = TestSubscriptionController.subscribe("")(FakeRequest().withJsonBody(api4FrontendLTDJson))
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe badRequestJson
      }

      "for service unavailable, return service unavailable" in {
        when(mockSubcriptionService.subscribe(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(serviceUnavailable))))
        val result = TestSubscriptionController.subscribe("").apply(FakeRequest().withJsonBody(api4FrontendLTDJson))
        status(result) shouldBe SERVICE_UNAVAILABLE
        contentAsJson(result) shouldBe serviceUnavailable
      }

      "internal server error, return internal server error" in {
        when(mockSubcriptionService.subscribe(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(serverError))))
        val result = TestSubscriptionController.subscribe("").apply(FakeRequest().withJsonBody(api4FrontendLTDJson))
        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe serverError
      }
    }

    "Subscription Controller " should {

      "use the correct Lookup service" in {
        SaSubscriptionController.lookupService shouldBe EtmpLookupService
      }

      "lookup submitted application from HODS when passed a valid awrs reference" in {
        when(mockEtmpLookupService.lookupApplication(Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(api4EtmpLTDJson))))
        val result = TestSubscriptionController.lookupApplication("12345", testRefNo).apply(FakeRequest())
        status(result) shouldBe OK
      }

      "return BAD REQUEST error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpLookupService.lookupApplication(Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(lookupFailure))))
        val result = TestSubscriptionController.lookupApplication("12345", "AAW00000123456").apply(FakeRequest())
        status(result) shouldBe BAD_REQUEST
      }

      "return NOT FOUND error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpLookupService.lookupApplication(Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(lookupFailure))))
        val result = TestSubscriptionController.lookupApplication("12345", "AAW00000123456").apply(FakeRequest())
        status(result) shouldBe NOT_FOUND
      }

      "return SERVICE UNAVAILABLE error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpLookupService.lookupApplication(Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(lookupFailure))))
        val result = TestSubscriptionController.lookupApplication("12345", "AAW00000123456").apply(FakeRequest())
        status(result) shouldBe SERVICE_UNAVAILABLE
      }

      "return INTERNAL SERVER ERROR error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpLookupService.lookupApplication(Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(lookupFailure))))
        val result = TestSubscriptionController.lookupApplication("12345", "AAW00000123456").apply(FakeRequest())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }

    "For API6, Subscription Controller " should {
      val updateSuccessResponse = Json.parse( """{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345"}""")

      "respond with OK" in {
        when(mockSubcriptionService.updateSubcription(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(updateSuccessResponse))))
        val result = TestSubscriptionController.updateSubscription("", "").apply(FakeRequest().withJsonBody(api6FrontendLTDJson))
        status(result) shouldBe OK
        contentAsJson(result) shouldBe updateSuccessResponse
      }

      "respond with BAD REQUEST" in {
        when(mockSubcriptionService.updateSubcription(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(badRequestJson))))
        val result = TestSubscriptionController.updateSubscription("", "").apply(FakeRequest().withJsonBody(api6FrontendLTDJson))
        status(result) shouldBe BAD_REQUEST
      }

      "return NOT FOUND error from HODS when passed an invalid awrs reference" in {
        when(mockSubcriptionService.updateSubcription(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(serviceUnavailable))))
        val result = TestSubscriptionController.updateSubscription("", "").apply(FakeRequest().withJsonBody(api6FrontendLTDJson))
        status(result) shouldBe NOT_FOUND
      }

      "return SERVICE UNAVAILABLE error from HODS when passed an invalid awrs reference" in {
        when(mockSubcriptionService.updateSubcription(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(serviceUnavailable))))
        val result = TestSubscriptionController.updateSubscription("", "").apply(FakeRequest().withJsonBody(api6FrontendLTDJson))
        status(result) shouldBe SERVICE_UNAVAILABLE
      }

      "return INTERNAL SERVER ERROR error from HODS when passed an invalid awrs reference" in {
        when(mockSubcriptionService.updateSubcription(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(serverError))))
        val result = TestSubscriptionController.updateSubscription("", "").apply(FakeRequest().withJsonBody(api6FrontendLTDJson))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "For API3, Subscription Controller " should {
      val updateSuccessResponse = Json.parse( """{"processingDate":"2015-12-17T09:30:47Z"}""")
      val fakeRequest = FakeRequest(method = "POST", uri = "", headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = api3FrontendJson)
      "respond with OK" in {
        when(mockSubcriptionService.updateGrpRepRegistrationDetails(Matchers.any(), Matchers.any(),Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(updateSuccessResponse))))
        val result = TestSubscriptionController.updateGrpRegistrationDetails("", "","").apply(fakeRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe updateSuccessResponse
      }

      "respond with BAD REQUEST" in {
        when(mockSubcriptionService.updateGrpRepRegistrationDetails(Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(badRequestJson))))
        val result = TestSubscriptionController.updateGrpRegistrationDetails("", "", "").apply(fakeRequest)
        status(result) shouldBe BAD_REQUEST
      }

      "return NOT FOUND error from HODS when passed an invalid awrs reference" in {
        when(mockSubcriptionService.updateGrpRepRegistrationDetails(Matchers.any(), Matchers.any(),Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(serviceUnavailable))))
        val result = TestSubscriptionController.updateGrpRegistrationDetails("", "", "").apply(fakeRequest)
        status(result) shouldBe NOT_FOUND
      }

      "return SERVICE UNAVAILABLE error from HODS when passed an invalid awrs reference" in {
        when(mockSubcriptionService.updateGrpRepRegistrationDetails(Matchers.any(), Matchers.any(),Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(serviceUnavailable))))
        val result = TestSubscriptionController.updateGrpRegistrationDetails("", "", "").apply(fakeRequest)
        status(result) shouldBe SERVICE_UNAVAILABLE
      }

      "return INTERNAL SERVER ERROR error from HODS when passed an invalid awrs reference" in {
        when(mockSubcriptionService.updateGrpRepRegistrationDetails(Matchers.any(), Matchers.any(),Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(serverError))))
        val result = TestSubscriptionController.updateGrpRegistrationDetails("", "", "").apply(fakeRequest)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "For API 9, Subscription Controller " should {

      "use the correct status service" in {
        SaSubscriptionController.statusService shouldBe EtmpStatusService
      }

      "check submitted application status from HODS when passed a valid awrs reference" in {

        def checkStatus(json: JsValue, expected: FormBundleStatus): Unit = {
          when(mockEtmpStatusService.checkStatus(Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, Some(json))))
          val result = TestSubscriptionController.checkStatus("12345", testRefNo).apply(FakeRequest())
          status(result) shouldBe OK
          await(result)
          val r = contentAsString(result)
          val subscriptionStatusType = SubscriptionStatusType.reader.reads(Json.parse(r))
          subscriptionStatusType.isSuccess shouldBe true
          subscriptionStatusType.get.formBundleStatus shouldBe expected
        }
        checkStatus(api9SuccessfulResponseJson, Pending)
        checkStatus(api9SuccessfulResponseUsingCodeJson, Pending)
        checkStatus(api9SuccessfulResponseWithMismatchCasesJson, Pending)
        checkStatus(api9SuccessfulDeRegResponseJson, DeRegistered)
      }

      "return BAD REQUEST error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpStatusService.checkStatus(Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(statusFailure))))
        val result = TestSubscriptionController.checkStatus("12345", "AAW00000123456").apply(FakeRequest())
        status(result) shouldBe BAD_REQUEST
      }

      "return NOT FOUND error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpStatusService.checkStatus(Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, Some(statusFailure))))
        val result = TestSubscriptionController.checkStatus("12345", "AAW00000123456").apply(FakeRequest())
        status(result) shouldBe NOT_FOUND
      }

      "return SERVICE UNAVAILABLE error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpStatusService.checkStatus(Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, Some(statusFailure))))
        val result = TestSubscriptionController.checkStatus("12345", "AAW00000123456").apply(FakeRequest())
        status(result) shouldBe SERVICE_UNAVAILABLE
      }

      "return INTERNAL SERVER ERROR error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpStatusService.checkStatus(Matchers.any())(Matchers.any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, Some(statusFailure))))
        val result = TestSubscriptionController.checkStatus("12345", "AAW00000123456").apply(FakeRequest())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }
  }
}
