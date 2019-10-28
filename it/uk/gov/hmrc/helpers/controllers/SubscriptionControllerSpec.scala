
package uk.gov.hmrc.helpers.controllers

import controllers.routes
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.helpers.{AuthHelpers, IntegrationSpec}
import utils.{AWRSFeatureSwitches, FeatureSwitch}

class SubscriptionControllerSpec extends IntegrationSpec with AuthHelpers  {

  val baseURI = "/alcohol-wholesaler-register"
  val subscriptionURI = "/subscription/"
  val regimeURI = "/registration/details"
  val safeId: String = "XE0001234567890"
  val AWRS_SERVICE_NAME = "HMRC-AWRS-ORG"
  val enrolmentKey = s"$AWRS_SERVICE_NAME~AWRSRefNumber~DummyRef"

  val jsonPostData = Json.parse("""{
                                  |  "subscriptionTypeFrontEnd": {
                                  |    "legalEntity": {
                                  |      "legalEntity": "LTD"
                                  |    },
                                  |    "businessPartnerName": "BusinessPartner",
                                  |    "groupDeclaration": {
                                  |      "groupRepConfirmation": true
                                  |    },
                                  |    "businessCustomerDetails": {
                                  |      "businessName": "sdfsdf",
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

  "return bad request" when {

    "AWRS feature flag is false" in {

      val successResponse: JsValue = Json.parse(
        s"""{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345","awrsRegistrationNumber": "DummyRef"}"""
      )

      stubbedPost(s"""$baseURI$subscriptionURI$safeId""", BAD_REQUEST, successResponse.toString)
      stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", OK)

      val controllerUrl = routes.OrgSubscriptionController.subscribe("test").url

      val resp: WSResponse = await(client(controllerUrl).post(jsonPostData))
      resp.status shouldBe 400
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

      //TODO: needs refactoring when completed solution in place as it will need to be passed into new function
      val successResponse: JsValue = Json.parse(
        s"""{"processingDate":"2015-12-17T09:30:47Z","etmpFormBundleNumber":"123456789012345","awrsRegistrationNumber": "DummyRef"}"""
      )

      FeatureSwitch.enable(AWRSFeatureSwitches.regimeCheck())

      stubbedGet(s"""$regimeURI""", OK, jsResultString)
      stubbedPost(s"""$baseURI$subscriptionURI$safeId""", BAD_REQUEST, successResponse.toString)
      stubbedPut(s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey", OK)

      val controllerUrl = routes.OrgSubscriptionController.subscribe("test").url

      val resp: WSResponse = await(client(controllerUrl).post(jsonPostData))
      resp.status shouldBe 400
    }

  }

}
