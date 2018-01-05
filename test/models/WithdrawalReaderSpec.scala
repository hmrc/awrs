/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class WithdrawalReaderSpec extends UnitSpec {

  "WithdrawalReaderSpec " should {

    "transform correctly to Withdrawal Model " in {
      val inputJsonAPI8 = Json.parse( """{"reason":"Something"}""")
      val withdrawalDetails = inputJsonAPI8.as[WithdrawalRequest](WithdrawalRequest.reader)
      withdrawalDetails shouldBe a[WithdrawalRequest]

      val withdrawalRequest = Json.parse(inputJsonAPI8.toString()).as[WithdrawalRequest]
      val etmpWithdrawalJson = Json.toJson(withdrawalRequest).toString()

      etmpWithdrawalJson should include("acknowledgmentReference")
      etmpWithdrawalJson should include("withdrawalDate")
      etmpWithdrawalJson should include("\"withdrawalReason\":\"Something\"")
      etmpWithdrawalJson shouldNot include("withdrawalReasonOthers")
    }

    "transform correctly to Withdrawal Model when optional other included" in {
      val inputJsonAPI8 = Json.parse( """{"reason":"Something", "reasonOther":"Something else"}""")
      val withdrawalDetails = inputJsonAPI8.as[WithdrawalRequest](WithdrawalRequest.reader)
      withdrawalDetails shouldBe a[WithdrawalRequest]

      val withdrawalRequest = Json.parse(inputJsonAPI8.toString()).as[WithdrawalRequest]
      val etmpWithdrawalJson = Json.toJson(withdrawalRequest).toString()

      etmpWithdrawalJson should include("acknowledgmentReference")
      etmpWithdrawalJson should include("withdrawalDate")
      etmpWithdrawalJson should include("\"withdrawalReason\":\"Something\"")
      etmpWithdrawalJson should include("\"withdrawalReasonOthers\":\"Something else\"")
    }

  }

}
