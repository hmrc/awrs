/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.helpers

import org.apache.pekko.util.Timeout
import org.scalatest._
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.ws.WSRequest
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.helpers.application.IntegrationApplication
import uk.gov.hmrc.helpers.http.StubbedBasicHttpCalls
import uk.gov.hmrc.helpers.wiremock.WireMockSetup
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.Future
import scala.concurrent.duration._

trait IntegrationSpec
  extends AnyWordSpecLike
    with OptionValues
    with FutureAwaits
    with DefaultAwaitTimeout
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with IntegrationApplication
    with WireMockSetup
    with StubbedBasicHttpCalls {

  override implicit def defaultAwaitTimeout: Timeout = 5.seconds

  implicit val hc: HeaderCarrier = HeaderCarrier()

  def client(path: String): WSRequest = ws.url(s"http://localhost:$port$path")
    .withFollowRedirects(false)

  def authorisedClient(path: String): WSRequest = {
    val sessionId = "X-Session-ID" -> "testSessionId"
    val authorisation = "Authorization" -> "Bearer 123"
    val headers = List(sessionId, authorisation)
    client(path)
      .withHttpHeaders(headers:_*)
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWmServer()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    resetWmServer()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    stopWmServer()
  }

  def awaitAndAssert[T](methodUnderTest: => Future[T])(assertions: T => Assertion): Assertion = {
    assertions(await(methodUnderTest))
  }
}



