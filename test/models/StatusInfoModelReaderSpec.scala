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

package models

import uk.gov.hmrc.play.test.UnitSpec
import utils.AwrsTestJson

class StatusInfoModelReaderSpec extends UnitSpec with AwrsTestJson {

  "StatusInfoModelReaderSpec " should {

    "transform correctly to StatusInfoType Frontend Model for success response" in {
      val statusInfoTypeDetails = api11SuccessfulResponseJson.as[StatusInfoType](StatusInfoType.reader)
      statusInfoTypeDetails shouldBe a[StatusInfoType]
      statusInfoTypeDetails.response should not be None
      statusInfoTypeDetails.response.get shouldBe a[StatusInfoSuccessResponseType]
    }

    "trims the CDATA tag" in {
      val statusInfoTypeDetails = api11SuccessfulCDATAResponseJson.as[StatusInfoType](StatusInfoType.reader)
      statusInfoTypeDetails shouldBe a[StatusInfoType]
      statusInfoTypeDetails.response should not be None
      statusInfoTypeDetails.response.get shouldBe a[StatusInfoSuccessResponseType]
      val proccessedStatusInfo = statusInfoTypeDetails.response.get.asInstanceOf[StatusInfoSuccessResponseType]
      StatusInfoSuccessResponseType.isInCDATATag(proccessedStatusInfo.secureCommText) shouldBe false
      val statusInfoTypeNoCDATADetails = api11SuccessfulResponseJson.as[StatusInfoType](StatusInfoType.reader)
      // this final test relies on the content of api11ResponseJson is the same as api11CDATAResponseJson but without the CDATA tag
      val cleanStatusInfo = statusInfoTypeDetails.response.get.asInstanceOf[StatusInfoSuccessResponseType]
      proccessedStatusInfo.secureCommText shouldBe cleanStatusInfo.secureCommText
    }

    "transform correctly to StatusInfoType Frontend Model for failure response " in {
      val statusInfoTypeDetails = api11FailureResponseJson.as[StatusInfoType](StatusInfoType.reader)
      statusInfoTypeDetails shouldBe a[StatusInfoType]
      statusInfoTypeDetails.response should not be None
      statusInfoTypeDetails.response.get shouldBe a[StatusInfoFailureResponseType]
    }

  }

}
