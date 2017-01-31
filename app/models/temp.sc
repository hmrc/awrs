import play.api.libs.json.JsValue
import play.api.libs.json.Json

val jsonString = "{\n  \"name\" : \"Watership Down\",\n  \"location\" : {\n    \"lat\" : 51.235685,\n    \"long\" : -1.309197\n  },\n  \"residents\" : [ {\n    \"name\" : \"Fiver\",\n    \"age\" : 4,\n    \"role\" : null\n  }, {\n    \"name\" : \"Bigwig\",\n    \"age\" : 6,\n    \"role\" : \"Owsla\"\n  } ]\n}"

val g: JsValue = Json.parse(jsonString)

(g \ "ghfdhgfd").validateOpt[String]