/*
 * Copyright 2025 HM Revenue & Customs
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
import java.time.{ZoneId, ZonedDateTime}
import scala.util.matching.Regex

sealed trait FeatureSwitch {
  def name: String
  def enabled: Boolean
}

trait TimedFeatureSwitch extends FeatureSwitch {

  def start: Option[ZonedDateTime]
  def end: Option[ZonedDateTime]
  def target: ZonedDateTime

  override def enabled: Boolean = (start, end) match {
    case (Some(s), Some(e)) => !target.isBefore(s) && !target.isAfter(e)
    case (None, Some(e)) => !target.isAfter(e)
    case (Some(s), None) => !target.isBefore(s)
    case (None, None) => false
  }
}

case class BooleanFeatureSwitch(name: String, enabled: Boolean) extends FeatureSwitch

case class EnabledTimedFeatureSwitch(name: String, start: Option[ZonedDateTime], end: Option[ZonedDateTime], target: ZonedDateTime) extends TimedFeatureSwitch
case class DisabledTimedFeatureSwitch(name: String, start: Option[ZonedDateTime], end: Option[ZonedDateTime], target: ZonedDateTime) extends TimedFeatureSwitch {
  override def enabled: Boolean = !super.enabled
}

object FeatureSwitch {

  val DisabledIntervalExtractor: Regex = """!(\S+)_(\S+)""".r
  val EnabledIntervalExtractor: Regex = """(\S+)_(\S+)""".r
  val UNSPECIFIED = "X"
  val dateFormat: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME


  private[utils] def getProperty(name: String): FeatureSwitch = {
    val value = sys.props.get(systemPropertyName(name))

    value match {
      case Some("true") => BooleanFeatureSwitch(name, enabled = true)
      case Some(DisabledIntervalExtractor(start, end)) => DisabledTimedFeatureSwitch(name, toDate(start), toDate(end), ZonedDateTime.now(ZoneId.of("UTC")))
      case Some(EnabledIntervalExtractor(start, end)) => EnabledTimedFeatureSwitch(name, toDate(start), toDate(end), ZonedDateTime.now(ZoneId.of("UTC")))

      case _ => BooleanFeatureSwitch(name, enabled = false)
    }
  }

  private[utils] def setProperty(name: String, value: String): FeatureSwitch = {
    sys.props += ((systemPropertyName(name), value))
    getProperty(name)
  }

  private[utils] def toDate(text: String) : Option[ZonedDateTime] = {

    text match {
      case UNSPECIFIED => None
      case _ => Some(ZonedDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }
  }

  private[utils] def systemPropertyName(name: String) = s"feature.$name"

  def enable(fs: FeatureSwitch): FeatureSwitch = setProperty(fs.name, "true")
  def disable(fs: FeatureSwitch): FeatureSwitch = setProperty(fs.name, "false")

  def apply(name: String, enabled: Boolean = false): FeatureSwitch = getProperty(name)
  def unapply(fs: FeatureSwitch): Option[(String, Boolean)] = Some(fs.name -> fs.enabled)
}

object AWRSFeatureSwitches extends AWRSFeatureSwitches

trait AWRSFeatureSwitches {

  def regimeCheck(): FeatureSwitch = FeatureSwitch.getProperty("regimeCheck")
  private def hipEnabled(): FeatureSwitch = FeatureSwitch.getProperty("hipEnabled")

  def apply(name: String): Option[FeatureSwitch] = name match {
    case "regimeCheck" => Some(regimeCheck())
    case "hipEnabled" => Some(hipEnabled())
    case _ => None
  }

  def all: Seq[FeatureSwitch] = {
    Seq.empty
  }
}
