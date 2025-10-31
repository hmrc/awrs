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

import java.time.LocalDate
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain._
import utils.TestUtil._

object AwrsTestJson extends AwrsTestJson {
  // We only want one test nino and utr throughout, therefore assign a value in the object declaration
  lazy val testNino: String = new Generator().nextNino.nino
  lazy val testUtr: String = new SaUtrGenerator().nextSaUtr.utr
  lazy val testGrpJoinDate: String = LocalDate.now().toString
  lazy val testRefNo = "DummmyRef"
  lazy val testSafeId = "DummmyRef"
}

trait AwrsTestJson extends AwrsPathConstants {

  // Frontend Json
  lazy val etmpCheckOrganisationString: String = loadWithDummy(etmpCheckOrg)
  lazy val etmpCheckIndividualString: String = loadWithDummy(etmpCheckIndividual)
  lazy val etmpCheckIndividualInvalidString: String = loadWithDummy(etmpCheckIndividualInvalid)
  lazy val etmpCheckOrganisationInvalidString: String = loadWithDummy(etmpCheckOrganisationInvalid)
  lazy val api4FrontendSOPString: String = loadWithDummy(api4FrontendSOP)
  lazy val api4FrontendLTDString: String = loadWithDummy(api4FrontendLTD)
  lazy val api4FrontendPartnershipString: String = loadWithDummy(api4FrontendPartnership)
  lazy val api4FrontendLLPString: String = loadWithDummy(api4FrontendLLP)
  lazy val api4FrontendLPString: String = loadWithDummy(api4FrontendLP)
  lazy val api4FrontendLLPGRPString: String = loadWithDummy(api4FrontendLLPGRP)
  lazy val api4FrontendLTDGRPWithCompaniesString: String = loadWithDummy(api4FrontendLTDGRPWithCompanies)

  lazy val api4FrontendSOPJson: JsValue = loadAndParseJsonWithDummyData(api4FrontendSOP)
  lazy val api4FrontendLTDJson: JsValue = loadAndParseJsonWithDummyData(api4FrontendLTD)
  lazy val api4FrontendLTDNewBusinessJson: JsValue = loadAndParseJsonWithDummyData(api4FrontendLTDNewBusiness)
  lazy val api4FrontendPartnershipJson: JsValue = loadAndParseJsonWithDummyData(api4FrontendPartnership)
  lazy val api4FrontendLLPJson: JsValue = loadAndParseJsonWithDummyData(api4FrontendLLP)
  lazy val api4FrontendLPJson: JsValue = loadAndParseJsonWithDummyData(api4FrontendLP)
  lazy val api4FrontendLLPGRPJson: JsValue = loadAndParseJsonWithDummyData(api4FrontendLLPGRP)
  lazy val api4FrontendLTDGRPWithCompaniesJson: JsValue = loadAndParseJsonWithDummyData(api4FrontendLTDGRPWithCompanies)

  lazy val api5FrontendPartnershipJson: JsValue = loadAndParseJsonWithDummyData(api5FrontendPartnership)
  lazy val api5FrontendLLPJson: JsValue = loadAndParseJsonWithDummyData(api5FrontendLLP)
  lazy val api5FrontendLLPGroupJson: JsValue = loadAndParseJsonWithDummyData(api5FrontendLLPGroup)
  lazy val api5FrontendLTDGroupJson: JsValue = loadAndParseJsonWithDummyData(api5FrontendLTDGroup)

  lazy val api6FrontendLTDJson: JsValue = loadAndParseJsonWithDummyData(api6FrontendLTD)

  lazy val api3FrontendJson: JsValue = loadAndParseJsonWithDummyData(api3Frontend)

  // Etmp Json
  lazy val getRegDetailsOrg: String = loadWithDummy(getRegDetailsOrgJson)
  lazy val getRegDetailsOrgWithRegime: String = loadWithDummy(getRegDetailsOrgWithRegimeJson)
  lazy val getRegDetailsIndividual: String = loadWithDummy(getRegDetailsIndividualJson)
  lazy val getRegDetailsIndividualWithRegime: String = loadWithDummy(getRegDetailsIndividualWithRegimeJson)
  lazy val api4EtmpLTDString: String = loadWithDummy(api4EtmpLTD)
  lazy val api4EtmpSOPString: String = loadWithDummy(api4EtmpSOP)
  lazy val api4EtmpPartnershipString: String = loadWithDummy(api4EtmpPartnership)
  lazy val api4EtmpLLPString: String = loadWithDummy(api4EtmpLLP)
  lazy val api4EtmpLTDGRPString: String = loadWithDummy(api4EtmpLTDGRP)
  lazy val api4EtmpLLPGRPString: String = loadWithDummy(api4EtmpLLPGRP)
  lazy val api4EtmpLTDNewBusinessString: String = loadWithDummy(api4EtmpLTDNewBusiness)

  lazy val api5EtmpLPString: String = loadWithDummy(api5EtmpLP)
  lazy val api5EtmpLLPGroupString: String = loadWithDummy(api5EtmpLLPGroup)
  lazy val api5EtmpLTDGroupString: String = loadWithDummy(api5EtmpLTDGroup)
  lazy val api5EtmpLTDString: String = loadWithDummy(api5EtmpLTD)
  lazy val api5EtmpPartnershipString: String = loadWithDummy(api5EtmpPartnership)
  lazy val api5EtmpSOPString: String = loadWithDummy(api5EtmpSOP)
  lazy val api5EtmpLLPString: String = loadWithDummy(api5EtmpLLP)

