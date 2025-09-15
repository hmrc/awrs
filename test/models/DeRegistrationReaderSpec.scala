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

package models

import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import utils.AwrsTestJson

class DeRegistrationReaderSpec extends PlaySpec with AwrsTestJson with AnyWordSpecLike {

  "DeRegistrationReaderSpec " must {

    "transform DeRegistration correctly to etmp" in {
      val deRegistrationData = DeRegistration("2012-02-10", "Group ended", None)
      val deRegistration = Json.toJson(deRegistrationData)
      val expectedJson = api10RequestJson.toString().replace("$ackRef", deRegistration.\("acknowledgementReference").get.toString().replace("\"", ""))
      deRegistration.toString() shouldBe expectedJson
    }

    "transform DeRegistration (inc other reason) correctly to etmp" in {
      val deRegistrationData = DeRegistration("2012-02-10", "Others", Some("other reason"))
      val deRegistration = Json.toJson(deRegistrationData)
      val expectedJson = api10OtherReasonRequestJson.toString().replace("$ackRef", deRegistration.\("acknowledgementReference").get.toString().replace("\"", ""))
      deRegistration.toString() shouldBe expectedJson
    }

    "transform DeRegistrationResponseType (successful) correctly from etmp" in {
      val deRegistration = api10SuccessfulResponseJson.as[DeRegistrationType](DeRegistrationType.etmpReader)
      deRegistration.response.get shouldBe a[DeRegistrationSuccessResponseType]
    }

    "transform DeRegistrationResponseType (failure) correctly from etmp" in {
      val deRegistration = api10FailureResponseJson.as[DeRegistrationType](DeRegistrationType.etmpReader)
      deRegistration.response.get shouldBe a[DeRegistrationFailureResponseType]
    }

  }

}
