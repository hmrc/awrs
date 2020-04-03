
package uk.gov.hmrc.helpers.controllers

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlMatching}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import controllers.routes
import org.scalatest.MustMatchers
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.helpers.{AuthHelpers, IntegrationSpec}
import utils.{AWRSFeatureSwitches, FeatureSwitch}

class SubscriptionControllerSpec extends IntegrationSpec with AuthHelpers with MustMatchers {

  val baseURI = "/alcohol-wholesaler-register"
  val subscriptionURI = "/subscription/"
  val regimeURI = "/registration/details"
  val safeId: String = "XE0001234567890"
  val AWRS_SERVICE_NAME = "HMRC-AWRS-ORG"
  val enrolmentKey = s"$AWRS_SERVICE_NAME~AWRSRefNumber~XAAW00000123456"


  val jsonOrgPostData = Json.parse("""{
                                    |  "subscriptionTypeFrontEnd": {
                                    |    "legalEntity": {
                                    |      "legalEntity": "LTD"
                                    |    },
                                    |    "businessPartnerName": "BusinessPartner",
                                    |    "groupDeclaration": {
                                    |      "groupRepConfirmation": true
                                    |    },
                                    |    "businessCustomerDetails": {
                                    |      "businessName": "Trading Trade",
                                    |      "businessType": "Corporate Body",
                                    |      "businessAddress": {
                                    |        "line_1": "1 Example Street",
                                    |        "line_2": "Exampe View",
                                    |        "line_3": "Exampe Town",
                                    |        "line_4": "Exampeshire",
                                    |        "postcode": "AA1 1AA",
                                    |        "country": "GB"
                                    |      },
                                    |      "sapNumber": "1234567890",
                                    |      "safeId": "XE0001234567890",
                                    |      "isAGroup": false,
                                    |      "agentReferenceNumber": "JARN1234567"
                                    |    },
                                    |    "businessDetails": {
                                    |      "doYouHaveTradingName": "Yes",
                                    |      "tradingName": "Trading name",
                                    |      "newAWBusiness": {
                                    |        "newAWBusiness": "No"
                                    |      }
                                    |    },
                                    |    "businessRegistrationDetails": {
                                    |      "legalEntity": "LTD",
                                    |      "isBusinessIncorporated": "Yes",
                                    |      "companyRegDetails": {
                                    |        "companyRegistrationNumber": "55555555",
                                    |        "dateOfIncorporation": "01/01/2015"
                                    |      },
                                    |      "doYouHaveVRN": "Yes",
                                    |      "vrn": "000000000",
                                    |      "doYouHaveUTR": "Yes",
                                    |      "utr": "$utr"
                                    |    },
                                    |    "placeOfBusiness": {
                                    |      "mainPlaceOfBusiness": "Yes",
                                    |      "placeOfBusinessLast3Years": "Yes",
                                    |      "operatingDuration": "over 10 years",
                                    |      "modelVersion" : "1.0"
                                    |    },
                                    |    "businessContacts": {
                                    |      "contactAddressSame": "Yes",
                                    |      "contactFirstName": "first name",
                                    |      "contactLastName": "last name",
                                    |      "email": "Email@email.com",
                                    |      "telephone": "07000111222",
                                    |      "modelVersion" : "1.1"
                                    |    },
                                    |    "additionalPremises": {
                                    |      "premises": [
                                    |        {
                                    |          "additionalPremises": "Yes",
                                    |          "additionalAddress": {
                                    |            "postcode": "BB1 1BB",
                                    |            "addressLine1": "150 Example Avenue",
                                    |            "addressLine2": "Exampletown"
                                    |          },
                                    |          "addAnother": "No"
                                    |        }
                                    |      ]
                                    |    },
                                    |    "businessDirectors": {
                                    |      "directors": [
                                    |        {
                                    |          "directorsAndCompanySecretaries": "Director",
                                    |          "personOrCompany": "person",
                                    |          "firstName": "sdfsd",
                                    |          "lastName": "sdfsdfsd",
                                    |          "doTheyHaveNationalInsurance": "Yes",
                                    |          "nino": "$nino",
                                    |          "otherDirectors": "Yes"
                                    |        },
                                    |        {
                                    |          "directorsAndCompanySecretaries": "Director and Company Secretary",
                                    |          "personOrCompany": "person",
                                    |          "firstName": "Example",
                                    |          "lastName": "Exampleson",
                                    |          "doTheyHaveNationalInsurance": "Yes",
                                    |          "nino": "$nino",
                                    |          "otherDirectors": "No"
                                    |        }
                                    |      ],
                                    |      "modelVersion" : "1.0"
                                    |    },
                                    |    "tradingActivity": {
                                    |      "wholesalerType": [
                                    |        "06",
                                    |        "01",
                                    |        "02",
                                    |        "03",
                                    |        "04",
                                    |        "05"
                                    |      ],
                                    |      "typeOfAlcoholOrders": [
                                    |        "01",
                                    |        "02",
                                    |        "03",
                                    |        "04"
                                    |      ],
                                    |      "doesBusinessImportAlcohol": "Yes",
                                    |      "doYouExportAlcohol": "Yes",
                                    |      "exportLocation": [
                                    |        "euDispatches",
                                    |        "outsideEU"
                                    |      ]
                                    |    },
                                    |    "products": {
                                    |      "mainCustomers": [
                                    |        "01",
                                    |        "02",
                                    |        "03",
                                    |        "04",
                                    |        "05",
                                    |        "06",
                                    |        "07",
                                    |        "08"
                                    |      ],
                                    |      "doesBusinessImportAlcohol": "Yes",
                                    |      "productType": [
                                    |        "02",
                                    |        "03",
                                    |        "05",
                                    |        "06",
                                    |        "99"
                                    |      ],
                                    |      "otherProductType": "rwerwrwerew"
                                    |    },
                                    |    "suppliers": {
                                    |      "suppliers": [
                                    |        {
                                    |          "alcoholSuppliers": "Yes",
                                    |          "supplierName": "dfgdfgfd",
                                    |          "vatRegistered": "No",
                                    |          "supplierAddress": {
                                    |            "postcode": "BB1 1BB",
                                    |            "addressLine1": "150 Example Avenue",
                                    |            "addressLine2": "Example Avenue"
                                    |          },
                                    |          "additionalSupplier": "No"
                                    |        }
                                    |      ]
                                    |    },
                                    |    "applicationDeclaration": {
                                    |      "declarationName": "AWRS application",
                                    |      "declarationRole": "AWRS application1"
                                    |    },
                                    |    "modelVersion": "1.0"
                                    |  }
                                    |}
                                    |""".stripMargin)


