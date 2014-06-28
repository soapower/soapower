package models

import java.util.{TimeZone, Calendar, Date, GregorianCalendar}
import java.text.SimpleDateFormat
import java.text.ParseException
import scala.util.Success
import scala.util.Failure
import play.Logger
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Await
import scala.concurrent.duration._
import org.joda.time.DateTime

object UtilDate {

  val v23h59min59s = ((24 * 60 * 60) - 1) * 1000
  val v1d = 24 * 60 * 60 * 1000
  val pattern = "today-([0-9]+)".r
  val longFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
  val defaultGCal = new GregorianCalendar()

  def getDate(sDate: String, addInMillis: Long = 0, isMax: Boolean = false): GregorianCalendar = {
    val gCal = new GregorianCalendar()

    gCal.set(Calendar.HOUR_OF_DAY, 0)
    gCal.set(Calendar.MINUTE, 0)
    gCal.set(Calendar.SECOND, 0)

    sDate match {
      case "all" => {
        if (isMax) {
          // the date is set to today date at 23 hour 59 minutes 59s
          gCal.setTimeInMillis(gCal.getTimeInMillis + v23h59min59s);
          return gCal
        }
        else {
          // the date is set to the start time of the oldest requestData
          val findMin = RequestData.getMinRequestData
          findMin.onComplete {
            case Success(request) =>
              if (request.isDefined) gCal.setTimeInMillis(request.get.startTime.getMillis)
              else gCal.setTime(new Date);

            case Failure(e) =>
              gCal.setTime(new Date);
          }
          Await.result(findMin, 5000 millis)
          return gCal
        }
      }
      case "today" =>
        if (isMax)
        // the date is set to todays date at 23 hour 59 minutes 59s
          gCal.setTimeInMillis(gCal.getTimeInMillis + v23h59min59s)
        return gCal

      case "yesterday" =>
        // the date is set to yesterdays date
        gCal.add(Calendar.DATE, -1)
        if (isMax) gCal.setTimeInMillis(gCal.getTimeInMillis + v23h59min59s)
        return gCal
      case pattern(days) =>
        gCal.add(Calendar.DATE, -(days.toInt));
        return gCal
      case _ =>
        try {
          val f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm")
          gCal.setTime(f.parse(sDate))
          return gCal
        }
        catch {
          case e: ParseException =>
            if (isMax) {
              gCal.setTimeInMillis(gCal.getTimeInMillis + v23h59min59s)
              return gCal
            }
            else {
              gCal.add(Calendar.DATE, -1)
              return gCal
            }
        }
    }

  }

  def formatDate(gCal: GregorianCalendar): String = {
    var rDate = gCal.get(Calendar.YEAR) + "-"
    rDate += addZero(gCal.get(Calendar.MONTH) + 1) + "-"
    rDate += addZero(gCal.get(Calendar.DATE)) + " "
    rDate += addZero(gCal.get(Calendar.HOUR_OF_DAY)) + ":"
    rDate += addZero(gCal.get(Calendar.MINUTE)) + ":"
    rDate += addZero(gCal.get(Calendar.SECOND))
    rDate
  }

  def getDateFormatees(date: Date): String = {
    defaultGCal.setTime(date).toString
    var rDate = defaultGCal.get(Calendar.YEAR) + "-"
    rDate += addZero(defaultGCal.get(Calendar.MONTH) + 1) + "-"
    rDate += addZero(defaultGCal.get(Calendar.DATE)) + " "
    rDate += addZero(defaultGCal.get(Calendar.HOUR_OF_DAY)) + ":"
    rDate += addZero(defaultGCal.get(Calendar.MINUTE)) + ":"
    rDate += addZero(defaultGCal.get(Calendar.SECOND)) + "."
    rDate += addZero(defaultGCal.get(Calendar.MILLISECOND))
    rDate
  }

  def parse(date: String): Date = {
    longFormat.parse(date)
  }

  def addZero(f: Int): String = {
    if (f < 10) "0" + f.toString else f.toString
  }

  def getGMTDateTime(date: Date): DateTime = {
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
    return new DateTime(date)
  }
}
