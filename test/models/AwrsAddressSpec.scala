/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.PlaySpec

class AwrsAddressSpec extends PlaySpec with AnyWordSpecLike {

  "AwrsAddressSpec" must {
    "output valid toString" in {

      val address = Address(Some("NE1 1AA"), "address line 1", "address line 2", Some("address line 3"),Some("address line 4"), Some("GB"))
      address.toString must include("address line 1, address line 2, address line 3, address line 4, NE1 1AA, ")

    }

  }

}