  lazy val api4EtmpLTDJson: JsValue = loadAndParseJsonWithDummyData(api4EtmpLTD)
  lazy val api6RequestUpdateJsonWithAck: JsValue = loadAndParseJsonWithDummyData(api6updateJsonWithAck)
  lazy val ackRemovedJson: JsValue = loadAndParseJsonWithDummyData(api6updateJsonWithAckRemoved)
  lazy val api4EtmpPartnershipJson: JsValue = loadAndParseJsonWithDummyData(api4EtmpPartnership)
  lazy val api4EtmpLLPJson: JsValue = loadAndParseJsonWithDummyData(api4EtmpLLP)
  lazy val api4EtmpLTDGRPJson: JsValue = loadAndParseJsonWithDummyData(api4EtmpLTDGRP)
  lazy val api4EtmpLLPGRPJson: JsValue = loadAndParseJsonWithDummyData(api4EtmpLLPGRP)
  lazy val api4EtmpLTDNewBusinessJson: JsValue = loadAndParseJsonWithDummyData(api4EtmpLTDNewBusiness)
  lazy val api4EtmpSOPJson: JsValue = loadAndParseJsonWithDummyData(api4EtmpSOP)

  lazy val api5EtmpLPJson: JsValue = loadAndParseJsonWithDummyData(api5EtmpLP)
  lazy val api5EtmpLTDJson: JsValue = loadAndParseJsonWithDummyData(api5EtmpLTD)
  lazy val api5EtmpPartnershipJson: JsValue = loadAndParseJsonWithDummyData(api5EtmpPartnership)
  lazy val api5EtmpSOPJson: JsValue = loadAndParseJsonWithDummyData(api5EtmpSOP)
  lazy val api5EtmpLLPJson: JsValue = loadAndParseJsonWithDummyData(api5EtmpLLP)

  // Request Json

  lazy val api8RequestJson: JsValue = loadAndParseJsonWithDummyData(api8Request)
  lazy val api8ValidRequestJson: JsValue = loadAndParseJsonWithDummyData(api8ValidRequest)
  lazy val api10RequestJson: JsValue = loadAndParseJsonWithDummyData(api10Request)
  lazy val api10OtherReasonRequestJson: JsValue = loadAndParseJsonWithDummyData(api10OtherReasonRequest)

  // Success Response Json

  lazy val api4SuccessResponse: JsValue = loadAndParseJsonWithDummyData(api4SuccessfulResponse)

  lazy val api6SuccessResponseJson: JsValue = loadAndParseJsonWithDummyData(api6SuccessfulResponse)
  lazy val api6SuccessResponseHipJson: JsValue = loadAndParseJsonWithDummyData(api6SuccessfulResponseHip)

  lazy val api8SuccessfulResponseJson: JsValue = loadAndParseJsonWithDummyData(api8SuccessfulResponse)

  lazy val api9SuccessfulResponseJson: JsValue = loadAndParseJsonWithDummyData(api9SuccessfulResponse)
  lazy val api9SuccessfulResponseUsingCodeJson: JsValue = loadAndParseJsonWithDummyData(api9SuccessfulResponseUsingCode)
  lazy val api9SuccessfulResponseWithMismatchCasesJson: JsValue = loadAndParseJsonWithDummyData(api9SuccessfulResponseWithMismatchCases)
  lazy val api9SuccessfulDeRegResponseJson: JsValue = loadAndParseJsonWithDummyData(api9SuccessfulDeRegResponse)

  lazy val api10SuccessfulResponseJson: JsValue = loadAndParseJsonWithDummyData(api10SuccessfulResponse)

  lazy val api11SuccessfulResponseJson: JsValue = loadAndParseJsonWithDummyData(api11SuccessfulResponse)
  lazy val api11SuccessfulResponseJsonWithNewLine: JsValue = loadAndParseJsonWithDummyData(api11SuccessfulResponseWithNewLine)
  lazy val api11SuccessfulCDATAResponseJson: JsValue = loadAndParseJsonWithDummyData(api11SuccessfulCDATAResponse)
  lazy val api11SuccessfulCDATAEncodedResponseJson: JsValue = loadAndParseJsonWithDummyData(api11SuccessfulCDATAEncodedResponse)
  lazy val api11SuccessfulCDATAEncodedResponseJsonWithNewLine: JsValue = loadAndParseJsonWithDummyData(api11SuccessfulCDATAEncodedResponseWithNewLine)

  // Failure Response Json
  lazy val api6FailureResponseJson: JsValue = loadAndParseJsonWithDummyData(api6FailureResponseHip)
  lazy val api11FailureResponseString: String = loadWithDummy(api11FailureResponse)

  lazy val api8FailureResponseJson: JsValue = loadAndParseJsonWithDummyData(api8FailureResponse)
  lazy val api10FailureResponseJson: JsValue = loadAndParseJsonWithDummyData(api10FailureResponse)
  lazy val api11FailureResponseJson: JsValue = loadAndParseJsonWithDummyData(api11FailureResponse)

  // Common Json

  lazy val commonDirectorsJson: JsValue = loadAndParseJsonWithDummyData(commonDirectors)
  lazy val commonGroupMemberString: String = loadWithDummy(commonGroupMember)
  lazy val commonGroupMembersString: String = loadWithDummy(commonGroupMembers)
  lazy val commonIndPartnershipString: String = loadWithDummy(commonIndPartnership)
  lazy val commonCorpBodyPartnershipString: String = loadWithDummy(commonCorpBodyPartnership)
  lazy val commonSOPPartnershipString: String = loadWithDummy(commonSOPPartnership)
  lazy val commonSOPPartnershipNoRegString: String = loadWithDummy(commonSOPPartnershipNoReg)
  lazy val commonLLPBusDetailsGroupString: String = loadWithDummy(commonLLPBusDetailsGroup)

}
