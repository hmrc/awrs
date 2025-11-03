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

package utils

import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsValue, Json}
import utils.Utility._

class UtilitySpec extends MockitoSugar with ScalaFutures with AnyWordSpecLike {

  "UtilitySpec" must {

    "convert years correctly from string dd/MM/yyyy to yyyy-MM-dd (mdtp to etmp)" in {
      for (i <- 2000 to 3000) {
        awrsToEtmpDateFormatter(s"03/01/$i") shouldBe (s"$i-01-03")
      }
    }

    "convert years correctly from yyyy-MM-dd to string dd/MM/yyyy (etmp to mdtp)" in {
      for (i <- 2000 to 3000) {
        etmpToAwrsDateFormatter(s"$i-01-03") shouldBe (s"03/01/$i")
      }
    }

    "extract JsObject successfully for a given successful HIP response payload" in {
      val hipResponsePayload: JsValue = Json.parse(
        """
          |{
          |  "success": {
          |    "processingDateTime": "2025-09-11T10:30:00Z"
          |  }
          |}
          |""".stripMargin)

      val expectedUpdatedJson: JsValue = Json.parse(
        """
          |{
          | "processingDateTime": "2025-09-11T10:30:00Z"
          |}
          |""".stripMargin)

      stripSuccessNode(hipResponsePayload) shouldBe expectedUpdatedJson
    }

    "Return the original json when 'success' node is missing in response" in {
      val hipResponsePayload: JsValue = Json.parse(
        """
          |{
          |  "notAsuccessKey": {
          |    "someOtherTestKey": "testValue"
          |  }
          |}
          |""".stripMargin)

      stripSuccessNode(hipResponsePayload) shouldBe hipResponsePayload
    }
  }
}

