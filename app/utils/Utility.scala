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

package utils

import java.time.format.DateTimeFormatter
import java.time.LocalDate

import scala.language.implicitConversions

object Utility {

  def awrsToEtmpDateFormatter(stringDate: String): String = toAndFromDateFormat(stringDate, "yyyy-MM-dd", "dd/MM/yyyy")

  def etmpToAwrsDateFormatter(date: String): String = toAndFromDateFormat(date, "dd/MM/yyyy", "yyyy-MM-dd")

  def formatOptionalDate(optionalDate: Option[String])(f: String => String): Option[String] = optionalDate.map(f)

  def toAndFromDateFormat(date: String, toFormat: String, fromFormat: String): String =
    LocalDate.parse(date, DateTimeFormatter.ofPattern(fromFormat)).format(DateTimeFormatter.ofPattern(toFormat))

  def etmpToAwrsDateFormatterOrNone(date: Option[String]): Option[String] = date.map(etmpToAwrsDateFormatter)

  def booleanToString: Boolean => Some[String] = (s: Boolean) => s match {
    case true => Some("Yes")
    case false => Some("No")
  }

  def trueToNoOrFalseToYes: Boolean => Some[String] = (s: Boolean) => s match {
    case true => Some("No")
    case false => Some("Yes")
  }
}

case class Bool(b: Boolean) {
  def ?[X](t: => X) = new {def | (f: => X) = if(b) t else f
  }
}

object Bool {
  implicit def BooleanBool(b: Boolean): Bool = Bool(b)
}
