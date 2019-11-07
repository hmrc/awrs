/*
 * Copyright 2019 HM Revenue & Customs
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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import uk.gov.hmrc.play.test.UnitSpec
import utils.Utility._

class UtilitySpec extends WordSpecLike with MockitoSugar with ScalaFutures with BeforeAndAfterAll with UnitSpec {

  "UtilitySpec" should {

    "convert years correctly from string dd/MM/yyyy to yyyy-MM-dd (mdtp to etmp)" in {
      for(i <- 2000 to 3000){
        awrsToEtmpDateFormatter("03/01/" + i ) shouldBe (i + "-01-03" )
      }
    }

    "convert years correctly from yyyy-MM-dd to string dd/MM/yyyy (etmp to mdtp)" in {
      for(i <- 2000 to 3000){
        etmpToAwrsDateFormatter(i + "-01-03") shouldBe ("03/01/" + i)
      }
    }
  }
}
