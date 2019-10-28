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

package controllers.test

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import utils.{AWRSFeatureSwitches, BooleanFeatureSwitch, FeatureSwitch}

import scala.concurrent.Future

@Singleton
class FeatureSwitchControllerImpl @Inject()(val actorSystem: ActorSystem,
                                            cc: ControllerComponents) extends BackendController(cc) {
  def switch(featureName : String, featureState : String) = Action.async {
    implicit request =>
      def feature = (featureName, featureState) match {
        case (jobName, "enable")  =>
          BooleanFeatureSwitch(featureName, true)
        case (jobName, "disable") =>
          BooleanFeatureSwitch(featureName, false)
      }

      FeatureSwitch.enable(feature)
      Future.successful(Ok(featureName + " - " + feature.enabled))
  }

  def show: Action[AnyContent] = Action.async {
    implicit request =>
      val f = AWRSFeatureSwitches.all.foldLeft("")((s: String, fs: FeatureSwitch) => s + s"""${fs.name} ${fs.enabled}\n""")
      Future.successful(Ok(f))
  }
}
