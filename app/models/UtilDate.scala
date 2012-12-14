package models

import java.util.{ Calendar, Date, GregorianCalendar }
import java.text.SimpleDateFormat

object UtilDate {

  val v23h59min59s = ((24 * 60 * 60) - 1) * 1000
  val v1d = 24 * 60 * 60 * 1000
  val pattern = "today-([0-9]+)".r

  def getDate(sDate: String, addInMillis: Long = 0): GregorianCalendar = {
    val gCal = new GregorianCalendar()

    val mDate: GregorianCalendar = sDate match {
      case "all" => gCal.setTime(RequestData.getMinStartTime.getOrElse(new Date)); gCal
      case "today" => gCal
      case "yesterday" => gCal.add(Calendar.DATE, -1); gCal
      case pattern(days) => gCal.add(Calendar.DATE, -(days.toInt)); gCal
      case _ =>
        val f = new SimpleDateFormat("yyyy-MM-dd")
        gCal.setTime(f.parse(sDate))
        gCal
    }
    mDate.setTimeInMillis(mDate.getTimeInMillis + addInMillis)
    mDate
  }

  def formatDate(gCal: GregorianCalendar): String = {
    var rDate = gCal.get(Calendar.YEAR) + "-"
    rDate += addZero(gCal.get(Calendar.MONTH) + 1) + "-"
    rDate += addZero(gCal.get(Calendar.DATE)) + ""
    rDate
  }

  def addZero(f: Int): String = {
    if (f < 10) "0" + f.toString else f.toString
  }
}
