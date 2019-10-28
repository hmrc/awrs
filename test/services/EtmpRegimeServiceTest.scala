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

package services

import java.util.UUID

import connectors.EtmpConnector
import org.mockito.Matchers
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.test.Helpers.OK
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{AWRSFeatureSwitches, BaseSpec, FeatureSwitch}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EtmpRegimeServiceTest extends BaseSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

  val mockEtmpConnector: EtmpConnector = mock[EtmpConnector]
  val safeId: String = "XV1234567890123"
  val regime: String = "AWRS"

  object TestEtmpRegimeService extends EtmpRegimeService(mockEtmpConnector)


  "awrsRegime" should {
    "successfully return an empty reference" in {
      when(mockEtmpConnector.awrsRegime(Matchers.any(), Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, None)))
      val result = TestEtmpRegimeService.getRegimeRefNumber(safeId, regime)

      await(result) shouldBe None
    }

    "successfully return a regimeRefNumber" in {
      when(mockEtmpConnector.awrsRegime(Matchers.any(), Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(Json.parse(
          """
            |{"regimeIdentifiers": [
            |{
            |  "regimeName": "String",
            |  "regimeRefNumber": "AWRS"
            |}
            |]}
          """.stripMargin)))))
      val result = TestEtmpRegimeService.getRegimeRefNumber(safeId, regime)

      await(result) shouldBe Some(regime)
    }

    "successfully return an empty ref when only regimeName is present" in {
      when(mockEtmpConnector.awrsRegime(Matchers.any(), Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(Json.parse(
          """
            |{"regimeIdentifiers": [
            |{
            |  "regimeName": "String"
            |}
            |]}
          """.stripMargin)))))
      val result = TestEtmpRegimeService.getRegimeRefNumber(safeId, regime)

      await(result) shouldBe None
    }

    "successfully return an empty ref when regimeName and regimeRefNumber are not present" in {
      when(mockEtmpConnector.awrsRegime(Matchers.any(), Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(Json.parse(
          """
            |{"regimeIdentifiers": [
            |{
            |
            |}
            |]}
          """.stripMargin)))))
      val result = TestEtmpRegimeService.getRegimeRefNumber(safeId, regime)

      await(result) shouldBe None
    }
  }

  "checkETMPApi" should {
    "successfully return a regimeRefNumber when the feature switch is enabled" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

      when(mockEtmpConnector.awrsRegime(Matchers.any(), Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(Json.parse(
          """
            |{"regimeIdentifiers": [
            |{
            |  "regimeName": "String",
            |  "regimeRefNumber": "AWRS"
            |}
            |]}
          """.stripMargin)))))
      val result = TestEtmpRegimeService.checkETMPApi(safeId, regime)

      await(result) shouldBe Some(regime)
    }

    "fail to return a regimeRefNumber when the feature switch is enabled but there are no regimeIdentifiers" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

      when(mockEtmpConnector.awrsRegime(Matchers.any(), Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(Json.parse(
          """
            |{}
          """.stripMargin)))))
      val result = TestEtmpRegimeService.checkETMPApi(safeId, regime)

      await(result) shouldBe None
    }

    "fail to return if the etmp call throws an exception" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

      when(mockEtmpConnector.awrsRegime(Matchers.any(), Matchers.any())(Matchers.any()))
        .thenReturn(Future.failed(new RuntimeException("test")))
      val result = TestEtmpRegimeService.checkETMPApi(safeId, regime)

      await(result) shouldBe None
    }

    "fail to return a regimeRefNumber when the feature switch is disabled" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.regimeCheck())

      val result = TestEtmpRegimeService.checkETMPApi(safeId, regime)

      await(result) shouldBe None
    }
  }
}
