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

package services

import connectors.{EnrolmentStoreConnector, EtmpConnector, HipConnector}
import models._
import org.mockito.ArgumentMatchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionId}
import utils.FeatureSwitch.disable
import utils.{AWRSFeatureSwitches, AwrsTestJson, BaseSpec, FeatureSwitch}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EtmpRegimeServiceTest extends BaseSpec with AnyWordSpecLike with BeforeAndAfterEach with BeforeAndAfterAll {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

  val mockEtmpConnector: EtmpConnector = mock[EtmpConnector]
  val mockHipConnector: HipConnector = mock[HipConnector]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockEnrolmentStore: EnrolmentStoreConnector = mock[EnrolmentStoreConnector]
  val safeId: String = "XE0001234567890"
  val regime: String = "AWRS"

  override def beforeEach(): Unit = {
    disable(AWRSFeatureSwitches.hipSwitch())
  }

  override def afterAll(): Unit = {
    disable(AWRSFeatureSwitches.hipSwitch())
  }

  object TestEtmpRegimeService extends EtmpRegimeService(mockEtmpConnector, mockHipConnector, mockEnrolmentStore, mockAuthConnector)

  "getEtmpBusinessDetails" must {

    "successfully return a regimeRefNumber" in {
      when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Json.parse(AwrsTestJson.getRegDetailsOrgWithRegime), Map.empty[String, Seq[String]])))
      val result = TestEtmpRegimeService.getEtmpBusinessDetails(safeId)

      val registrationDetails = {
        EtmpRegistrationDetails(Some("ACME Trading"), "1234567890", "XE0001234567890", Some(false), "XAAW00000123456", Some("AARN1234567"), None, None)
      }

      await(result) shouldBe Some(registrationDetails)
    }

    "successfully return an empty ref when only regimeName is present" in {
      val json: JsObject = Json.parse(AwrsTestJson.getRegDetailsOrgWithRegime).as[JsObject].-("regimeIdentifiers")

      when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, json, Map.empty[String, Seq[String]])))
      val result = TestEtmpRegimeService.getEtmpBusinessDetails(safeId)

      await(result) shouldBe None
    }
  }

  "checkETMPApi" must {
    "successfully return a regimeRefNumber when the feature switch is enabled" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

      when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(AffinityGroup.Organisation)))
      when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Json.parse(AwrsTestJson.getRegDetailsOrgWithRegime), Map.empty[String, Seq[String]])))
      when(mockEnrolmentStore.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT, " ")))

      val resp = Json.obj(
        "processingDate" -> "20/01/2010",
        "formBundleStatus" -> Approved.name,
        "groupBusinessPartner" -> false
      )

      when(mockEtmpConnector.checkStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, resp, Map.empty[String, Seq[String]])))

      val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
      val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress, "1234567890",
        "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
      val registrationDetails = EtmpRegistrationDetails(Some("ACME Trading"),
        "1234567890", "XE0001234567890", Some(false), "XAAW00000123456", Some("AARN1234567"), None, None)
      val result = TestEtmpRegimeService.checkETMPApi(businessCustomerDetails, "")

      await(result) shouldBe Some(registrationDetails)
    }

    "successfully return a regimeRefNumber when the feature switch is enabled and SOP Business type" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

      when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(AffinityGroup.Organisation)))
      when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Json.parse(AwrsTestJson.getRegDetailsOrgWithRegime), Map.empty[String, Seq[String]])))
      when(mockEnrolmentStore.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT, " ")))
      when(mockEtmpConnector.checkStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))

      val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
      val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("SOP"), businessAddress, "1234567890",
        "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
      val registrationDetails = EtmpRegistrationDetails(Some("ACME Trading"),
        "1234567890", "XE0001234567890", Some(false), "XAAW00000123456", Some("AARN1234567"), None, None)
      val result = TestEtmpRegimeService.checkETMPApi(businessCustomerDetails, "SOP")

      await(result) shouldBe Some(registrationDetails)
    }

    val nonSelfHealStatuses: Seq[String] = List("Rejected", "Revoked", "Withdrawal", "De-Registered")

    for (status <- nonSelfHealStatuses) {

      s"fail when a status of $status is received (no self heal)" in {

        FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

        when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(
          ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(AffinityGroup.Organisation)))
        when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Json.parse(AwrsTestJson.getRegDetailsOrgWithRegime), Map.empty[String, Seq[String]])))
        when(mockEnrolmentStore.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))

        val resp = Json.obj(
          "processingDate" -> "20/01/2010",
          "formBundleStatus" -> status,
          "groupBusinessPartner" -> false
        )

        when(mockEtmpConnector.checkStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, resp, Map.empty[String, Seq[String]])))

        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress, "1234567890",
          "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
        val result = TestEtmpRegimeService.checkETMPApi(businessCustomerDetails, "")

        await(result) shouldBe None
      }
    }

    "fail to return a regimeRefNumber when the feature switch is enabled but there are no regimeIdentifiers" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

      when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Json.parse(
          """
            |{}
          """.stripMargin), Map.empty[String, Seq[String]])))
      val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
      val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress, "1234567890",
        "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)

      val result = TestEtmpRegimeService.checkETMPApi(businessCustomerDetails, "")

      await(result) shouldBe None
    }

    "fail to return a regimeRefNumber when the feature switch is disabled" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.regimeCheck())
      val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
      val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress, "1234567890",
        "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
      val result = TestEtmpRegimeService.checkETMPApi(businessCustomerDetails, "")

      await(result) shouldBe None
    }

    "fails to return registration details when upserting enrolment fails" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

      when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(AffinityGroup.Organisation)))
      when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Json.parse(AwrsTestJson.getRegDetailsOrgWithRegime), Map.empty[String, Seq[String]])))
      when(mockEnrolmentStore.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("failure to return registration details")))

      val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
      val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress, "1234567890",
        "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
      val result = TestEtmpRegimeService.checkETMPApi(businessCustomerDetails, "")

      await(result) shouldBe None
    }
  }


  "handleDuplicateSubscription" must {

    "return ETMP registration details for an organisation" when {

      "the regimeRefNumber in ETMP matches the one in business matching" in {

        when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(AffinityGroup.Organisation)))
        val etmpRegistrationDetails =
          EtmpRegistrationDetails(
            Some("ACME Trading"), "1234567890", "XE0001234567890", Some(false), "XAAW00000123456", Some("AARN1234567"), None, None)
        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress, "1234567890",
          "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
        val result = TestEtmpRegimeService.handleDuplicateSubscription(etmpRegistrationDetails, businessCustomerDetails)

        await(result) shouldBe Some(etmpRegistrationDetails)
      }
    }

    "return none" when {
      "ETMP does not match business details" in {

        when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(AffinityGroup.Organisation)))
        val etmpRegistrationDetails =
          EtmpRegistrationDetails(
            Some("ACME Trading"), "1", "XE0001234567890", Some(false), "XAAW00000123456", Some("AARN1234567"), None, None)
        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress, "1234567890",
          "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
        val result = TestEtmpRegimeService.handleDuplicateSubscription(etmpRegistrationDetails, businessCustomerDetails)

        await(result) shouldBe None
      }
    }
    "return registration details for an individual" when {
      "the atedRefNumber in ETMP matches the one in business matching and admin" in {

        when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(AffinityGroup.Individual)))
        val etmpRegistrationDetails =
          EtmpRegistrationDetails(
            Some("ACME Trading"), "1", "XE0001234567890", Some(false), "XAAW00000123456", Some("AARN1234567"), Some("first"), Some("last"))
        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress, "1234567890",
          "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), Some("first"), Some("last"))
        val result = TestEtmpRegimeService.handleDuplicateSubscription(etmpRegistrationDetails, businessCustomerDetails)

        await(result) shouldBe None

      }
    }

    "getEtmpRegistrationDetails returns Some EtmpRegistrationDetails" when {
      "passed affinity group individual" in {
        val etmpRegistrationDetails =
          EtmpRegistrationDetails(
            Some("ACME Trading"), "1234567890", "XE0001234567890", Some(false), "XAAW00000123456", Some("AARN1234567"), Some("first"), Some("last"))
        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress, "1234567890",
          "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), Some("first"), Some("last"))

        val result = TestEtmpRegimeService.getEtmpRegistrationDetails(Some(AffinityGroup.Individual), businessCustomerDetails, etmpRegistrationDetails)

        result shouldBe Some(etmpRegistrationDetails)
      }

      "passed affinity group organisation" in {
        val etmpRegistrationDetails =
          EtmpRegistrationDetails(
            Some("ACME Trading"), "1234567890", "XE0001234567890", Some(false), "XAAW00000123456", Some("AARN1234567"), Some("first"), Some("last"))
        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress, "1234567890",
          "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), Some("first"), Some("last"))

        val result = TestEtmpRegimeService.getEtmpRegistrationDetails(Some(AffinityGroup.Organisation), businessCustomerDetails, etmpRegistrationDetails)

        result shouldBe Some(etmpRegistrationDetails)
      }
    }

    "getEtmpRegistrationDetails return None" when {
      "passed other affinity group" in {
        val etmpRegistrationDetails =
          EtmpRegistrationDetails(
            Some("ACME Trading"), "1234567890", "XE0001234567890", Some(false), "XAAW00000123456", Some("AARN1234567"), Some("first"), Some("last"))
        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress, "1234567890",
          "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), Some("first"), Some("last"))

        val result = TestEtmpRegimeService.getEtmpRegistrationDetails(Some(AffinityGroup.Agent), businessCustomerDetails, etmpRegistrationDetails)

        result shouldBe None
      }
    }
  }

  def makeBusinessCustomerDetails(businessName: String, sapNumber: String, safeId: String, isAGroup: Boolean,
                                  regimeRefNumber: String, agentRefNumber: Option[String], firstName: Option[String],
                                  lastName: Option[String]): BusinessCustomerDetails =
    BusinessCustomerDetails(
      businessName,
      None,
      BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB"),
      sapNumber,
      safeId,
      isAGroup,
      Some(regimeRefNumber),
      agentRefNumber,
      firstName,
      lastName
    )

  def makeEtmpDetails(businessName: String, sapNumber: String, safeId: String, isAGroup: Boolean,
                      regimeRefNumber: String, agentRefNumber: Option[String], firstName: Option[String],
                      lastName: Option[String]): EtmpRegistrationDetails =
    EtmpRegistrationDetails(
      Some(businessName),
      sapNumber,
      safeId,
      Some(isAGroup),
      regimeRefNumber,
      agentRefNumber,
      firstName,
      lastName
    )

  "matchIndividual" must {
    "return true" when {
      "all elements match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))

        TestEtmpRegimeService.matchIndividual(bcd, ecd) shouldBe true
      }

      "all elements match where case is different" in {
        val bcd = makeBusinessCustomerDetails("BUSINESSNAME", "sapNumber", "SAFEID", isAGroup = true, "regimeRef", Some("AGENTREF"), Some("first"), Some("LAST"))
        val ecd = makeEtmpDetails("businessName", "SAPNUMBER", "safeId", isAGroup = true, "REGIMEREF", Some("agentRef"), Some("FIRST"), Some("last"))

        TestEtmpRegimeService.matchIndividual(bcd, ecd) shouldBe true
      }

      "first name are both None" in {
        val bcd = makeBusinessCustomerDetails("BUSINESSNAME", "sapNumber", "SAFEID", isAGroup = true, "regimeRef", None, None, Some("LAST"))
        val ecd = makeEtmpDetails("businessName", "SAPNUMBER", "safeId", isAGroup = true, "REGIMEREF", None, None, Some("last"))

        TestEtmpRegimeService.matchIndividual(bcd, ecd) shouldBe true
      }

      "last name are both None" in {
        val bcd = makeBusinessCustomerDetails("BUSINESSNAME", "sapNumber", "SAFEID", isAGroup = true, "regimeRef", None, Some("first"), None)
        val ecd = makeEtmpDetails("businessName", "SAPNUMBER", "safeId", isAGroup = true, "REGIMEREF", None, Some("first"), None)

        TestEtmpRegimeService.matchIndividual(bcd, ecd) shouldBe true
      }
    }

    "return false" when {
      "sapNumber does not match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumberAltered", "safeId", isAGroup = true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))

        TestEtmpRegimeService.matchIndividual(bcd, ecd) shouldBe false
      }

      "safeId and regimeRef do not match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRefAlt", Some("agentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeIdAlt", isAGroup = true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))

        TestEtmpRegimeService.matchIndividual(bcd, ecd) shouldBe false
      }

      "first and last names do not match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRef", Some("agentRef"), Some("first"), Some("lastFirst"))
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRef", Some("agentRef"), Some("firstLast"), Some("last"))

        TestEtmpRegimeService.matchIndividual(bcd, ecd) shouldBe false
      }

      "first and last does not match None" in {
        val bcd = makeBusinessCustomerDetails("BUSINESSNAME", "sapNumber", "SAFEID", isAGroup = true, "regimeRef", None, None, None)
        val ecd = makeEtmpDetails("businessName", "SAPNUMBER", "safeId", isAGroup = true, "REGIMEREF", None, Some("first"), Some("last"))

        TestEtmpRegimeService.matchIndividual(bcd, ecd) shouldBe false
      }
    }
  }

  "matchOrg" must {
    "return true" when {
      "all elements match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe true
      }

      "all elements match regardless of case" in {
        val bcd = makeBusinessCustomerDetails("businessName", "SAPNUMBER", "safeId", isAGroup = true, "REGIMEREF", Some("agentRef"), Some("FIRST"), Some("last"))
        val ecd = makeEtmpDetails("BUSINESSNAME", "sapNumber", "SAFEID", isAGroup = true, "regimeRef", Some("AGENTREF"), Some("first"), Some("LAST"))

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe true
      }

      "AgentRef are both None" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRef", None, Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRef", None, Some("firstLast"), Some("lastFirst"))

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe true
      }

      "first and last name are both None" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRef", None, None, None)
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRef", None, None, None)

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe true
      }
    }

    "return false" when {
      "sapNumber does not match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumberAltered", "safeId", isAGroup = true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe false
      }

      "safeId and regimeRef do not match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeIdAlt", isAGroup = true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRefAlt", Some("agentRef"), Some("first"), Some("last"))

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe false
      }

      "business name does not match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessNameDiff", "sapNumber", "safeId", isAGroup = true, "regimeRef", Some("agentRef"), Some("firstLast"), Some("lastFirst"))

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe false
      }

      "AgentRef does not match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRef", Some("differentAgentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRef", Some("agentRef"), Some("firstLast"), Some("lastFirst"))

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe false
      }

      "first and last does not match None" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRef", Some("differentAgentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeId", isAGroup = true, "regimeRef", Some("agentRef"), None, None)

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe false
      }
    }
  }

  "upsertEacdEnrolment" must {
    "upsert an eacd enrolment" when {
      "provided details to enrol" in {
        when(mockEnrolmentStore.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, "", Map.empty[String, Seq[String]])))

        val result = TestEtmpRegimeService.upsertEacdEnrolment("safeId", Some("UTR"), "BusinessType", "postcode", "awrsRefNumber")

        await(result).status shouldBe OK
      }
    }
    "failed to upsert eacd enrolment" when {
      "provided details to enrolment" in {
        when(mockEnrolmentStore.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.failed(new RuntimeException("failed to upsert enrolment")))

        val result = TestEtmpRegimeService.upsertEacdEnrolment("safeId", Some("UTR"), "BusinessType", "postcode", "awrsRefNumber")
        intercept[RuntimeException](await(result))
      }
    }
  }

  "fetchETMPStatus" must {
    "provide the status of the registration if it already exists" when {
      "a rejected case" in {
        val resp = Json.obj(
          "processingDate" -> "20/01/2010",
          "formBundleStatus" -> "Rejected",
          "groupBusinessPartner" -> false
        )

        when(mockEtmpConnector.checkStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, resp, Map.empty[String, Seq[String]])))

        Helpers.await(TestEtmpRegimeService.fetchETMPStatus("testRef")) shouldBe Some(Rejected.name)
      }

    }

    "provide no status" when {
      "the AWRS subscription cannot be found" in {
        when(mockEtmpConnector.checkStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))

        Helpers.await(TestEtmpRegimeService.fetchETMPStatus("testRef")) shouldBe None
      }

      "the status returned from ETMP is not recognised" in {
        val resp = Json.obj(
          "processingDate" -> "20/01/2010",
          "formBundleStatus" -> "Dodgy Status",
          "groupBusinessPartner" -> false
        )

        when(mockEtmpConnector.checkStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, resp, Map.empty[String, Seq[String]])))

        await(TestEtmpRegimeService.fetchETMPStatus("testRef")) shouldBe Some("not found")
      }
    }

    "throw an exception" when {
      "a server error status is received" in {
        when(mockEtmpConnector.checkStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

        intercept[RuntimeException](await(TestEtmpRegimeService.fetchETMPStatus("testRef")))
      }

      "a server error status is received from HIP" in {

        when(mockHipConnector.checkStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

        intercept[RuntimeException](await(TestEtmpRegimeService.fetchETMPStatus("testRef")))
      }

      "a 422 response with code different from '003 - ID not found' is received from HIP" in {

        val response: String =
          """
            {
            |  "errors": {
            |    "processingDate": "2022-01-31T09:26:17Z",
            |    "code": "999",
            |    "text": "Technical error"
            |  }
            |}
            |""".stripMargin

        when(mockHipConnector.checkStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, response)))

        intercept[RuntimeException](await(TestEtmpRegimeService.fetchETMPStatus("testRef")))
      }
    }

    "checkETMPApi against HIP API" must {
      reset(mockEtmpConnector)

      "successfully return a regimeRefNumber when the regime feature switch is enabled" in {

        FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
        FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())

        when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(AffinityGroup.Organisation)))
        when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Json.parse(AwrsTestJson.getRegDetailsOrgWithRegime), Map.empty[String, Seq[String]])))
        when(mockEnrolmentStore.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(NO_CONTENT, " ")))

        val resp = Json.parse(
          """
            |{
            |  "success": {
            |    "processingDate": "2025-09-11T10:30:00Z",
            |    "formBundleStatus": "Pending",
            |    "groupBusinessPartner": false,
            |    "safeid": "XA0000123456789"
            |  }
            |}
            |""".stripMargin)

        when(mockHipConnector.checkStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, resp, Map.empty[String, Seq[String]])))

        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress, "1234567890",
          "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
        val registrationDetails = EtmpRegistrationDetails(Some("ACME Trading"),
          "1234567890", "XE0001234567890", Some(false), "XAAW00000123456", Some("AARN1234567"), None, None)
        val result = TestEtmpRegimeService.checkETMPApi(businessCustomerDetails, "")

        await(result) shouldBe Some(registrationDetails)
      }

      "successfully return a regimeRefNumber when the feature switch is enabled and SOP Business type" in {

        FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())
        FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

        when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(AffinityGroup.Organisation)))
        when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Json.parse(AwrsTestJson.getRegDetailsOrgWithRegime), Map.empty[String, Seq[String]])))
        when(mockEnrolmentStore.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(NO_CONTENT, " ")))
        when(mockHipConnector.checkStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))

        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("SOP"), businessAddress, "1234567890",
          "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
        val registrationDetails = EtmpRegistrationDetails(Some("ACME Trading"),
          "1234567890", "XE0001234567890", Some(false), "XAAW00000123456", Some("AARN1234567"), None, None)
        val result = TestEtmpRegimeService.checkETMPApi(businessCustomerDetails, "SOP")

        await(result) shouldBe Some(registrationDetails)
      }

      "successfully return a regimeRefNumber when the feature switch is enabled and SOP Business type (and ID not found from HIP)" in {

        FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())
        FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

        when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(AffinityGroup.Organisation)))
        when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Json.parse(AwrsTestJson.getRegDetailsOrgWithRegime), Map.empty[String, Seq[String]])))
        when(mockEnrolmentStore.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(NO_CONTENT, " ")))

        val response: String =
          """
            {
            |  "errors": {
            |    "processingDate": "2022-01-31T09:26:17Z",
            |    "code": "002",
            |    "text": "ID not found"
            |  }
            |}
            |""".stripMargin

        when(mockHipConnector.checkStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, response)))

        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("SOP"), businessAddress, "1234567890",
          "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
        val registrationDetails = EtmpRegistrationDetails(Some("ACME Trading"),
          "1234567890", "XE0001234567890", Some(false), "XAAW00000123456", Some("AARN1234567"), None, None)

        val result = TestEtmpRegimeService.checkETMPApi(businessCustomerDetails, "SOP")

        await(result) shouldBe Some(registrationDetails)
      }

      val nonSelfHealStatuses: Seq[String] = List("Rejected", "Revoked", "Withdrawal", "De-Registered")

      for (status <- nonSelfHealStatuses) {

        s"fail when a status of $status is received (no self heal)" in {

          FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())
          FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

          when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(
            ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(AffinityGroup.Organisation)))
          when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, Json.parse(AwrsTestJson.getRegDetailsOrgWithRegime), Map.empty[String, Seq[String]])))
          when(mockEnrolmentStore.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(NO_CONTENT, "")))

          val resp = Json.parse(
            s"""
               |{
               |  "success": {
               |    "processingDate": "2025-09-11T10:30:00Z",
               |    "formBundleStatus": "$status",
               |    "groupBusinessPartner": false
               |  }
               |}
               |""".stripMargin)

          when(mockHipConnector.checkStatus(ArgumentMatchers.any())(ArgumentMatchers.any()))
            .thenReturn(Future.successful(HttpResponse(OK, resp, Map.empty[String, Seq[String]])))

          val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
          val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress, "1234567890",
            "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
          val result = TestEtmpRegimeService.checkETMPApi(businessCustomerDetails, "")

          await(result) shouldBe None

        }

      }

      "fail to return a regimeRefNumber when the feature switch is enabled but there are no regimeIdentifiers" in {

        FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())
        FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

        when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK, Json.parse(
            """
              |{}
          """.stripMargin), Map.empty[String, Seq[String]])))
        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress, "1234567890",
          "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)

        val result = TestEtmpRegimeService.checkETMPApi(businessCustomerDetails, "")

        await(result) shouldBe None
      }

      "fail to return a regimeRefNumber when the feature switch is disabled" in {

        FeatureSwitch.enable(AWRSFeatureSwitches.hipSwitch())
        FeatureSwitch.disable(AWRSFeatureSwitches.regimeCheck())

        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress, "1234567890",
          "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
        val result = TestEtmpRegimeService.checkETMPApi(businessCustomerDetails, "")

        await(result) shouldBe None
      }
    }
  }
}