  val jsonIndividualPostData = Json.parse("""{
                                            |  "subscriptionTypeFrontEnd": {
                                            |    "legalEntity": {
                                            |      "legalEntity": "SOP"
                                            |    },
                                            |    "businessPartnerName": "BusinessPartner",
                                            |    "businessCustomerDetails": {
                                            |      "businessName": "Example Name",
                                            |      "businessType": "Sole Trader",
                                            |      "businessAddress": {
                                            |        "line_1": "1 Example Street",
                                            |        "line_2": "Exampe View",
                                            |        "line_3": "Exampe Town",
                                            |        "line_4": "Exampeshire",
                                            |        "postcode": "AA1 1AA",
                                            |        "country": "GB"
                                            |      },
                                            |      "sapNumber": "1234567890",
                                            |      "safeId": "XE0001234567890",
                                            |      "isAGroup": false,
                                            |      "agentReferenceNumber": "01234567890",
                                            |      "firstName": "Example",
                                            |      "lastName": "Exampleson"
                                            |    },
                                            |    "businessDetails": {
                                            |      "doYouHaveTradingName": "Yes",
                                            |      "tradingName": "Example Trading",
                                            |      "newAWBusiness": {
                                            |        "newAWBusiness": "No"
                                            |      }
                                            |    },
                                            |    "businessRegistrationDetails": {
                                            |      "legalEntity": "SOP",
                                            |      "doYouHaveNino": "Yes",
                                            |      "nino": "$nino",
                                            |      "doYouHaveVRN": "Yes",
                                            |      "vrn": "000000000",
                                            |      "doYouHaveUTR": "Yes",
                                            |      "utr": "$utr"
                                            |    },
                                            |    "placeOfBusiness": {
                                            |      "mainPlaceOfBusiness": "Yes",
                                            |      "placeOfBusinessLast3Years": "Yes",
                                            |      "operatingDuration": "2 to 5 years",
                                            |      "modelVersion" : "1.0"
                                            |    },
                                            |    "businessContacts": {
                                            |      "contactAddressSame": "Yes",
                                            |      "contactFirstName": "Example",
                                            |      "contactLastName": "Exampleson",
                                            |      "email": "Example@gmail.com",
                                            |      "confirmEmail": "Example@gmail.com",
                                            |      "telephone": "01234567891",
                                            |      "modelVersion" : "1.0"
                                            |    },
                                            |    "additionalPremises": {
                                            |      "premises": [
                                            |        {
                                            |          "additionalPremises": "Yes",
                                            |          "additionalAddress": {
                                            |            "postcode": "AA1 1AA",
                                            |            "addressLine1": "82 Example Street",
                                            |            "addressLine2": "Exampledon"
                                            |          },
                                            |          "addAnother": "Yes"
                                            |        },
                                            |        {
                                            |          "additionalPremises": "Yes",
                                            |          "additionalAddress": {
                                            |            "postcode": "AA1 1AA",
                                            |            "addressLine1": "82 Example Street",
                                            |            "addressLine2": "Exampledon"
                                            |          },
                                            |          "addAnother": "No"
                                            |        }
                                            |      ]
                                            |    },
                                            |    "tradingActivity": {
                                            |      "wholesalerType": [
                                            |        "01",
                                            |        "02",
                                            |        "03",
                                            |        "04",
                                            |        "05",
                                            |        "99"
                                            |      ],
                                            |      "otherWholesaler": "Other wholesaler type",
                                            |      "typeOfAlcoholOrders": [
                                            |        "02",
                                            |        "03",
                                            |        "99"
                                            |      ],
                                            |      "otherTypeOfAlcoholOrders": "Other alcohol order",
                                            |      "doesBusinessImportAlcohol": "Yes",
                                            |      "thirdPartyStorage": "Yes",
                                            |      "doYouExportAlcohol": "Yes",
                                            |      "exportLocation": [
                                            |        "euDispatches",
                                            |        "outsideEU"
                                            |      ]
                                            |    },
                                            |    "products": {
                                            |      "mainCustomers": [
                                            |        "02",
                                            |        "03"
                                            |      ],
                                            |      "productType": [
                                            |        "03",
                                            |        "99"
                                            |      ],
                                            |      "otherProductType": "Other product type"
                                            |    },
                                            |    "suppliers": {
                                            |      "suppliers": [
                                            |        {
                                            |          "alcoholSuppliers": "Yes",
                                            |          "supplierName": "Example Supplier",
                                            |          "vatRegistered": "No",
                                            |          "supplierAddress": {
                                            |            "postcode": "BB1 1BB",
                                            |            "addressLine1": "3 Example Road",
                                            |            "addressLine2": "Exampledon"
                                            |          },
                                            |          "additionalSupplier": "Yes"
                                            |        },
                                            |        {
                                            |          "alcoholSuppliers": "Yes",
                                            |          "supplierName": "ExampleName",
                                            |          "supplierAddress": {
                                            |            "postcode": "BB1 1BB",
                                            |            "addressLine1": "3 Example Road",
                                            |            "addressLine2": "Exampledon"
                                            |          },
                                            |          "additionalSupplier": "No"
                                            |        }
                                            |      ]
                                            |    },
                                            |    "applicationDeclaration": {
                                            |      "declarationName": "Example Exampleson",
                                            |      "declarationRole": "Other"
                                            |    },
                                            |    "modelVersion": "1.0"
                                            |  }
                                            |}
                                            |""".stripMargin)


