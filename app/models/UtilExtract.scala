package models

import play.Logger

object UtilExtract {

  /**
   * An url is composed of the following members :
   * protocol://host:port/path
   * This operation return the URL's path
   * @param textualURL The url from which extract and return the path
   * @return The url's path or none if it's not a URL with a valid path
   */
  def extractPathFromURL(textualURL: String): Option[String] = {
    try {
      // Search the first "/" since index 10 (http://) to find the third "/"
      // and take the String from this index
      // Add +1 to remove the / to have path instead of /path
      // Using substring and not java.net.URL,
      // explanations : https://github.com/soapower/soapower/pull/33#issuecomment-21371242
      Some(textualURL.substring(textualURL.indexOf("/", 10) + 1))
    } catch {
      case e: IndexOutOfBoundsException => {
        Logger.error("Invalid remoteTarget:" + textualURL)
        None
      }
    }
  }
}
