package models

import play.api.Play.current
import reactivemongo.bson._
import reactivemongo.api.collections.default.BSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json.Json
import scala.Some
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONInteger
import reactivemongo.bson.BSONBoolean
import reactivemongo.bson.BSONString
import scala.Some
import reactivemongo.api.collections.default.BSONCollection
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.Logger
import scala.util.Success
import scala.util.Failure
import org.joda.time.DateTime
import java.util.Date
import scala.collection.mutable.ListBuffer

case class Stat(_id: Option[BSONObjectID],
                groups: List[String],
                environmentName: String,
                serviceAction: String,
                avgInMillis: Long,
                nbOfRequestData: Long,
                atDate: DateTime) {

  def this(groups: List[String], environmentName: String, serviceAction: String, avgInMillis: Long, nbOfRequestData: Long, atDate: DateTime) =
    this(Some(BSONObjectID.generate), groups, environmentName, serviceAction, avgInMillis, nbOfRequestData, atDate)

}

object Stat {

  /*
   * Collection MongoDB
   *
   */
  def collection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("statistics")

  implicit val statsFormat = Json.format[Stat]

  implicit object StatsBSONReader extends BSONDocumentReader[Stat] {
    def read(doc: BSONDocument): Stat = {
      Stat(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[List[String]]("groups").toList.flatten,
        doc.getAs[String]("environmentName").get,
        doc.getAs[String]("serviceAction").get,
        doc.getAs[Long]("avgInMillis").get,
        doc.getAs[Long]("nbOfRequestData").get,
        new DateTime(doc.getAs[BSONDateTime]("atDate").get.value)
      )
    }
  }

  implicit object StatsBSONWriter extends BSONDocumentWriter[Stat] {
    def write(stat: Stat): BSONDocument =
      BSONDocument(
        "_id" -> stat._id,
        "groups" -> stat.groups,
        "environmentName" -> BSONString(stat.environmentName),
        "serviceAction" -> BSONString(stat.serviceAction),
        "avgInMillis" -> BSONLong(stat.avgInMillis),
        "nbOfRequestData" -> BSONLong(stat.nbOfRequestData),
        "atDate" -> BSONDateTime(stat.atDate.getMillis))
  }

  /**
   * Insert stat
   * @param stat
   */
  def insert(stat: Stat) = {
    val exists = findByGroupsEnvirService(stat)
    exists.onComplete {
      case Success(option) =>
        if (!option.isDefined) {
          collection.insert(stat)
        }
      case Failure(e) => throw new Exception("Error when inserting statistic")
    }
  }

  /**
   * Find a stat using groups, environmnentName and serviceaction
   * @param stat
   * @return
   */
  def findByGroupsEnvirService(stat: Stat): Future[Option[Stat]] = {
    val find = BSONDocument("groups" -> stat.groups, "environmentName" -> stat.environmentName, "serviceAction" -> stat.serviceAction)
    collection
      .find(find)
      .one[Stat]
  }

  /**
   * Return a page of stats
   * @param groups
   * @param environmentName
   * @param minDate
   * @param maxDate
   * @return a list of (groups, environmentName, serviceaction, avgTime, Treshold)
   */
  def find(groups: String, environmentName: String, minDate: Date, maxDate: Date): Future[List[(String, String, String, Long, Long)]] = {
    var query = BSONDocument()
    if (groups != "all") {
      query = query ++ ("groups" -> BSONDocument("$in" -> groups.split(',')))
    }
    if (environmentName != "all") {
      query = query ++ ("environmentName" -> environmentName)
    }
    // We retrieve a map of all serviceactions and their treshold, organized by name and groups
    val serviceActions = Await.result(ServiceAction.findAll, 1.second).map {
      sa =>
        ((sa.name, sa.groups), sa.thresholdms)
    }.toMap

    query = query ++ ("atDate" -> BSONDocument(
      "$gte" -> BSONDateTime(minDate.getTime),
      "$lt" -> BSONDateTime(maxDate.getTime))
      )
    collection.
      find(query).
      cursor[Stat].
      collect[List]().map {
      list =>
        var res = ListBuffer.empty[(String, String, String, Long, Long)]
        // We group by all the stats by groups
        val map = list.groupBy(_.groups)
        map.foreach {
          statsByGroups =>
            val statsByServiceActions = statsByGroups._2.groupBy(_.serviceAction)

            statsByServiceActions.foreach {
              statsBySingleService =>
              // For each stats, grouped by serviceaction, we calculate the average time of response
                var avg = 0.toLong
                var nb = 0.toLong
                statsBySingleService._2.foreach {
                  stat =>
                    avg += stat.avgInMillis * stat.nbOfRequestData
                    nb += stat.nbOfRequestData
                }
                avg = avg / nb

                val treshold = serviceActions.apply((statsBySingleService._1, statsByGroups._1))
                res += ((statsByGroups._1.mkString(", "), environmentName, statsBySingleService._1, avg, treshold))
            }
            res
        }
        res.toList
    }
  }

  def findResponseTimes(groups: String, environmentName: String, serviceAction: String, minDate: Date, maxDate: Date, status: String): Future[List[Stat]] = {
    var query = BSONDocument()
    if (groups != "all") {
      query = query ++ ("groups" -> BSONDocument("$in" -> groups.split(',')))
    }
    if (environmentName != "all") {
      query = query ++ ("environmentName" -> environmentName)
    }

    query = query ++ ("atDate" -> BSONDocument(
      "$gte" -> BSONDateTime(minDate.getTime),
      "$lt" -> BSONDateTime(maxDate.getTime))
      )

    if (serviceAction != "all") {
      query = query ++ ("serviceAction" -> serviceAction)
    }

    collection.find(query).cursor[Stat].collect[List]()
  }
}

