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

case class Stat (_id: Option[BSONObjectID],
                    groups: List[String],
                    environmentName: String,
                    serviceAction: String,
                    avgInMillis: Long,
                    nbOfRequestData: Long,
                    atDate: DateTime) {

  def this(groups: List[String], environmentName: String, serviceAction: String, avgInMillis: Long, nbOfRequestData: Long, atDate:DateTime) =
  this(Some(BSONObjectID.generate), groups, environmentName, serviceAction, avgInMillis, nbOfRequestData, atDate)

}

object Stat {

  /*
   * Collection MongoDB
   * The collection cannot be named "stats"
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
   * Insert stat if it doesn't already exists, update stat otherwise
   * @param stat
   */
  def insert(stat: Stat) = {
    val exists = findByGroupsEnvirService(stat)
    exists.onComplete {
      case Success(option) =>
        if(!option.isDefined) {
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

  def find(groups: String, environmentName: String, minDate: Date, maxDate: Date) : Future[List[Stat]] = {
    var query = BSONDocument()
    if(groups != "all") {
      query = query ++ ("groups" -> BSONDocument("$in" -> groups.split(',')))
    }
    if(environmentName != "all") {
      query = query ++ ("environmentName" -> environmentName)
    }
    // TODO
    // dates and stats avg
    collection.
      find(query).
      cursor[Stat].
      collect[List]()
  }


}
