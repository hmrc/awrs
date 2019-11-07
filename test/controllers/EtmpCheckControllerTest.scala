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

import models.EtmpRegistrationDetails
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.EtmpRegimeService
import utils.{BaseSpec, AwrsTestJson}

import scala.concurrent.Future

class EtmpCheckControllerTest extends BaseSpec with MockitoSugar {

  val mockEtmpRegimeService: EtmpRegimeService = mock[EtmpRegimeService]
  val cc: ControllerComponents = app.injector.instanceOf[ControllerComponents]

  object TestEtmpCheckController extends EtmpCheckController(cc, mockEtmpRegimeService)

  "checkEtmp" should {
    "return an OK" when {
      "there is a regime model and there is ETMP registration details for an organisation" in {
        val etmpRegistrationDetails = EtmpRegistrationDetails(
          None,
          "sapNumber",
          "safeId",
          None,
          "regRef",
          None
        )

          when(mockEtmpRegimeService.checkETMPApi(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(Some(etmpRegistrationDetails)))

        val result = TestEtmpCheckController.checkEtmp().apply(FakeRequest().withJsonBody(Json.parse(etmpCheckOrganisationString)))
        status(result) shouldBe OK
      }

      "there is a regime model and there is ETMP registration details for an individual" in {
        val etmpRegistrationDetails = EtmpRegistrationDetails(
          None,
          "sapNumber",
          "safeId",
          None,
          "regRef",
          None
        )

          when(mockEtmpRegimeService.checkETMPApi(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(Future.successful(Some(etmpRegistrationDetails)))

        val result = TestEtmpCheckController.checkEtmp().apply(FakeRequest().withJsonBody(Json.parse(etmpCheckIndividualString)))
        status(result) shouldBe OK
      }
    }

    "return a NO CONTENT" when {
      "there is a regime model and there is ETMP registration details for an individual with an invalid request body" in {
        val etmpRegistrationDetails = EtmpRegistrationDetails(
          None,
          "sapNumber",
          "safeId",
          None,
          "regRef",
          None
        )

        when(mockEtmpRegimeService.checkETMPApi(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(etmpRegistrationDetails)))

        val result = TestEtmpCheckController.checkEtmp().apply(FakeRequest().withJsonBody(Json.parse(etmpCheckIndividualInvalidString)))
        status(result) shouldBe NO_CONTENT
      }

      "there is a regime model and there is ETMP registration details for an organisation with an invalid request body" in {
        val etmpRegistrationDetails = EtmpRegistrationDetails(
          None,
          "sapNumber",
          "safeId",
          None,
          "regRef",
          None
        )

        when(mockEtmpRegimeService.checkETMPApi(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(etmpRegistrationDetails)))

        val result = TestEtmpCheckController.checkEtmp().apply(FakeRequest().withJsonBody(Json.parse(etmpCheckOrganisationInvalidString)))
        status(result) shouldBe NO_CONTENT
      }

      "there is a regime model and there aren't any ETMP registration details for an organisation" in {
        when(mockEtmpRegimeService.checkETMPApi(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))

        val result = TestEtmpCheckController.checkEtmp().apply(FakeRequest().withJsonBody(Json.parse(etmpCheckOrganisationString)))
        status(result) shouldBe NO_CONTENT
      }
    }
  }

}
