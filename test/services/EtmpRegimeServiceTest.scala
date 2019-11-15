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

package services

import java.util.UUID

import connectors.{EnrolmentStoreConnector, EtmpConnector}
import models.{BCAddress, BusinessCustomerDetails, BusinessRegistrationDetails, EtmpRegistrationDetails}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector}
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.{AWRSFeatureSwitches, AwrsTestJson, BaseSpec, FeatureSwitch}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EtmpRegimeServiceTest extends BaseSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

  val mockEtmpConnector: EtmpConnector = mock[EtmpConnector]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockEnrolmentStore: EnrolmentStoreConnector = mock[EnrolmentStoreConnector]
  val safeId: String = "XE0001234567890"
  val regime: String = "AWRS"

  object TestEtmpRegimeService extends EtmpRegimeService(mockEtmpConnector, mockEnrolmentStore, mockAuthConnector )


  "getEtmpBusinessDetails" should {
    "successfully return a regimeRefNumber" in {
      when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(Json.parse(AwrsTestJson.getRegDetailsOrgWithRegime)))))
      val result = TestEtmpRegimeService.getEtmpBusinessDetails(safeId)

      val registrationDetails = {
        EtmpRegistrationDetails(Some("ACME Trading"), "1234567890", "XE0001234567890", Some(false), "XAAW00000123456", Some("AARN1234567"), None, None)
      }

      await(result) shouldBe Some(registrationDetails)
    }

    "successfully return an empty ref when only regimeName is present" in {
      val json: JsObject = Json.parse(AwrsTestJson.getRegDetailsOrgWithRegime).as[JsObject].-("regimeIdentifiers")

      when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(json))))
      val result = TestEtmpRegimeService.getEtmpBusinessDetails(safeId)

      await(result) shouldBe None
    }
  }

  "checkETMPApi" should {
    "successfully return a regimeRefNumber when the feature switch is enabled" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

      when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(AffinityGroup.Organisation)))
      when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(Json.parse(AwrsTestJson.getRegDetailsOrgWithRegime)))))
      when(mockEnrolmentStore.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT)))

      val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
      val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress , "1234567890",
        "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
      val businessRegistrationDetails = BusinessRegistrationDetails(None, None, None, None, None, None, None, None, None)
      val registrationDetails = EtmpRegistrationDetails(Some("ACME Trading"), "1234567890", "XE0001234567890", Some(false), "XAAW00000123456", Some("AARN1234567"), None, None)
      val result = TestEtmpRegimeService.checkETMPApi(safeId, businessCustomerDetails, businessRegistrationDetails)

      await(result) shouldBe Some(registrationDetails)
    }

    "successfully return a regimeRefNumber when the feature switch is enabled and SOP Business type" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

      when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(AffinityGroup.Organisation)))
      when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(Json.parse(AwrsTestJson.getRegDetailsOrgWithRegime)))))
      when(mockEnrolmentStore.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT)))

      val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
      val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("SOP"), businessAddress , "1234567890",
        "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
      val businessRegistrationDetails = BusinessRegistrationDetails(Some("SOP"), None, None, None, None, None, None, None, None)
      val registrationDetails = EtmpRegistrationDetails(Some("ACME Trading"), "1234567890", "XE0001234567890", Some(false), "XAAW00000123456", Some("AARN1234567"), None, None)
      val result = TestEtmpRegimeService.checkETMPApi(safeId, businessCustomerDetails, businessRegistrationDetails)

      await(result) shouldBe Some(registrationDetails)
    }

    "fail to return a regimeRefNumber when the feature switch is enabled but there are no regimeIdentifiers" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

      when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(Json.parse(
          """
            |{}
          """.stripMargin)))))
      val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
      val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress , "1234567890",
        "XE0001234567890", false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
      val businessRegistrationDetails = BusinessRegistrationDetails(None, None, None, None, None, None, None, None, None)

      val result = TestEtmpRegimeService.checkETMPApi(safeId, businessCustomerDetails, businessRegistrationDetails)

      await(result) shouldBe None
    }

    "fail to return if the etmp call throws an exception" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

      when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("test")))
      val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
      val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress , "1234567890",
        "XE0001234567890", false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
      val businessRegistrationDetails = BusinessRegistrationDetails(None, None, None, None, None, None, None, None, None)
      val result = TestEtmpRegimeService.checkETMPApi(safeId, businessCustomerDetails, businessRegistrationDetails)

      await(result) shouldBe None
    }

    "fail to return a regimeRefNumber when the feature switch is disabled" in {
      FeatureSwitch.disable(AWRSFeatureSwitches.regimeCheck())
      val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
      val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress , "1234567890",
        "XE0001234567890", false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
      val businessRegistrationDetails = BusinessRegistrationDetails(None, None, None, None, None, None, None, None, None)
      val result = TestEtmpRegimeService.checkETMPApi(safeId, businessCustomerDetails, businessRegistrationDetails)

      await(result) shouldBe None
    }

    "fails to return registration details when upserting enrolment fails" in {
      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

      when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(AffinityGroup.Organisation)))
      when(mockEtmpConnector.awrsRegime(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(Json.parse(AwrsTestJson.getRegDetailsOrgWithRegime)))))
      when(mockEnrolmentStore.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("failure to return registration details")))

      val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
      val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress , "1234567890",
        "XE0001234567890", isAGroup = false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
      val businessRegistrationDetails = BusinessRegistrationDetails(None, None, None, None, None, None, None, None, None)
      val registrationDetails = EtmpRegistrationDetails(Some("ACME Trading"), "1234567890", "XE0001234567890", Some(false), "XAAW00000123456", Some("AARN1234567"), None, None)
      val result = TestEtmpRegimeService.checkETMPApi(safeId, businessCustomerDetails, businessRegistrationDetails)

      await(result) shouldBe None
    }
  }

  "handleDuplicateSubscription" should {
    "return ETMP registration details for an organisation" when {
      "the regimeRefNumber in ETMP matches the one in business matching" in {

        when(mockAuthConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(AffinityGroup.Organisation)))
        val etmpRegistrationDetails =
          EtmpRegistrationDetails(
            Some("ACME Trading"), "1234567890", "XE0001234567890", Some(false),"XAAW00000123456", Some("AARN1234567"), None, None)
        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress , "1234567890",
          "XE0001234567890", false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
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
            Some("ACME Trading"), "1", "XE0001234567890", Some(false),"XAAW00000123456", Some("AARN1234567"), None, None)
        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress , "1234567890",
          "XE0001234567890", false, Some("XAAW00000123456"), Some("AARN1234567"), None, None)
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
            Some("ACME Trading"), "1", "XE0001234567890", Some(false),"XAAW00000123456", Some("AARN1234567"), Some("first"), Some("last"))
        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress , "1234567890",
          "XE0001234567890", false, Some("XAAW00000123456"), Some("AARN1234567"), Some("first"), Some("last"))
        val result = TestEtmpRegimeService.handleDuplicateSubscription(etmpRegistrationDetails, businessCustomerDetails)

        await(result) shouldBe None

      }
    }

    "getEtmpRegistrationDetails returns Some EtmpRegistrationDetails" when {
      "passed affinity group individual" in {
        val etmpRegistrationDetails =
          EtmpRegistrationDetails(
            Some("ACME Trading"), "1234567890", "XE0001234567890", Some(false),"XAAW00000123456", Some("AARN1234567"), Some("first"), Some("last"))
        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress , "1234567890",
          "XE0001234567890", false, Some("XAAW00000123456"), Some("AARN1234567"), Some("first"), Some("last"))

        val result = TestEtmpRegimeService.getEtmpRegistrationDetails(Some(AffinityGroup.Individual), businessCustomerDetails, etmpRegistrationDetails)

        await(result) shouldBe Some(etmpRegistrationDetails)
      }

      "passed affinity group organisation" in {
        val etmpRegistrationDetails =
          EtmpRegistrationDetails(
            Some("ACME Trading"), "1234567890", "XE0001234567890", Some(false),"XAAW00000123456", Some("AARN1234567"), Some("first"), Some("last"))
        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress , "1234567890",
          "XE0001234567890", false, Some("XAAW00000123456"), Some("AARN1234567"), Some("first"), Some("last"))

        val result = TestEtmpRegimeService.getEtmpRegistrationDetails(Some(AffinityGroup.Organisation), businessCustomerDetails, etmpRegistrationDetails)

        await(result) shouldBe Some(etmpRegistrationDetails)
      }
    }

    "getEtmpRegistrationDetails return None" when {
      "passed other affinity group" in {
        val etmpRegistrationDetails =
          EtmpRegistrationDetails(
            Some("ACME Trading"), "1234567890", "XE0001234567890", Some(false),"XAAW00000123456", Some("AARN1234567"), Some("first"), Some("last"))
        val businessAddress = BCAddress("1 LS House", "LS Way", Some("LS"), Some("Line 4"), Some("Postcode"), "GB")
        val businessCustomerDetails = BusinessCustomerDetails("ACME Trading", Some("Corporate Body"), businessAddress , "1234567890",
          "XE0001234567890", false, Some("XAAW00000123456"), Some("AARN1234567"), Some("first"), Some("last"))

        val result = TestEtmpRegimeService.getEtmpRegistrationDetails(Some(AffinityGroup.Agent), businessCustomerDetails, etmpRegistrationDetails)

        await(result) shouldBe None
      }
    }
  }

  def makeBusinessCustomerDetails(businessName: String, sapNumber: String, safeId: String, isAGroup: Boolean, regimeRefNumber: String, agentRefNumber: Option[String], firstName: Option[String], lastName: Option[String]) =
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

  def makeEtmpDetails(businessName: String, sapNumber: String, safeId: String, isAGroup: Boolean, regimeRefNumber: String, agentRefNumber: Option[String], firstName: Option[String], lastName: Option[String]) =
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

  "matchIndividual" should {
    "return true" when {
      "all elements match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeId", true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))

        TestEtmpRegimeService.matchIndividual(bcd, ecd) shouldBe true
      }

      "all elements match where case is different" in {
        val bcd = makeBusinessCustomerDetails("BUSINESSNAME", "sapNumber", "SAFEID", true, "regimeRef", Some("AGENTREF"), Some("first"), Some("LAST"))
        val ecd = makeEtmpDetails("businessName", "SAPNUMBER", "safeId", true, "REGIMEREF", Some("agentRef"), Some("FIRST"), Some("last"))

        TestEtmpRegimeService.matchIndividual(bcd, ecd) shouldBe true
      }

      "first name are both None" in {
        val bcd = makeBusinessCustomerDetails("BUSINESSNAME", "sapNumber", "SAFEID", true, "regimeRef", None, None, Some("LAST"))
        val ecd = makeEtmpDetails("businessName", "SAPNUMBER", "safeId", true, "REGIMEREF", None, None, Some("last"))

        TestEtmpRegimeService.matchIndividual(bcd, ecd) shouldBe true
      }

      "last name are both None" in {
        val bcd = makeBusinessCustomerDetails("BUSINESSNAME", "sapNumber", "SAFEID", true, "regimeRef", None, Some("first"), None)
        val ecd = makeEtmpDetails("businessName", "SAPNUMBER", "safeId", true, "REGIMEREF", None, Some("first"), None)

        TestEtmpRegimeService.matchIndividual(bcd, ecd) shouldBe true
      }
    }

    "return false" when {
      "sapNumber does not match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumberAltered", "safeId", true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))

        TestEtmpRegimeService.matchIndividual(bcd, ecd) shouldBe false
      }

      "safeId and regimeRef do not match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", true, "regimeRefAlt", Some("agentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeIdAlt", true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))

        TestEtmpRegimeService.matchIndividual(bcd, ecd) shouldBe false
      }

      "first and last names do not match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", true, "regimeRef", Some("agentRef"), Some("first"), Some("lastFirst"))
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeId", true, "regimeRef", Some("agentRef"), Some("firstLast"), Some("last"))

        TestEtmpRegimeService.matchIndividual(bcd, ecd) shouldBe false
      }

      "first and last does not match None" in {
        val bcd = makeBusinessCustomerDetails("BUSINESSNAME", "sapNumber", "SAFEID", true, "regimeRef", None, None, None)
        val ecd = makeEtmpDetails("businessName", "SAPNUMBER", "safeId", true, "REGIMEREF", None, Some("first"), Some("last"))

        TestEtmpRegimeService.matchIndividual(bcd, ecd) shouldBe false
      }
    }
  }

  "matchOrg" should {
    "return true" when {
      "all elements match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeId", true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe true
      }

      "all elements match regardless of case" in {
        val bcd = makeBusinessCustomerDetails("businessName", "SAPNUMBER", "safeId", true, "REGIMEREF", Some("agentRef"), Some("FIRST"), Some("last"))
        val ecd = makeEtmpDetails("BUSINESSNAME", "sapNumber", "SAFEID", true, "regimeRef", Some("AGENTREF"), Some("first"), Some("LAST"))

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe true
      }

      "AgentRef are both None" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", true, "regimeRef", None, Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeId", true, "regimeRef", None, Some("firstLast"), Some("lastFirst"))

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe true
      }

      "first and last name are both None" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", true, "regimeRef", None, None, None)
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeId", true, "regimeRef", None, None, None)

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe true
      }
    }

    "return false" when {
      "sapNumber does not match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumberAltered", "safeId", true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe false
      }

      "safeId and regimeRef do not match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeIdAlt", true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeId", true, "regimeRefAlt", Some("agentRef"), Some("first"), Some("last"))

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe false
      }

      "business name does not match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", true, "regimeRef", Some("agentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessNameDiff", "sapNumber", "safeId", true, "regimeRef", Some("agentRef"), Some("firstLast"), Some("lastFirst"))

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe false
      }

      "AgentRef does not match" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", true, "regimeRef", Some("differentAgentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeId", true, "regimeRef", Some("agentRef"), Some("firstLast"), Some("lastFirst"))

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe false
      }

      "first and last does not match None" in {
        val bcd = makeBusinessCustomerDetails("businessName", "sapNumber", "safeId", true, "regimeRef", Some("differentAgentRef"), Some("first"), Some("last"))
        val ecd = makeEtmpDetails("businessName", "sapNumber", "safeId", true, "regimeRef", Some("agentRef"), None, None)

        TestEtmpRegimeService.matchOrg(bcd, ecd) shouldBe false
      }
    }
  }

  "upsertEacdEnrolment" should {
    "upsert an eacd enrolment" when {
      "provided details to enrol" in {
        when(mockEnrolmentStore.upsertEnrolment(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

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
}
