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

import uk.gov.hmrc.play.test.UnitSpec
import utils.AwrsTestJson

class StatusModelReaderSpec extends UnitSpec with AwrsTestJson {

  "StatusModelReaderSpec " should {

    "transform correctly to SubscriptionStatusType Frontend Model " in {
      val subscriptionStatusTypeDetails = api9SuccessfulResponseJson.as[SubscriptionStatusType](SubscriptionStatusType.reader)
      subscriptionStatusTypeDetails shouldBe a[SubscriptionStatusType]
    }

    "transform correctly to SubscriptionStatusType Frontend Model for a de-registration " in {
      val subscriptionStatusTypeDetails = api9SuccessfulDeRegResponseJson.as[SubscriptionStatusType](SubscriptionStatusType.reader)
      subscriptionStatusTypeDetails shouldBe a[SubscriptionStatusType]
    }

  }

}
