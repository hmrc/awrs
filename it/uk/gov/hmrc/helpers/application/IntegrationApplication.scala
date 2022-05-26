
package uk.gov.hmrc.helpers.application

import org.scalatest.TestSuite
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import uk.gov.hmrc.helpers.wiremock.WireMockConfig

trait IntegrationApplication extends GuiceOneServerPerSuite with WireMockConfig {
  self: TestSuite =>

  val currentAppBaseUrl: String = "awrs"
  val testAppUrl: String        = s"http://localhost:$port/$currentAppBaseUrl"


  def appConfig(extraConfig: (String,String)*): Map[String, Any] = Map(
    "play.http.router"                    -> "testOnlyDoNotUseInAppConf.Routes",
    "microservice.services.auth.host"       -> wireMockHost,
    "microservice.services.auth.port"       -> wireMockPort,
    "auditing.consumer.baseUri.host"        -> wireMockHost,
    "auditing.consumer.baseUri.port"        -> wireMockPort,
    "microservice.services.etmp-hod.host" -> wireMockHost,
    "microservice.services.etmp-hod.port" -> wireMockPort,
    "microservice.services.enrolment-store-proxy.host" -> wireMockHost,
    "microservice.services.enrolment-store-proxy.port" -> wireMockPort

  ) ++ extraConfig

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(appConfig())
    .build()

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]
}
