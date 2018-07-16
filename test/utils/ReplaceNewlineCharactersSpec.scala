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

package utils

import uk.gov.hmrc.play.test.UnitSpec

class ReplaceNewlineCharactersSpec extends UnitSpec {

  "ReplaceNewlineCharacters" should {
    "replace new lines with html break" in {
      val newlineText = "<P>Abcdefghijklmnopqrstuvwxyz\n\n0123456789110720182</P>"
      val expectedText = "<P>Abcdefghijklmnopqrstuvwxyz<br><br>0123456789110720182</P>"

      ReplaceNewlineCharacters.replaceNewlineWithHtmlBr(newlineText) shouldBe expectedText
    }

    "strip our carriage returns" in {
      val newlineText = "<P>Abcdefghijklmnopqrstuvwxyz\r0123456789110720182</P>"
      val expectedText = "<P>Abcdefghijklmnopqrstuvwxyz0123456789110720182</P>"

      ReplaceNewlineCharacters.stripOtherCharacters(newlineText) shouldBe expectedText
    }

    "strip out tabulator" in {
      val newlineText = "<P>Abcdefghijklmnopqrstuvwxyz\t0123456789110720182</P>"
      val expectedText = "<P>Abcdefghijklmnopqrstuvwxyz0123456789110720182</P>"

      ReplaceNewlineCharacters.stripOtherCharacters(newlineText) shouldBe expectedText
    }
  }

}
