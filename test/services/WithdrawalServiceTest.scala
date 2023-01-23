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

package services

import connectors.EtmpConnector
import metrics.AwrsMetrics
import org.mockito.ArgumentMatchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId}
import utils.BaseSpec

import java.util.UUID
import scala.concurrent.Future

class WithdrawalServiceTest extends BaseSpec with AnyWordSpecLike {
  val mockEtmpConnector: EtmpConnector = mock[EtmpConnector]

  object TestWithdrawalService extends WithdrawalService(app.injector.instanceOf[AwrsMetrics], mockEtmpConnector)

  "Withdrawal Service" must {
    val awrsRefNo = "XAAW0000010001"
    implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

    "perform a withdrawal when passed valid json" in {
      when(mockEtmpConnector.withdrawal(ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, api8SuccessfulResponseJson, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalService.withdrawal(api8RequestJson, awrsRefNo)
      val response = await(result)
      response.status shouldBe OK
      response.json shouldBe api8SuccessfulResponseJson
    }

    "respond with BadRequest, when withdrawal request fails with a Bad request" in {
      when(mockEtmpConnector.withdrawal(ArgumentMatchers.any(),ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, api8FailureResponseJson, Map.empty[String, Seq[String]])))
      val result = TestWithdrawalService.withdrawal(api8RequestJson, awrsRefNo)
      val response = await(result)
      response.status shouldBe BAD_REQUEST
      response.json shouldBe api8FailureResponseJson
    }

  }

}
