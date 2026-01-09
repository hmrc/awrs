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
import models._
import org.mockito.ArgumentMatchers.any
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.ControllerComponents
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import services.{LookupService, EtmpRegimeService, EtmpStatusService, SubscriptionService}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.AwrsTestJson.testRefNo
import utils.BaseSpec

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionControllerTest extends BaseSpec with AnyWordSpecLike {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  val mockSubcriptionService: SubscriptionService = mock[SubscriptionService]
  val mockLookupService: LookupService = mock[LookupService]
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
    mockLookupService,
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

      "for 422 with HIP code 002, return NOT_FOUND" in {
        val error002 =
          """{
            |  "errors": {
            |    "processingDate": "2025-12-02T13:14:41Z",
            |    "code": "002",
            |    "text": "ID not found"
            |  }
            |}
            |""".stripMargin

        when(mockSubcriptionService.subscribe(any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error002, Map.empty[String, Seq[String]])))

        val result = TestSubscriptionController.subscribe().apply(FakeRequest().withJsonBody(api4FrontendLTDJson))
        status(result) shouldBe NOT_FOUND
      }

      "for 422 with HIP code 007, return BAD_REQUEST" in {
        val error007 =
          """{
            |  "errors": {
            |    "processingDate": "2025-12-02T13:14:41Z",
            |    "code": "007",
            |    "text": "Business Partner already has an active AWRS Subscription"
            |  }
            |}
            |""".stripMargin

        when(mockSubcriptionService.subscribe(any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error007, Map.empty[String, Seq[String]])))

        val result = TestSubscriptionController.subscribe().apply(FakeRequest().withJsonBody(api4FrontendLTDJson))
        status(result) shouldBe BAD_REQUEST
      }

      "for 422 with HIP code 999, return INTERNAL_SERVER_ERROR" in {
        val error999 =
          """{
            |  "errors": {
            |    "processingDate": "2025-12-02T13:14:41Z",
            |    "code": "999",
            |    "text": "Technical Error"
            |  }
            |}
            |""".stripMargin

        when(mockSubcriptionService.subscribe(any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error999, Map.empty[String, Seq[String]])))

        val result = TestSubscriptionController.subscribe().apply(FakeRequest().withJsonBody(api4FrontendLTDJson))
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "lookupApplication" must {

      "lookup submitted application from HODS when passed a valid awrs reference" in {
        when(mockLookupService.lookupApplication(any())(any())).thenReturn(Future.successful(HttpResponse(OK, api4EtmpLTDJson, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.lookupApplication(testRefNo).apply(FakeRequest())
        status(result) shouldBe OK
      }

      "return BAD REQUEST error from HODS when passed an invalid awrs reference" in {
        when(mockLookupService.lookupApplication(any())(any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, lookupFailure, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.lookupApplication("AAW00000123456").apply(FakeRequest())
        status(result) shouldBe BAD_REQUEST
      }

      "return NOT FOUND error from HODS when passed an invalid awrs reference" in {
        when(mockLookupService.lookupApplication(any())(any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, lookupFailure, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.lookupApplication("AAW00000123456").apply(FakeRequest())
        status(result) shouldBe NOT_FOUND
      }

      "return SERVICE UNAVAILABLE error from HODS when passed an invalid awrs reference" in {
        when(mockLookupService.lookupApplication(any())(any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, lookupFailure, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.lookupApplication("AAW00000123456").apply(FakeRequest())
        status(result) shouldBe SERVICE_UNAVAILABLE
      }

      "return INTERNAL SERVER ERROR error from HODS when passed an invalid awrs reference" in {
        when(mockLookupService.lookupApplication(any())(any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, lookupFailure, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.lookupApplication("AAW00000123456").apply(FakeRequest())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "return NOT_FOUND for 422 with HIP code 002" in {
        val error002 =
          """{
            |  "errors": {
            |    "processingDate": "2025-12-02T13:14:41Z",
            |    "code": "002",
            |    "text": "ID not found"
            |  }
            |}
            |""".stripMargin

        when(mockLookupService.lookupApplication(any())(any()))
          .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error002, Map.empty[String, Seq[String]])))

        val result = TestSubscriptionController.lookupApplication(testRefNo).apply(FakeRequest())
        status(result) shouldBe NOT_FOUND
      }

      "return BAD_REQUEST for 422 with HIP code 006" in {
        val error006 =
          """{
            |  "errors": {
            |    "processingDate": "2025-12-02T13:14:41Z",
            |    "code": "006",
            |    "text": "No Records Found"
            |  }
            |}
            |""".stripMargin

        when(mockLookupService.lookupApplication(any())(any()))
          .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error006, Map.empty[String, Seq[String]])))

        val result = TestSubscriptionController.lookupApplication(testRefNo).apply(FakeRequest())
        status(result) shouldBe BAD_REQUEST
      }

      "return INTERNAL_SERVER_ERROR for 422 with HIP code 999" in {
        val error999 =
          """{
            |  "errors": {
            |    "processingDate": "2025-12-02T13:14:41Z",
            |    "code": "999",
            |    "text": "Technical Error"
            |  }
            |}
            |""".stripMargin

        when(mockLookupService.lookupApplication(any())(any()))
          .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error999, Map.empty[String, Seq[String]])))

        val result = TestSubscriptionController.lookupApplication(testRefNo).apply(FakeRequest())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "updateSubscription" must {
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

      "for 422 with HIP code 002, return NOT_FOUND" in {
        val error002 =
          """{
            |  "errors": {
            |    "processingDate": "2025-12-02T13:14:41Z",
            |    "code": "002",
            |    "text": "ID not found"
            |  }
            |}
            |""".stripMargin

        when(mockSubcriptionService.updateSubscription(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error002, Map.empty[String, Seq[String]])))

        val result = TestSubscriptionController.updateSubscription(testRefNo).apply(FakeRequest().withJsonBody(api6FrontendLTDJson))
        status(result) shouldBe NOT_FOUND
      }

      "for 422 with HIP code 008, return BAD_REQUEST" in {
        val error008 =
          """{
            |  "errors": {
            |    "processingDate": "2025-12-02T13:14:41Z",
            |    "code": "008",
            |    "text": "Business Partner does not have an active AWRS Subscription"
            |  }
            |}
            |""".stripMargin

        when(mockSubcriptionService.updateSubscription(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error008, Map.empty[String, Seq[String]])))

        val result = TestSubscriptionController.updateSubscription(testRefNo).apply(FakeRequest().withJsonBody(api6FrontendLTDJson))
        status(result) shouldBe BAD_REQUEST
      }

      "for 422 with HIP code 999, return INTERNAL_SERVER_ERROR" in {
        val error999 =
          """{
            |  "errors": {
            |    "processingDate": "2025-12-02T13:14:41Z",
            |    "code": "999",
            |    "text": "Technical Error"
            |  }
            |}
            |""".stripMargin

        when(mockSubcriptionService.updateSubscription(any(), any())(any()))
          .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error999, Map.empty[String, Seq[String]])))

        val result = TestSubscriptionController.updateSubscription(testRefNo).apply(FakeRequest().withJsonBody(api6FrontendLTDJson))
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

    "checkStatus" must {

      "check submitted application status from HODS when passed a valid awrs reference" in {

        def checkStatus(json: JsValue, expected: FormBundleStatus): Unit = {
          when(mockEtmpStatusService.checkStatus(any())(any(), any())).thenReturn(Future.successful(HttpResponse(OK, json, Map.empty[String, Seq[String]])))
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
        when(mockEtmpStatusService.checkStatus(any())(any(), any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, statusFailure, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.checkStatus("AAW00000123456").apply(FakeRequest())
        status(result) shouldBe BAD_REQUEST
      }

      "return NOT FOUND error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpStatusService.checkStatus(any())(any(), any())).thenReturn(Future.successful(HttpResponse(NOT_FOUND, statusFailure, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.checkStatus("AAW00000123456").apply(FakeRequest())
        status(result) shouldBe NOT_FOUND
      }

      "return SERVICE UNAVAILABLE error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpStatusService.checkStatus(any())(any(), any())).thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE, statusFailure, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.checkStatus("AAW00000123456").apply(FakeRequest())
        status(result) shouldBe SERVICE_UNAVAILABLE
      }

      "return INTERNAL SERVER ERROR error from HODS when passed an invalid awrs reference" in {
        when(mockEtmpStatusService.checkStatus(any())(any(), any())).thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, statusFailure, Map.empty[String, Seq[String]])))
        val result = TestSubscriptionController.checkStatus("AAW00000123456").apply(FakeRequest())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "return NOT_FOUND for 422 with HIP code 002" in {
        val error002 =
          """{
            |  "errors": {
            |    "processingDate": "2025-12-02T13:14:41Z",
            |    "code": "002",
            |    "text": "ID not found"
            |  }
            |}
            |""".stripMargin

        when(mockEtmpStatusService.checkStatus(any())(any(), any()))
          .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error002, Map.empty[String, Seq[String]])))

        val result = TestSubscriptionController.checkStatus(testRefNo).apply(FakeRequest())
        status(result) shouldBe NOT_FOUND
      }

      "return INTERNAL_SERVER_ERROR for 422 with HIP code 999" in {
        val error999 =
          """{
            |  "errors": {
            |    "processingDate": "2025-12-02T13:14:41Z",
            |    "code": "999",
            |    "text": "Technical Error"
            |  }
            |}
            |""".stripMargin

        when(mockEtmpStatusService.checkStatus(any())(any(), any()))
          .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, error999, Map.empty[String, Seq[String]])))

        val result = TestSubscriptionController.checkStatus(testRefNo).apply(FakeRequest())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
