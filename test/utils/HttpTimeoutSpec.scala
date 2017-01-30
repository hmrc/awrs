/*
 * Copyright 2017 HM Revenue & Customs
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

package utils

import java.net.{ServerSocket, URI}
import java.util.concurrent.TimeoutException

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.webbitserver.handler.{DelayedHttpHandler, StringHttpHandler}
import org.webbitserver.netty.NettyWebServer
import play.api.Play
import play.api.test.FakeApplication
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.ws.WSHttp
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.play.audit.http.HttpAuditing

class HttpTimeoutSpec extends WordSpecLike with Matchers with MockitoSugar with ScalaFutures with BeforeAndAfterAll {

  val fakeApplication = FakeApplication(additionalConfiguration = Map("ws.timeout.request" -> "45000"))

  override def beforeAll() {
    super.beforeAll()
    Play.start(fakeApplication)
  }

  override def afterAll() {
    super.afterAll()
    Play.stop(fakeApplication)
  }


  "HttpCalls" should {

    "be gracefully timeout when no response is received within the 'timeout' frame" in {
      val http = new WSHttp with MockAuditing

      // get an unused port
      val ss = new ServerSocket(0)
      ss.close()
      val publicUri = URI.create(s"http://localhost:${ss.getLocalPort}")
      val ws = new NettyWebServer(global, ss.getLocalSocketAddress, publicUri)
      try {
        //starts web server
        ws.add("/test", new DelayedHttpHandler(global, 46000, new StringHttpHandler("application/json", "{name:'pong'}")))
        ws.start().get()

        implicit val hc = HeaderCarrier()

        val start= System.currentTimeMillis()
        intercept[TimeoutException] {
          //make request to web server
          Concurrent.await(http.doPost(s"$publicUri/test", "{name:'ping'}", Seq()))
        }
      } finally {
        ws.stop()
      }

    }


    "worked when the response is received within the 'timeout' frame" in {
      val http = new WSHttp with MockAuditing

      // get an unused port
      val ss = new ServerSocket(0)
      ss.close()
      val publicUri = URI.create(s"http://localhost:${ss.getLocalPort}")
      val ws = new NettyWebServer(global, ss.getLocalSocketAddress, publicUri)
      try {
        //starts web server
        ws.add("/test", new DelayedHttpHandler(global, 44000, new StringHttpHandler("application/json", "{name:'pong'}")))
        ws.start().get()

        implicit val hc = HeaderCarrier()

        val start= System.currentTimeMillis()
        intercept[TimeoutException] {
          //make request to web server
          Concurrent.await(http.doPost(s"$publicUri/test", "{name:'ping'}", Seq()))
        }
      } finally {
        ws.stop()
      }

    }

  }

  object Concurrent {
    import scala.concurrent.{Await, Future}
    import scala.concurrent.duration._

    val defaultTimeout = 5 seconds

    implicit def extractAwait[A](future: Future[A]) = await[A](future)
    implicit def liftFuture[A](v: A) = Future.successful(v)

    def await[A](future: Future[A]) = Await.result(future, defaultTimeout)
  }

  trait MockAuditing  extends HttpAuditing {
    val hooks = Seq(AuditingHook)
    def auditConnector: AuditConnector = mock[AuditConnector]
    def appName: String = "test-app"
  }
}
