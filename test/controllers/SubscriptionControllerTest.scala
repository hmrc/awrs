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
import models._
import org.mockito.ArgumentMatchers.any
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.ControllerComponents
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import services.{EtmpLookupService, EtmpRegimeService, EtmpStatusService, SubscriptionService}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.AwrsTestJson.testRefNo
import utils.BaseSpec

import scala.concurrent.Future

class SubscriptionControllerTest extends BaseSpec with AnyWordSpecLike {
  val mockSubcriptionService: SubscriptionService = mock[SubscriptionService]
  val mockEtmpLookupService: EtmpLookupService = mock[EtmpLookupService]
  val mockEtmpStatusService: EtmpStatusService = mock[EtmpStatusService]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockEtmpRegimeService: EtmpRegimeService = mock[EtmpRegimeService]
  val awrsMetrics: AwrsMetrics = app.injector.instanceOf[AwrsMetrics]
  val cc: ControllerComponents = app.injector.instanceOf[ControllerComponents]
  val servicesConfig: ServicesConfig = app.injector.instanceOf[ServicesConfig]

  val utr = "testUtr"
  val lookupSuccess: JsValue = Json.parse( """{"reason": "All ok"}""")
  val lookupFailure: JsValue = Json.parse( """{"reason": "Resource not found"}""")
  val statusFailure: JsValue = Json.parse( """{"reason": "Resource not found"}""")

  val badRequestJson: JsValue = Json.parse("""{"Reason" : "Bad Request"}""")
  val serviceUnavailable: JsValue = Json.parse("""{"Reason" : "Service unavailable"}""")
  val serverError: JsValue = Json.parse("""{"Reason" : "Internal server error"}""")

  object TestSubscriptionController extends SubscriptionController(
    mockAuditConnector,
    awrsMetrics,
    mockSubcriptionService,
    mockEtmpLookupService,
    mockEtmpStatusService,
    mockEtmpRegimeService,
    cc,
    "awrs"
  ) {
    override val appName: String = "awrs"
    override val audit: Audit = new TestAudit(mockAuditConnector)
    override val metrics: AwrsMetrics = awrsMetrics
    override val regimeService: EtmpRegimeService = mockEtmpRegimeService
    override val auditConnector: AuditConnector = mockAuditConnector
  }

  "SubscriptionController" must {

    "subscribe" must {
      val successResponse = Json.parse( s"""{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345","awrsRegistrationNumber": "$testRefNo"}""")
      val registerSuccessResponse = HttpResponse(OK, successResponse, Map.empty[String, Seq[String]])
      val matchFailure = Json.parse( """{"Reason": "Resource not found"}""")
      val matchFailureResponse = HttpResponse(NOT_FOUND, matchFailure, Map.empty[String, Seq[String]])

      "respond with OK" in {
        when(mockSubcriptionService.subscribe(any(), any(), any(), any(), any())(any())).thenReturn(Future.successful(registerSuccessResponse))
        val result = TestSubscriptionController.subscribe().apply(FakeRequest().withJsonBody(api4FrontendLTDJson))
        contentType(result).get shouldBe "text/plain"
        status(result) shouldBe OK
      }

      "return Businessdetails for successful register" in {
        when(mockSubcriptionService.subscribe(any(), any(), any(), any(), any())(any())).thenReturn(Future.successful(registerSuccessResponse))
        val result = TestSubscriptionController.subscribe().apply(FakeRequest().withJsonBody(api4FrontendLTDJson))
        contentAsJson(result) shouldBe successResponse
      }

      "for an unsuccessful match return Not found" in {
        when(mockSubcriptionService.subscribe(any(), any(), any(), any(), any())(any())).thenReturn(Future.successful(matchFailureResponse))
        val result = TestSubscriptionController.subscribe().apply(FakeRequest().withJsonBody(api4FrontendLTDJson))
        status(result) shouldBe NOT_FOUND
        contentAsJson(result) shouldBe matchFailureResponse.json
      }

      "for a bad request, return BadRequest" in {
        when(mockSubcriptionService.subscribe(any(), any(), any(), any(), any())(any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, badRequestJson, Map.empty[String, Seq[String]])))
        when(mockEtmpRegimeService.checkETMPApi(any(), any())(any(), any())).thenReturn(Future.successful(None))
        val result = TestSubscriptionController.subscribe()(FakeRequest().withJsonBody(api4FrontendLTDJson))
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe badRequestJson
      }

      "for a bad request, return ACCEPTED when checkETMPApi returns etmpRegistrationDetails" in {
        val etmpRegistrationDetails =
          EtmpRegistrationDetails(
            Some("ACME Trading"), "1", "XE0001234567890", Some(false),"XAAW00000123456", Some("AARN1234567"), None, None)
        when(mockSubcriptionService.subscribe(any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, badRequestJson, Map.empty[String, Seq[String]])))
        when(mockEtmpRegimeService.checkETMPApi( any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(etmpRegistrationDetails)))
        val result = TestSubscriptionController.subscribe()(FakeRequest().withJsonBody(api4FrontendLTDJson))
        status(result) shouldBe ACCEPTED
      }

