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

import connectors.HipConnector
import org.mockito.ArgumentMatchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsObject, JsResult, JsValue, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.mvc.Http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.AwrsTestJson.testRefNo
import utils.BaseSpec

import scala.concurrent.{ExecutionContext, Future}

class DeRegistrationServiceTest extends BaseSpec with AnyWordSpecLike{

  val mockHipConnector: HipConnector = mock[HipConnector]
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val config: ServicesConfig = app.injector.instanceOf[ServicesConfig]

  object TestLookupService$ extends LookupService(mockHipConnector)

  val testDeRegistrationService: DeRegistrationService = new DeRegistrationService(mockHipConnector)(ec)

  val groupEndedJson: JsValue = api10RequestJson
  val otherReason: JsValue = api10OtherReasonRequestJson
  val successResponse: JsValue = api10SuccessfulResponseJson

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "Deregistration service: deRegistration" must {

    "successfully process the deregistration request when HIP feature switch is enabled" in {

      val responseJson: JsValue = Json.parse(
        """
          |{
          |  "success": {
          |    "processingDate": "2025-09-11T10:30:00Z"
          |  }
          |}
          |""".stripMargin)

      when(mockHipConnector.deRegister(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(Status.CREATED, responseJson, Map.empty[String, Seq[String]])))

      val result = testDeRegistrationService.deRegistration(testRefNo, groupEndedJson)
      await(result).status shouldBe Status.OK
    }

    "respond with appropriate failure status code for a deregistration request when HIP feature switch is enabled" in {

      val responseJson: JsValue = Json.parse(
        """
          |{
          |  "error": {
          |    "code": "400",
          |    "message": "string",
          |    "logID": "24B56DEABD748EB11C66897AB601D222"
          |  }
          |}
          |""".stripMargin)

      when(mockHipConnector.deRegister(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(Status.SERVICE_UNAVAILABLE, responseJson, Map.empty[String, Seq[String]])))

      val result = testDeRegistrationService.deRegistration(testRefNo, groupEndedJson)
      await(result).status shouldBe Status.SERVICE_UNAVAILABLE
    }

    "return BAD_REQUEST when JSON transformation fails (JsError case) when HIP feature switch is enabled" in {

      val invalidJson: JsValue = Json.parse("""["invalid", "json"]""")

      val result = testDeRegistrationService.deRegistration(testRefNo, invalidJson)
      val response = await(result)

      response.status shouldBe Status.BAD_REQUEST
      response.body should include("JSON transformation failed")
    }
  }

  "Deregistration service: updateRequestForHip" must {
    "remove the acknowledgement reference in updated request" in {
      val result: JsResult[JsObject] = testDeRegistrationService.updateRequestForHip(groupEndedJson)

      val expectedJson: JsValue = Json.parse(
        """
          |{
          | "deregistrationDate": "2012-02-10",
          | "deregistrationReason": "Group ended"
          |}
          |""".stripMargin)

      result.isSuccess shouldBe true
      result.get shouldBe expectedJson
    }

    "deregReasonOther should have text if 'Others' is selected in deregistrationReason" in {
      val result: JsResult[JsObject] = testDeRegistrationService.updateRequestForHip(otherReason)
      result.isSuccess shouldBe true
      (result.get \ "deregistrationReason").as[String] shouldBe "Others"
      (result.get \ "deregReasonOther").as[String] shouldBe "other reason"
    }
  }
}
