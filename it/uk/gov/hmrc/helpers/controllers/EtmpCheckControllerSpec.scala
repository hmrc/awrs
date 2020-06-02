
package uk.gov.hmrc.helpers.controllers

import controllers.routes
import models.{Pending, Rejected, Revoked}
import org.scalatest.MustMatchers
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.helpers.{AuthHelpers, IntegrationSpec}
import utils.{AWRSFeatureSwitches, FeatureSwitch}

class EtmpCheckControllerSpec extends IntegrationSpec with AuthHelpers with MustMatchers {

  val baseURI = "/alcohol-wholesaler-register"
  val subscriptionURI = "/subscription/"
  val regimeURI = "/registration/details"
  val safeId: String = "XE0001234567890"
  val AWRS_SERVICE_NAME = "HMRC-AWRS-ORG"
  val enrolmentKey = s"$AWRS_SERVICE_NAME~AWRSRefNumber~XAAW00000123456"

  val jsonPostData = Json.parse("""{
                                  |  "businessCustomerDetails": {
                                  |    "businessName": "sdfsdf",
                                  |    "businessType": "Corporate Body",
                                  |    "businessAddress": {
                                  |      "line_1": "1 Example Street",
                                  |      "line_2": "Exampe View",
                                  |      "line_3": "Exampe Town",
                                  |      "line_4": "Exampeshire",
                                  |      "postcode": "AA1 1AA",
                                  |      "country": "GB"
                                  |    },
                                  |    "sapNumber": "1234567890",
                                  |    "safeId": "XE0001234567890",
                                  |    "isAGroup": false,
                                  |    "agentReferenceNumber": "JARN1234567"
                                  |  },
                                  |  "businessRegistrationDetails": {
                                  |    "legalEntity": "LTD",
                                  |    "isBusinessIncorporated": "Yes",
                                  |    "companyRegDetails": {
                                  |      "companyRegistrationNumber": "55555555",
                                  |      "dateOfIncorporation": "01/01/2015"
                                  |    },
                                  |    "doYouHaveVRN": "Yes",
                                  |    "vrn": "000000000",
                                  |    "doYouHaveUTR": "Yes",
                                  |    "utr": "utr"
                                  |  },
                                  |  "legalEntity": "LTD"
                                  |}
                                  |""".stripMargin)

  "return NO CONTENT" when {

    "AWRS feature flag is false" in {

      val successResponse: JsValue = Json.parse(
        s"""{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345","awrsRegistrationNumber": "DummyRef"}"""
      )

      stubbedPost(s"""$baseURI$subscriptionURI$safeId""", BAD_REQUEST, successResponse.toString)
      stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", OK)

      val controllerUrl = routes.EtmpCheckController.checkEtmp().url

      val resp: WSResponse = await(client(controllerUrl).post(jsonPostData))
      resp.status mustBe 204
    }

    "AWRS feature flag is true, but the status is rejected" in {

      val jsResultString =
        """
          |{
          |  "sapNumber": "1234567890",
          |  "safeId": "XE0001234567890",
          |  "agentReferenceNumber": "JARN1234567",
          |  "regimeIdentifiers": [
          |    {
          |      "regimeName": "AWRS",
          |      "regimeRefNumber": "XAAW00000123456"
          |    }
          |  ],
          |  "nonUKIdentification": {
          |    "idNumber": "123456",
          |    "issuingInstitution": "France Institution",
          |    "issuingCountryCode": "FR"
          |  },
          |  "isEditable": true,
          |  "isAnAgent": false,
          |  "isAnIndividual": true,
          |  "addressDetails": {
          |    "addressLine1": "100 SomeStreet",
          |    "addressLine2": "Wokingham",
          |    "addressLine3": "Surrey",
          |    "addressLine4": "London",
          |    "postalCode": "DH14EJ",
          |    "countryCode": "GB"
          |  },
          |  "contactDetails": {
          |    "regimeName": "AWRS",
          |    "phoneNumber": "01332752856",
          |    "mobileNumber": "07782565326",
          |    "faxNumber": "01332754256",
          |    "emailAddress": "stephen@manncorpone.co.uk"
          |  }
          |}
        """.stripMargin

      val successResponse: JsValue = Json.parse(
        s"""{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345","awrsRegistrationNumber": "XAAW00000123456"}"""
      )

      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

      val statusresp = Json.obj(
        "processingDate" -> "20/01/2010",
        "formBundleStatus" -> Rejected.name,
        "groupBusinessPartner" -> false
      )

      stubbedGet(s"""$regimeURI""", OK, jsResultString)
      stubbedPost(s"""$baseURI$subscriptionURI$safeId""", OK, successResponse.toString)
      stubbedGet(s"""$baseURI${subscriptionURI}XAAW00000123456/status""", OK, statusresp.toString())
      stubbedPost(s"/auth/authorise", OK,
        """
          |{
          | "affinityGroup": "Individual"
          |}
        """.stripMargin)
      stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", NO_CONTENT)

      val controllerUrl = routes.EtmpCheckController.checkEtmp().url

      val resp: WSResponse = await(client(controllerUrl).post(jsonPostData))
      resp.status mustBe 204
    }
  }

