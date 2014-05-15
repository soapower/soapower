package models

import play.api.Play.current
import reactivemongo.bson._
import reactivemongo.api.collections.default.BSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json.Json
import scala.Some
import reactivemongo.api.collections.default.BSONCollection
import scala.Some
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONInteger
import reactivemongo.bson.BSONBoolean
import reactivemongo.bson.BSONString
import scala.Some
import reactivemongo.api.collections.default.BSONCollection

case class Stats (_id: Option[BSONObjectID],
                    groups: List[String],
                    environmentName: String,
                    serviceAction: String,
                    avgInMillis: Long,
                    nbOfRequestData: Long) {

  def this(groups: List[String], environmentName: String, serviceAction: String, avgInMillis: Long, nbOfRequestData: Long) =
  this(Some(BSONObjectID.generate), groups, environmentName, serviceAction, avgInMillis, nbOfRequestData)

}

object Stats {

  /*
   * Collection MongoDB
   */
  def collection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("stats")

  implicit val statsFormat = Json.format[Stats]

  implicit object StatsBSONReader extends BSONDocumentReader[Stats] {
    def read(doc: BSONDocument): Stats = {
      Stats(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[List[String]]("groups").toList.flatten,
        doc.getAs[String]("environmentName").get,
        doc.getAs[String]("serviceAction").get,
        doc.getAs[Long]("avgInMillis").get,
        doc.getAs[Long]("nbOfRequestData").get
      )
    }
  }
  implicit object StatsBSONWriter extends BSONDocumentWriter[Stats] {
    def write(stats: Stats): BSONDocument =
      BSONDocument(
        "_id" -> stats._id,
        "name" -> stats.groups,
        "environmentName" -> BSONString(stats.environmentName),
        "serviceAction" -> BSONString(stats.serviceAction),
        "avgInMillis" -> BSONLong(stats.avgInMillis),
        "nbOfRequestData" -> BSONLong(stats.nbOfRequestData))
  }
}
