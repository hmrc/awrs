/*
 * Copyright 2020 HM Revenue & Customs
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

import uk.gov.hmrc.play.test.UnitSpec

class StripDataTagsSpec extends UnitSpec with AwrsTestJson {

  "isInCDATATag" should {
    "return true" when {
      "the tags are present" in {
        val secureCommentText = (api11SuccessfulCDATAResponseJson \ "secureCommText").as[String]
        StripDataTags.isInCDATATag(secureCommentText) shouldBe true
      }
    }

    "return false" when {
      "the tags are not present" in {
        val secureCommText = (api11SuccessfulResponseJson \ "secureCommText").as[String]
        StripDataTags.isInCDATATag(secureCommText) shouldBe false
      }
    }
  }

  "Strip CData tag" should {
    "remove the data tag" when {
      "the tag is present" in {
        val secureCommText = (api11SuccessfulCDATAResponseJson \ "secureCommText").as[String]
        val expectedSecureCommText = (api11SuccessfulResponseJson \ "secureCommText").as[String]
        StripDataTags.stripCData(secureCommText) shouldBe expectedSecureCommText
      }
    }

    "leave the data unchanged" when {
      "there is no tag present" in {
        val secureCommText = (api11SuccessfulResponseJson \ "secureCommText").as[String]
        StripDataTags.stripCData(secureCommText) shouldBe secureCommText
      }
    }
  }

}