  "AWRS feature flag is true" in {

    val jsResultString =
      """
        |{"regimeIdentifiers": [
        |{
        |  "regimeName": "String",
        |  "regimeRefNumber": "AWRS"
        |}
        |]}
      """.stripMargin

    val successResponse: JsValue = Json.parse(
      s"""{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345","awrsRegistrationNumber": "DummyRef"}"""
    )

    FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

    stubbedGet(s"""$regimeURI""", OK, jsResultString)
    stubbedPost(s"""$baseURI$subscriptionURI$safeId""", BAD_REQUEST, successResponse.toString)
    stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", OK)

    val controllerUrl = routes.EtmpCheckController.checkEtmp().url

    val resp: WSResponse = await(client(controllerUrl).post(jsonPostData))
    resp.status mustBe 204
  }

  "return 200" when {

    "AWRS feature flag is true" in {

      val jsResultString =
        """
          |{
          |  "sapNumber": "1234567890",
          |  "safeId": "XE0001234567890",
          |  "agentReferenceNumber": "JARN1234567",
          |  "regimeIdentifiers": [
          |    {
          |      "regimeName": "AWRS",
          |      "regimeRefNumber": "XAAW00000123456"
          |    }
          |  ],
          |  "nonUKIdentification": {
          |    "idNumber": "123456",
          |    "issuingInstitution": "France Institution",
          |    "issuingCountryCode": "FR"
          |  },
          |  "isEditable": true,
          |  "isAnAgent": false,
          |  "isAnIndividual": false,
          |  "organisation": {
          |    "organisationName": "sdfsdf",
          |    "isAGroup": false,
          |    "organisationType": "Corporate body"
          |  },
          |  "addressDetails": {
          |    "addressLine1": "100 SomeStreet",
          |    "addressLine2": "Wokingham",
          |    "addressLine3": "Surrey",
          |    "addressLine4": "London",
          |    "postalCode": "DH14EJ",
          |    "countryCode": "GB"
          |  },
          |  "contactDetails": {
          |    "regimeName": "AWRS",
          |    "phoneNumber": "01332752856",
          |    "mobileNumber": "07782565326",
          |    "faxNumber": "01332754256",
          |    "emailAddress": "stephen@manncorpone.co.uk"
          |  }
          |}
        """.stripMargin

      val successResponse: JsValue = Json.parse(
        s"""{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345","awrsRegistrationNumber": "XAAW00000123456"}"""
      )

      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

      stubbedGet(s"""$regimeURI""", OK, jsResultString)
      stubbedPost(s"""$baseURI$subscriptionURI$safeId""", OK, successResponse.toString)
      stubbedPost(s"/auth/authorise", OK,
        """
          |{
          | "affinityGroup": "Organisation"
          |}
        """.stripMargin)
      stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", NO_CONTENT)

      val controllerUrl = routes.EtmpCheckController.checkEtmp().url

      val resp: WSResponse = await(client(controllerUrl).post(jsonPostData))
      resp.status mustBe 200
    }

    "AWRS feature flag is true for an individual" in {

      val jsResultString =
        """
          |{
          |  "sapNumber": "1234567890",
          |  "safeId": "XE0001234567890",
          |  "agentReferenceNumber": "JARN1234567",
          |  "regimeIdentifiers": [
          |    {
          |      "regimeName": "AWRS",
          |      "regimeRefNumber": "XAAW00000123456"
          |    }
          |  ],
          |  "nonUKIdentification": {
          |    "idNumber": "123456",
          |    "issuingInstitution": "France Institution",
          |    "issuingCountryCode": "FR"
          |  },
          |  "isEditable": true,
          |  "isAnAgent": false,
          |  "isAnIndividual": true,
          |  "addressDetails": {
          |    "addressLine1": "100 SomeStreet",
          |    "addressLine2": "Wokingham",
          |    "addressLine3": "Surrey",
          |    "addressLine4": "London",
          |    "postalCode": "DH14EJ",
          |    "countryCode": "GB"
          |  },
          |  "contactDetails": {
          |    "regimeName": "AWRS",
          |    "phoneNumber": "01332752856",
          |    "mobileNumber": "07782565326",
          |    "faxNumber": "01332754256",
          |    "emailAddress": "stephen@manncorpone.co.uk"
          |  }
          |}
        """.stripMargin

      val successResponse: JsValue = Json.parse(
        s"""{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345","awrsRegistrationNumber": "XAAW00000123456"}"""
      )

      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

      val statusresp = Json.obj(
        "processingDate" -> "20/01/2010",
        "formBundleStatus" -> Pending.name,
        "groupBusinessPartner" -> false
      )

      stubbedGet(s"""$regimeURI""", OK, jsResultString)
      stubbedPost(s"""$baseURI$subscriptionURI$safeId""", OK, successResponse.toString)
      stubbedGet(s"""$baseURI${subscriptionURI}XAAW00000123456/status""", OK, statusresp.toString())
      stubbedPost(s"/auth/authorise", OK,
        """
          |{
          | "affinityGroup": "Individual"
          |}
        """.stripMargin)
      stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", NO_CONTENT)

      val controllerUrl = routes.EtmpCheckController.checkEtmp().url

      val resp: WSResponse = await(client(controllerUrl).post(jsonPostData))
      resp.status mustBe 200
    }

  }

}
