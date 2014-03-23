package models

object UtilConvert {

  def headersToString(headers: Map[String, String]): String = {
    if (headers != null) {
      headers.foldLeft("") {
        (string, header) => string + header._1 + " -> " + header._2 + "\n"
      }
    } else {
      ""
    }
  }

  def headersFromString(headersAsStr: String): Map[String, String] = {
    def headers = headersAsStr.split("\n").collect {
      case header =>
        val parts = header.split(" -> ")
        (parts(0), parts.tail.mkString(""))
    }
    headers.toMap
  }

}