  def stubShowAndRedirectExternalCalls(affinityGroup: String): StubMapping = {
    stubFor(post(urlMatching("/auth/authorise"))
      .willReturn(
        aResponse()
          .withStatus(OK)
          .withBody(
            s"""{
               |  "authorisedEnrolments": [{}],
               |  "affinityGroup": "$affinityGroup",
               |  "credentials": {"providerId": "12345-credId", "providerType": "GovernmentGateway"},
               |  "authProviderId": { "ggCredId": "123" }
               |}""".stripMargin
          )
      )
    )
  }

  "return bad request" when {

    "AWRS feature flag is false" in {

      val successResponse: JsValue = Json.parse(
        s"""{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345","awrsRegistrationNumber": "DummyRef"}"""
      )

      stubbedPost(s"""$baseURI$subscriptionURI$safeId""", BAD_REQUEST, successResponse.toString)
      stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", OK)

      val controllerUrl = routes.OrgSubscriptionController.subscribe("test").url

      val resp: WSResponse = await(client(controllerUrl).post(jsonOrgPostData))
      resp.status mustBe 400
    }

    "AWRS feature flag is true and malformed json response from regime end point" in {

      val jsResultString =
        """
          |{"regimeIdentifiers": [
          |{
          |  "regimeName": "String",
          |  "regimeRefNumber": "AWRS"
          |}
          |]}
        """.stripMargin

      val failureResponse: JsValue = Json.parse(
        s"""{"reason": "Resource not found"}"""
      )

      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

      stubbedGet(s"""$regimeURI""", OK, jsResultString)
      stubbedPost(s"""$baseURI$subscriptionURI$safeId""", BAD_REQUEST, failureResponse.toString)
      stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", OK)

      val controllerUrl = routes.OrgSubscriptionController.subscribe("test").url

      val resp: WSResponse = await(client(controllerUrl).post(jsonOrgPostData))
      resp.status mustBe 400
    }

    "AWRS feature flag is true and Organisation data does not match" in {

      val jsResultString =
        """
          |{
          |  "sapNumber": "1234567890",
          |  "safeId": "XE0001234567890",
          |  "agentReferenceNumber": "AARN1234567",
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
          |    "organisationName": "ACME Trading",
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

      val failureResponse: JsValue = Json.parse(
        s"""{"reason": "Resource not found"}"""
      )

      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
      stubShowAndRedirectExternalCalls("Organisation")

      stubbedGet(s"""$regimeURI""", OK, jsResultString)
      stubbedPost(s"""$baseURI$subscriptionURI$safeId""", BAD_REQUEST, failureResponse.toString)
      stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", NO_CONTENT)

      val controllerUrl = routes.OrgSubscriptionController.subscribe("test").url

      val resp: WSResponse = await(client(controllerUrl).post(jsonOrgPostData))
      resp.status mustBe 400
    }

    "AWRS feature flag is true and Individual data does not match" in {

      val jsResultString =
        """
          |{
          |  "sapNumber": "1234567890",
          |  "safeId": "XE0001234567890",
          |  "regimeIdentifiers": [
          |    {
          |      "regimeName": "AWRS",
          |      "regimeRefNumber": "XAAW00000123456"
          |    }
          |  ],
          |  "isEditable": true,
          |  "isAnAgent": false,
          |  "isAnIndividual": true,
          |  "individual": {
          |        "firstName": "first",
          |        "lastName": "last",
          |        "dateOfBirth": "1980-12-25"
          |        },
          |  "addressDetails": {
          |    "addressLine1": "100 SomeStreet",
          |    "addressLine2": "Wokingham",
          |    "addressLine3": "Surrey",
          |    "addressLine4": "London",
          |    "postalCode": "DH14EJ",
          |    "countryCode": "GB"
          |  },
          |  "contactDetails": {}
          |}
        """.stripMargin

      val failureResponse: JsValue = Json.parse(
        s"""{"reason": "Resource not found"}"""
      )

      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
      stubShowAndRedirectExternalCalls("Individual")

      stubbedGet(s"""$regimeURI""", OK, jsResultString)
      stubbedPost(s"""$baseURI$subscriptionURI$safeId""", BAD_REQUEST, failureResponse.toString)
      stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", NO_CONTENT)

      val controllerUrl = "/awrs/send-data"

      val resp: WSResponse = await(client(controllerUrl).post(jsonIndividualPostData))
      resp.status mustBe 400
    }


    "return 202" when {
      "Awrs Feature flag is true and organisation details match" in {

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
            |    "organisationName": "Trading Trade",
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

        val failureResponse: JsValue = Json.parse(
          s"""{"reason": "Resource not found"}"""
        )

        FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
        stubShowAndRedirectExternalCalls("Organisation")

        stubbedGet(s"""$regimeURI""", OK, jsResultString)
        stubbedPost(s"""$baseURI$subscriptionURI$safeId""", BAD_REQUEST, failureResponse.toString)
        stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", NO_CONTENT)

        val controllerUrl = routes.OrgSubscriptionController.subscribe("test").url

        val resp: WSResponse = await(client(controllerUrl).post(jsonOrgPostData))
        resp.status mustBe 202
      }
    }

    "AWRS feature flag is true for an individual" in {

      val jsResultString =
        """
          |{
          |  "sapNumber": "1234567890",
          |  "safeId": "XE0001234567890",
          |  "regimeIdentifiers": [
          |    {
          |      "regimeName": "AWRS",
          |      "regimeRefNumber": "XAAW00000123456"
          |    }
          |  ],
          |  "isEditable": true,
          |  "isAnAgent": false,
          |  "isAnIndividual": true,
          |  "individual": {
          |        "firstName": "Example",
          |        "lastName": "Exampleson",
          |        "dateOfBirth": "1980-12-25"
          |        },
          |  "addressDetails": {
          |    "addressLine1": "100 SomeStreet",
          |    "addressLine2": "Wokingham",
          |    "addressLine3": "Surrey",
          |    "addressLine4": "London",
          |    "postalCode": "AA1 1AA",
          |    "countryCode": "GB"
          |  },
          |  "contactDetails": {}
          |}
        """.stripMargin

      val successResponse: JsValue = Json.parse(
        s"""{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345","awrsRegistrationNumber": "XAAW00000123456"}"""
      )

      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())
      stubShowAndRedirectExternalCalls("Individual")

      stubbedGet(s"""$regimeURI""", OK, jsResultString)
      stubbedPost(s"""$baseURI$subscriptionURI$safeId""", BAD_REQUEST, successResponse.toString)
      stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", NO_CONTENT)

      val controllerUrl = routes.SaSubscriptionController.subscribe("test").url

      val resp: WSResponse = await(client(controllerUrl).post(jsonIndividualPostData))
      resp.status mustBe 202
    }
  }
}
