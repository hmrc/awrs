/*
 * Copyright 2023 HM Revenue & Customs
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

class NewBusinessSpec extends PlaySpec with AnyWordSpecLike {

  "NewBusinessSpec" must {

    val etmpFormatDate = "2016-07-31"
    val feFormatDate = "31/07/2016"

    "parse data from etmp correctly" in {
      val newBusinessString =
        s"""{"newAWBusiness" : true, "proposedStartDate" : "$etmpFormatDate"}"""
      val jsNewBusiness = NewAWBusiness.reader.reads(Json.parse(newBusinessString))
      jsNewBusiness.isSuccess shouldBe true
      val newBusiness = jsNewBusiness.get
      newBusiness.proposedStartDate shouldBe Some(feFormatDate)
    }

    "prase etmp data correctly even when proposed start date is missing" in {
      val newBusinessString =
        """{"newAWBusiness" : true, "proposedStartDate" : ""}"""
      val jsNewBusiness = NewAWBusiness.reader.reads(Json.parse(newBusinessString))
      jsNewBusiness.isSuccess shouldBe true
      val newBusiness = jsNewBusiness.get
      newBusiness.proposedStartDate shouldBe None
    }

  }
}
