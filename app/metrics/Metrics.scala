/*
 * Copyright 2017 HM Revenue & Customs
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

package metrics

import com.codahale.metrics.Timer
import com.codahale.metrics.Timer.Context
import com.kenshoo.play.metrics.MetricsRegistry
import models.ApiType
import models.ApiType.ApiType

trait Metrics {
  def startTimer(api: ApiType): Timer.Context

  def incrementSuccessCounter(api: ApiType.ApiType): Unit

  def incrementFailedCounter(api: ApiType.ApiType): Unit
}

object Metrics extends Metrics {

  val timers = Map(
    ApiType.API4Subscribe -> MetricsRegistry.defaultRegistry.timer("api4-subscribe-response-timer"),
    ApiType.API4AddKnownFacts -> MetricsRegistry.defaultRegistry.timer("api4-add-known-facts-response-timer"),
    ApiType.API5LookupSubscription -> MetricsRegistry.defaultRegistry.timer("api5-lookup-subscription-response-timer"),
    ApiType.API6UpdateSubscription -> MetricsRegistry.defaultRegistry.timer("api5-update-subscription-response-timer"),
    ApiType.API8Withdrawal -> MetricsRegistry.defaultRegistry.timer("api8-withdrawal-response-timer"),
    ApiType.API9UpdateSubscription -> MetricsRegistry.defaultRegistry.timer("api9-subscription-status-response-timer"),
    ApiType.API10DeRegistration -> MetricsRegistry.defaultRegistry.timer("api10-de-registration-response-timer"),
    ApiType.API11GetStatusInfo -> MetricsRegistry.defaultRegistry.timer("api11-get-status-info-response-timer")
  )

  val successCounters = Map(
    ApiType.API4Subscribe -> MetricsRegistry.defaultRegistry.counter("api4-subscribe-success"),
    ApiType.API4AddKnownFacts -> MetricsRegistry.defaultRegistry.counter("api4-add-known-facts-success"),
    ApiType.API5LookupSubscription -> MetricsRegistry.defaultRegistry.counter("api5-lookup-subscription-success"),
    ApiType.API6UpdateSubscription -> MetricsRegistry.defaultRegistry.counter("api5-update-subscription-success"),
    ApiType.API8Withdrawal -> MetricsRegistry.defaultRegistry.counter("api8-withdrawal-response-success"),
    ApiType.API9UpdateSubscription -> MetricsRegistry.defaultRegistry.counter("api9-subscription-status-success"),
    ApiType.API10DeRegistration -> MetricsRegistry.defaultRegistry.counter("api10-de-registration-success"),
    ApiType.API11GetStatusInfo -> MetricsRegistry.defaultRegistry.counter("api11-get-status-info-success")
  )

  val failedCounters = Map(
    ApiType.API4Subscribe -> MetricsRegistry.defaultRegistry.counter("api4-subscribe-failed"),
    ApiType.API4AddKnownFacts -> MetricsRegistry.defaultRegistry.counter("api4-add-known-facts-failed"),
    ApiType.API5LookupSubscription -> MetricsRegistry.defaultRegistry.counter("api5-lookup-subscription-failed"),
    ApiType.API6UpdateSubscription -> MetricsRegistry.defaultRegistry.counter("api5-update-subscription-failed"),
    ApiType.API8Withdrawal -> MetricsRegistry.defaultRegistry.counter("api8-withdrawal-response-failed"),
    ApiType.API9UpdateSubscription -> MetricsRegistry.defaultRegistry.counter("api9-subscription-status-failed"),
    ApiType.API10DeRegistration -> MetricsRegistry.defaultRegistry.counter("api10-de-registration-failed"),
    ApiType.API11GetStatusInfo -> MetricsRegistry.defaultRegistry.counter("api11-get-status-info-failed")
  )

  override def startTimer(api: ApiType): Context = timers(api).time()

  override def incrementSuccessCounter(api: ApiType): Unit = successCounters(api).inc()

  override def incrementFailedCounter(api: ApiType): Unit = failedCounters(api).inc()
}
