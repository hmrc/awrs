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

package services

import connectors.{EtmpConnector, HipConnector}
import org.mockito.ArgumentMatchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsObject, JsResult, JsValue, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.mvc.Http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.AwrsTestJson.testRefNo
import utils.{AWRSFeatureSwitches, BaseSpec, FeatureSwitch}

import scala.concurrent.{ExecutionContext, Future}

class DeRegistrationServiceTest extends BaseSpec with AnyWordSpecLike{

  val mockEtmpConnector: EtmpConnector = mock[EtmpConnector]
  val mockHipConnector: HipConnector = mock[HipConnector]
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  object TestEtmpLookupService extends EtmpLookupService(mockEtmpConnector)

  val testDeRegistrationService: DeRegistrationService = new DeRegistrationService(mockEtmpConnector, mockHipConnector)(ec)

  val groupEndedJson: JsValue = api10RequestJson
  val otherReason: JsValue = api10OtherReasonRequestJson

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "Deregistration service: deRegistration" must {

    "successfully process the deregistration request" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())

      val responseJson: JsValue = Json.parse(
        """
          |{
          |  "success": {
          |    "processingDateTime": "2025-09-11T10:30:00Z"
          |  }
          |}
          |""".stripMargin)

      when(mockHipConnector.deRegister(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(Status.CREATED, responseJson, Map.empty[String, Seq[String]])))

      val result = testDeRegistrationService.deRegistration(testRefNo, groupEndedJson)
      await(result).status shouldBe Status.OK
    }

    "respond the caller with appropriate failure status code for a deregistration request" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())

      val responseJson: JsValue = Json.parse(
        """
          |{
          |  "success": {
          |    "processingDateTime": "2025-09-11T10:30:00Z"
          |  }
          |}
          |""".stripMargin)

      when(mockHipConnector.deRegister(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(Status.SERVICE_UNAVAILABLE, responseJson, Map.empty[String, Seq[String]])))

      val result = testDeRegistrationService.deRegistration(testRefNo, groupEndedJson)
      await(result).status shouldBe Status.SERVICE_UNAVAILABLE
    }

    "throw an exception for an invalid deregistration request" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())

      val inputJson: JsValue = Json.parse(
        """
          {
          |  "acknowledgementReference": "$ackRef",
          |  "deregistrationDate": "2012-02-10",
          |  "deregistrationReason": "Others"
          |}
          |""".stripMargin)

      val exception: RuntimeException = intercept[RuntimeException] {
        testDeRegistrationService.deRegistration(testRefNo, inputJson)
      }
      exception.getMessage shouldBe "'deregReasonOther' is not set when deregistrationReason is set to 'Others'"
    }
  }

  "Deregistration service: updateRequestForHip" must {
    "convert the reason to the correct code" in {
      val result: JsResult[JsObject] = testDeRegistrationService.updateRequestForHip(groupEndedJson)
      result.isSuccess shouldBe true
      (result.get \ "deregistrationReason").as[String] shouldBe "05"
    }

    "convert the other reason to code 99 and include the other reason text" in {
      val result: JsResult[JsObject] = testDeRegistrationService.updateRequestForHip(otherReason)
      result.isSuccess shouldBe true
      (result.get \ "deregistrationReason").as[String] shouldBe "99"
      (result.get \ "deregistrationOther").as[String] shouldBe "other reason"
    }

    "throw an Exception when other reason code is Others and does not include deregReasonOther field" in {

      val inputJson: JsValue = Json.parse(
        """
          {
          |  "acknowledgementReference": "$ackRef",
          |  "deregistrationDate": "2012-02-10",
          |  "deregistrationReason": "Others"
          |}
          |""".stripMargin)

      val exception: RuntimeException = intercept[RuntimeException] {
        testDeRegistrationService.updateRequestForHip(inputJson)
      }
      exception.getMessage shouldBe "'deregReasonOther' is not set when deregistrationReason is set to 'Others'"
    }

    "throw an Exception when other reason code is invalid" in {

      val inputJson: JsValue = Json.parse(
        """
          {
          |  "acknowledgementReference": "$ackRef",
          |  "deregistrationDate": "2012-02-10",
          |  "deregistrationReason": "Invalid"
          |}
          |""".stripMargin)

      val exception: NoSuchElementException = intercept[NoSuchElementException] {
        testDeRegistrationService.updateRequestForHip(inputJson)
      }
      exception.getMessage shouldBe "Invalid deregistration code received"
    }
  }

  "Deregistration service: updateResponseForHip" must {
    "correctly rename the 'processingDateTime' key to 'processingDate' when present" in {

      val inputJson: JsValue = Json.parse(
        """
          |{
          |  "success": {
          |    "processingDateTime": "2025-09-11T10:30:00Z"
          |  }
          |}
          |""".stripMargin)

      val expectedJson: JsValue = Json.parse(
        """
          |{
          | "processingDate": "2025-09-11T10:30:00Z"
          |}
          |""".stripMargin)

      val resultJson: JsValue = testDeRegistrationService.updateResponseForHip(inputJson)
      resultJson shouldBe expectedJson
    }

    "throw an Exception when 'processingDateTime' key is missing from the success node" in {

      val inputJson: JsValue = Json.parse(
        """
          |{
          |  "success": {
          |    "someOtherTestKey": "testValue"
          |  }
          |}
          |""".stripMargin)

      val exception: RuntimeException = intercept[RuntimeException] {
        testDeRegistrationService.updateResponseForHip(inputJson)
      }
      exception.getMessage shouldBe "Received response is missing the 'processingDateTime' key in the 'success' node."
    }
  }
}
