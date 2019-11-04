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
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.AwrsTestJson.testRefNo
import utils.BaseSpec

import scala.concurrent.Future

class EtmpLookupServiceTest extends BaseSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

  val mockEtmpConnector: EtmpConnector = mock[EtmpConnector]

  object TestEtmpLookupService extends EtmpLookupService(mockEtmpConnector)


  "EtmpLookupService " should {

    "successfully lookup application when passed a valid reference number" in {
      val awrsRefNo = testRefNo
      when(mockEtmpConnector.lookup(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(OK, None)))
      val result = TestEtmpLookupService.lookupApplication(awrsRefNo)
      await(result).status shouldBe 200
    }

    "return Bad Request when passed an invalid reference number" in {
      val invalidAwrsRefNo = "AAW00000123456"
      when(mockEtmpConnector.lookup(ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(HttpResponse(BAD_REQUEST, None)))
      val result = TestEtmpLookupService.lookupApplication(invalidAwrsRefNo)
      await(result).status shouldBe 400
    }
  }
}
