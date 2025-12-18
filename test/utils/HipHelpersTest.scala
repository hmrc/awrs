package utils

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.wordspec.AnyWordSpec

import play.api.libs.json.Json

class HipHelpersTest extends AnyWordSpec {

  "HipHelpers" should  {

    "successfully extract a Some of a code if code string present" in {
      val expectedErrorCode = "002"
      val hipError: String = Json.parse(
        s"""
          {
           |  "errors": {
           |    "processingDate": "2022-01-31T09:26:17Z",
           |    "code": "$expectedErrorCode",
           |    "text": "Some relevant description"
           |  }
           |}
           |""".stripMargin).toString()

      HipHelpers.extractHipErrorCode(hipError) mustBe Some(expectedErrorCode)
    }
  }

  "return None in case a code string is not present" in {
    val hipError: String = Json.parse(
      """
          {
         |  "errors": {
         |    "noCodeElement": "no code element present"
         |  }
         |}
         |""".stripMargin).toString()

    HipHelpers.extractHipErrorCode(hipError) mustBe None
  }

  "return None in case of an ivalid JSON string" in {
    val hipError: String =
      """
         |not a valid JSON string
         |""".stripMargin

    HipHelpers.extractHipErrorCode(hipError) mustBe None
  }
}