      "for service unavailable, return service unavailable" in {
        when(mockSubcriptionService.subscribe(any(), any(), any(), any(), any())(any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, serviceUnavailable, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.subscribe().apply(FakeRequest().withJsonBody(api4FrontendLTDJson))
        status(result) shouldBe SERVICE_UNAVAILABLE
        contentAsJson(result) shouldBe serviceUnavailable
      }

      "internal server error, return internal server error" in {
        when(mockSubcriptionService.subscribe(any(), any(), any(), any(), any())(any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, serverError, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.subscribe().apply(FakeRequest().withJsonBody(api4FrontendLTDJson))
        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe serverError
      }
    }

    "Subscription Controller " must {

      "lookup submitted application from HODS when passed a valid awrs reference" in {
        when(mockEtmpLookupService.lookupApplication(any())(any())).thenReturn(Future.successful(HttpResponse(OK, api4EtmpLTDJson, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.lookupApplication(testRefNo).apply(FakeRequest())
        status(result) shouldBe OK
      }

      "return BAD REQUEST error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpLookupService.lookupApplication(any())(any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, lookupFailure, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.lookupApplication("AAW00000123456").apply(FakeRequest())
        status(result) shouldBe BAD_REQUEST
      }

      "return NOT FOUND error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpLookupService.lookupApplication(any())(any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, lookupFailure, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.lookupApplication("AAW00000123456").apply(FakeRequest())
        status(result) shouldBe NOT_FOUND
      }

      "return SERVICE UNAVAILABLE error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpLookupService.lookupApplication(any())(any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, lookupFailure, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.lookupApplication("AAW00000123456").apply(FakeRequest())
        status(result) shouldBe SERVICE_UNAVAILABLE
      }

      "return INTERNAL SERVER ERROR error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpLookupService.lookupApplication(any())(any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, lookupFailure, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.lookupApplication("AAW00000123456").apply(FakeRequest())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }

    "For API6, Subscription Controller " must {
      val updateSuccessResponse = Json.parse( """{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345"}""")

      "respond with OK" in {
        when(mockSubcriptionService.updateSubscription(any(), any())(any())).thenReturn(Future.successful(HttpResponse(OK, updateSuccessResponse, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.updateSubscription("").apply(FakeRequest().withJsonBody(api6FrontendLTDJson))
        status(result) shouldBe OK
        contentAsJson(result) shouldBe updateSuccessResponse
      }

      "respond with BAD REQUEST" in {
        when(mockSubcriptionService.updateSubscription(any(), any())(any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, badRequestJson, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.updateSubscription("").apply(FakeRequest().withJsonBody(api6FrontendLTDJson))
        status(result) shouldBe BAD_REQUEST
      }

      "return NOT FOUND error from HODS when passed an invalid awrs reference" in {
        when(mockSubcriptionService.updateSubscription(any(), any())(any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, serviceUnavailable, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.updateSubscription("").apply(FakeRequest().withJsonBody(api6FrontendLTDJson))
        status(result) shouldBe NOT_FOUND
      }

      "return SERVICE UNAVAILABLE error from HODS when passed an invalid awrs reference" in {
        when(mockSubcriptionService.updateSubscription(any(), any())(any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, serviceUnavailable, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.updateSubscription("").apply(FakeRequest().withJsonBody(api6FrontendLTDJson))
        status(result) shouldBe SERVICE_UNAVAILABLE
      }

      "return INTERNAL SERVER ERROR error from HODS when passed an invalid awrs reference" in {
        when(mockSubcriptionService.updateSubscription(any(), any())(any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, serverError, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.updateSubscription("").apply(FakeRequest().withJsonBody(api6FrontendLTDJson))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "For API3, Subscription Controller " must {
      val updateSuccessResponse = Json.parse( """{"processingDate":"2015-12-17T09:30:47Z"}""")
      val fakeRequest = FakeRequest(method = "POST", uri = "", headers = FakeHeaders(Seq("Content-type" -> "application/json")), body = api3FrontendJson)
      "respond with OK" in {
        when(mockSubcriptionService.updateGrpRepRegistrationDetails(any(), any())(any())).thenReturn(Future.successful(HttpResponse(OK, updateSuccessResponse, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.updateGrpRegistrationDetails("", "").apply(fakeRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe updateSuccessResponse
      }

      "respond with BAD REQUEST" in {
        when(mockSubcriptionService.updateGrpRepRegistrationDetails(any(), any())(any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, badRequestJson, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.updateGrpRegistrationDetails("", "").apply(fakeRequest)
        status(result) shouldBe BAD_REQUEST
      }

      "return NOT FOUND error from HODS when passed an invalid awrs reference" in {
        when(mockSubcriptionService.updateGrpRepRegistrationDetails(any(), any())(any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, serviceUnavailable, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.updateGrpRegistrationDetails("", "").apply(fakeRequest)
        status(result) shouldBe NOT_FOUND
      }

      "return SERVICE UNAVAILABLE error from HODS when passed an invalid awrs reference" in {
        when(mockSubcriptionService.updateGrpRepRegistrationDetails(any(), any())(any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, serviceUnavailable, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.updateGrpRegistrationDetails("", "").apply(fakeRequest)
        status(result) shouldBe SERVICE_UNAVAILABLE
      }

      "return INTERNAL SERVER ERROR error from HODS when passed an invalid awrs reference" in {
        when(mockSubcriptionService.updateGrpRepRegistrationDetails(any(), any())(any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, serverError, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.updateGrpRegistrationDetails("", "").apply(fakeRequest)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "For API 9, Subscription Controller " must {

      "check submitted application status from HODS when passed a valid awrs reference" in {

        def checkStatus(json: JsValue, expected: FormBundleStatus): Unit = {
          when(mockEtmpStatusService.checkStatus(any())(any())).thenReturn(Future.successful(HttpResponse(OK, json, Map.empty[String, Seq[String]])))
          val result = TestSubscriptionController.checkStatus(testRefNo).apply(FakeRequest())
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
        when(mockEtmpStatusService.checkStatus(any())(any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, statusFailure, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.checkStatus("AAW00000123456").apply(FakeRequest())
        status(result) shouldBe BAD_REQUEST
      }

      "return NOT FOUND error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpStatusService.checkStatus(any())(any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, statusFailure, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.checkStatus("AAW00000123456").apply(FakeRequest())
        status(result) shouldBe NOT_FOUND
      }

      "return SERVICE UNAVAILABLE error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpStatusService.checkStatus(any())(any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, statusFailure, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.checkStatus("AAW00000123456").apply(FakeRequest())
        status(result) shouldBe SERVICE_UNAVAILABLE
      }

      "return INTERNAL SERVER ERROR error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpStatusService.checkStatus(any())(any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, statusFailure, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.checkStatus("AAW00000123456").apply(FakeRequest())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }
  }
}
