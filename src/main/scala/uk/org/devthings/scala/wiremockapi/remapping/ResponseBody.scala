package uk.org.devthings.scala.wiremockapi.remapping

private[wiremockapi] trait ResponseBodyOps {

  implicit class ResponseBodyStringOps(value: String) {
    def asJsonResponse: ResponseBody = ResponseBody.jsonBody(value)
    def asStringResponse: ResponseBody = ResponseBody.stringBody(value)
  }
}

object ResponseBody {
  def jsonBody(json: String): JsonResponseBody = JsonResponseBody(json)
  def binaryBody(value: List[Byte]): BinaryResponseBody = BinaryResponseBody(value)
  def stringBody(string: String): StringResponseBody = StringResponseBody(string)
}

sealed abstract class ResponseBody
case object EmptyResponseBody extends ResponseBody
case class BinaryResponseBody(value: List[Byte]) extends ResponseBody
case class StringResponseBody(value: String) extends ResponseBody
case class JsonResponseBody(value: String) extends ResponseBody {
  val jsonHeader: (String, String) = "content-type" -> "application/json"
}
