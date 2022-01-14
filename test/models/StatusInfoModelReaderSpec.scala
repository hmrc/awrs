/*
 * Copyright 2022 HM Revenue & Customs
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
import utils.BaseSpec

class StatusInfoModelReaderSpec extends BaseSpec with AnyWordSpecLike {

  "StatusInfoModelReaderSpec " must {

    "transform correctly to StatusInfoType Frontend Model for success response" in {
      val statusInfoTypeDetails = api11SuccessfulCDATAEncodedResponseJson.as[StatusInfoType](StatusInfoType.reader)
      statusInfoTypeDetails shouldBe a[StatusInfoType]
      statusInfoTypeDetails.response should not be None
      statusInfoTypeDetails.response.get shouldBe a[StatusInfoSuccessResponseType]
    }

    "trims the CDATA tag" in {
      val statusInfoTypeDetails = api11SuccessfulCDATAEncodedResponseJson.as[StatusInfoType](StatusInfoType.reader)
      statusInfoTypeDetails shouldBe a[StatusInfoType]
      statusInfoTypeDetails.response should not be None
      statusInfoTypeDetails.response.get shouldBe a[StatusInfoSuccessResponseType]

      val secureCommentText = statusInfoTypeDetails.response.collect {
        case statusInfoSuccessResponseType: StatusInfoSuccessResponseType =>
          statusInfoSuccessResponseType.secureCommText
      }

      val expectedSecureCommenttext = (api11SuccessfulResponseJson \ "secureCommText").as[String]
      secureCommentText.contains(expectedSecureCommenttext) shouldBe true
    }

    "trims the CDATA tag when there is a new line" in {
      val statusInfoTypeDetails = api11SuccessfulCDATAEncodedResponseJsonWithNewLine.as[StatusInfoType](StatusInfoType.reader)
      statusInfoTypeDetails shouldBe a[StatusInfoType]
      statusInfoTypeDetails.response should not be None
      statusInfoTypeDetails.response.get shouldBe a[StatusInfoSuccessResponseType]

      val secureCommentText = statusInfoTypeDetails.response.collect {
        case statusInfoSuccessResponseType: StatusInfoSuccessResponseType =>
          statusInfoSuccessResponseType.secureCommText
      }

      val expectedSecureCommenttext = (api11SuccessfulResponseJsonWithNewLine \ "secureCommText").as[String]
      secureCommentText.contains(expectedSecureCommenttext) shouldBe true
    }

    "transform correctly to StatusInfoType Frontend Model for failure response " in {
      val statusInfoTypeDetails = api11FailureResponseJson.as[StatusInfoType](StatusInfoType.reader)
      statusInfoTypeDetails shouldBe a[StatusInfoType]
      statusInfoTypeDetails.response should not be None
      statusInfoTypeDetails.response.get shouldBe a[StatusInfoFailureResponseType]
    }

  }

}
