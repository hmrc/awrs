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

package utils

import java.text.SimpleDateFormat
import java.util.Date

object Utility {

  def stringToDate(stringDate: String): String = {
    val ddMMyyyyFormat = new SimpleDateFormat("dd/MM/yyyy")
    val date: Date = ddMMyyyyFormat.parse(stringDate)
    new SimpleDateFormat("yyyy-MM-dd").format(date).toString
  }

  def etmpToMdtpDateFormatter(date: String): String = {
    toAndFromDateFormat(date, "dd/MM/yyyy", "yyyy-MM-dd")
  }

  def etmpToAwrsDateFormatter(date: String): String = {
    toAndFromDateFormat(date, "dd MMMM yyyy", "yyyy-MM-dd")
  }

  def etmpToAwrsOptionalDateFormatter(date: Option[String]) = {
    formatOptionalDate(date)(x => etmpToAwrsDateFormatter(x))
  }

  def formatOptionalDate(optionalDate: Option[String])(f: String => String): Option[String] = {
    optionalDate match {
      case Some(date) => Some(f(date))
      case _ => None
    }
  }

  def toAndFromDateFormat(date: String, toFormat: String, fromFormat: String): String = {
    val fromDate = new SimpleDateFormat(fromFormat)
    val toDate = new SimpleDateFormat(toFormat)
    toDate.format(fromDate.parse(date))
  }

  def etmpToMdtpDateFormatterOrNone(date: Option[String]): Option[String] = {
    date match {
      case Some(date) => Some(etmpToMdtpDateFormatter(date))
      case _ => None
    }
  }

  def booleanToString = (s: Boolean) => s match {
    case true => Some("Yes")
    case false => Some("No")
  }

  def trueToNoOrFalseToYes = (s: Boolean) => s match {
    case true => Some("No")
    case false => Some("Yes")
  }
}

case class Bool(b: Boolean) {
  def ?[X](t: => X) = new {def | (f: => X) = if(b) t else f
  }
}

object Bool {
  implicit def BooleanBool(b: Boolean) = Bool(b)
}
