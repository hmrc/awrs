/*
 * Copyright 2016 HM Revenue & Customs
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

package connector

import java.util.UUID
import java.util.concurrent.TimeUnit

import connectors.GovernmentGatewayAdminConnector
import models.{KnownFact, KnownFactsForService}
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.Play
import play.api.http.Status._
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse, InternalServerException}
import uk.gov.hmrc.play.test.UnitSpec
import utils.AwrsTestJson
import utils.AwrsTestJson.testRefNo

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class GGAdminConnectorTest extends UnitSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter with AwrsTestJson {

  object TestAuditConnector extends AuditConnector with AppName with RunMode {
    override lazy val auditingConfig = LoadAuditingConfig("auditing")
  }

  class MockHttp extends WSGet with WSPost with HttpAuditing {
    override val hooks = Seq(AuditingHook)

    override def auditConnector: AuditConnector = TestAuditConnector

    override def appName = Play.configuration.getString("appName").getOrElse("awrs")
  }

  val mockWSHttp = mock[MockHttp]

  object TestGGAdminConnector extends GovernmentGatewayAdminConnector {
    override val http = mockWSHttp
    override val retryWait = 1 // override the retryWait as the wait time is irrelevant to the meaning of the test and reducing it speeds up the tests
  }

  before {
    reset(mockWSHttp)
  }

  "GGAdminConnector" should {
    "for a successful submission, return OK response" in {

      val knownFact = KnownFactsForService(List(KnownFact("AWRS-REF-NO", testRefNo)))
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK, responseJson = None)))
      val result = TestGGAdminConnector.addKnownFacts(knownFact, testRefNo)
      await(result).status shouldBe OK
    }

    "for a successful submission, if the first GG call fails, retry and if 2nd succeeds return OK response" in {

      val knownFact = KnownFactsForService(List(KnownFact("AWRS-REF-NO", testRefNo)))
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, responseJson = None)))
        .thenReturn(Future.successful(HttpResponse(OK, responseJson = None)))
      val result = TestGGAdminConnector.addKnownFacts(knownFact, testRefNo)
      await(result).status shouldBe OK
      // verify that the call is made 2 times, i.e. the first failed call plus 1 successful retry
      verify(mockWSHttp, times(2)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

    "for a successful submission, if the first GG call fails, and all retries fail, return INTERNAL_SERVER_ERROR response" in {

      val knownFact = KnownFactsForService(List(KnownFact("AWRS-REF-NO", testRefNo)))
      implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
      when(mockWSHttp.POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, responseJson = None)))
      val result = TestGGAdminConnector.addKnownFacts(knownFact, testRefNo)
      await(result)(FiniteDuration(10, TimeUnit.SECONDS)).status shouldBe INTERNAL_SERVER_ERROR
      // verify that the call is made 6 times, i.e. the first failed call plus 5 failed retries
      verify(mockWSHttp, times(6)).POST[JsValue, HttpResponse](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any())
    }

  }
}
