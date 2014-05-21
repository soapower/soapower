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
import org.joda.time.format.ISODateTimeFormat
import reactivemongo.core.commands.RawCommand

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
    val exists = findByGroupsEnvirServiceDate(stat)
    exists.onComplete {
      case Success(option) =>
        if (!option.isDefined) {
          Logger.debug("New statistics for env : "+stat.environmentName+", in groups : "+stat.groups.mkString(", ")+" and for serviceAction : "+stat.serviceAction+" at "+stat.atDate.toDate)
          collection.insert(stat)
        }
        else {
          Logger.debug("This statistic already exists")
        }
      case Failure(e) => throw new Exception("Error when inserting statistic")
    }
  }

  /**
   * Find a stat using groups, environmentName and serviceaction
   * @param stat
   * @return
   */
  def findByGroupsEnvirServiceDate(stat: Stat): Future[Option[Stat]] = {
    val find = BSONDocument("groups" -> stat.groups, "environmentName" -> stat.environmentName, "serviceAction" -> stat.serviceAction, "atDate" -> BSONDocument("$eq" -> BSONDateTime(stat.atDate.toDate.getTime)))
    collection
      .find(find)
      .one[Stat]
  }

  case class PageStat(groups: List[String], environmentName: String, serviceAction: String, avgInMillis: Long, treshold: Long)

  /**
   * Return a page of stats
   * @param groups
   * @param environmentName
   * @param minDate
   * @param maxDate
   * @return a list of (groups, environmentName, serviceaction, avgTime, Treshold)
   */
  def find(groups: String, environmentName: String, minDate: Date, maxDate: Date): Future[List[PageStat]] = {

    // First, we retrieve a map of all serviceactions and their treshold, organized by name and groups
    val serviceActions = Await.result(ServiceAction.findAll, 1.second).map {
      sa =>
        ((sa.name, sa.groups), sa.thresholdms)
    }.toMap

    // We remove 1000 millisecond to minDate to avoid issue with last two milliseconds being random
    // when mindate is set to yesterday
    var matchQuery = BSONDocument("atDate" -> BSONDocument(
      "$gt" -> BSONDateTime(minDate.getTime - 1000),
      "$lte" -> BSONDateTime(maxDate.getTime))
    )

    if(groups != "all") {
      matchQuery = matchQuery ++ ("groups" -> BSONDocument("$in" -> groups.split(',')))
    }
    if(environmentName != "all") {
      matchQuery = matchQuery ++ ("environmentName" -> environmentName)
    }

    val command =
      BSONDocument(
        "aggregate" -> collection.name, // we aggregate on collection
        "pipeline" -> BSONArray(
          BSONDocument(
            "$match" -> matchQuery
          ),
          BSONDocument(
            "$project" -> BSONDocument(
              "groups" -> "$groups",
              "environmentName" -> "$environmentName",
              "serviceAction" -> "$serviceAction",
              "atDate" -> "$atDate",
              "nbOfRequestData" -> "$nbOfRequestData",
              "ponderateAvg" -> BSONDocument("$multiply" -> BSONArray("$nbOfRequestData", "$avgInMillis"))
            )
          ),
          BSONDocument(
            "$group" -> BSONDocument(
              "_id" -> BSONDocument("groups" -> "$groups",
                                    "environmentName" -> "$environmentName",
                                    "serviceAction" -> "$serviceAction"
              ),
              "sumOfPonderate" -> BSONDocument(
                "$sum" -> "$ponderateAvg"
              ),
              "totalRequest" -> BSONDocument(
                "$sum" -> "$nbOfRequestData"
              )
            )
          ),
          BSONDocument(
            "$project" -> BSONDocument(
              "groups" -> "$groups",
              "serviceAction" -> "$serviceAction",
              "totalAvg" -> BSONDocument(
                "$divide" -> BSONArray(
                  "$sumOfPonderate",
                  "$totalRequest"
                )
              )
            )
          )
        )
      )

    // Perform the query
    val query = ReactiveMongoPlugin.db.command(RawCommand(command))
    query.map {
      list =>
        // Decode the result
        var listRes = ListBuffer.empty[PageStat]
        list.elements.foreach {
          document =>
            if(document._1 == "result") {
              // We retrieve the result element (contain all the results)
              if(document._2.isInstanceOf[BSONArray]) {
                // Each result is a BSONArray containing a list of BSONDocuments
                document._2.asInstanceOf[BSONArray].values.foreach {
                  result =>
                    var sa = ""
                    var groups = ListBuffer.empty[String]
                    var avg = 0.toLong
                    if(result.isInstanceOf[BSONDocument]) {
                      // Each BSONDocument is a statistics composed of the id (groups and service action) and the avg time
                      result.asInstanceOf[BSONDocument].elements.foreach {
                        stat =>
                          if(stat._1 == "_id") {
                            stat._2.asInstanceOf[BSONDocument].elements.foreach {
                              groupsOrService =>
                                if(groupsOrService._1 == "groups") {
                                  // For each element we retrieve its groups
                                  groupsOrService._2.asInstanceOf[BSONArray].values.foreach{
                                    group =>
                                      groups += group.asInstanceOf[BSONString].value
                                  }
                                }
                                if(groupsOrService._1 == "serviceAction") {
                                  sa = groupsOrService._2.asInstanceOf[BSONString].value
                                }
                            }
                          }
                          else if (stat._1 == "totalAvg") {
                            // We retrieve the average
                            avg = stat._2.asInstanceOf[BSONDouble].value.toLong
                          }
                      }
                    }
                    val pageStat = new PageStat(groups.toList, environmentName, sa, avg, serviceActions.apply((sa, groups.toList)))
                    listRes += pageStat
                }
              }
            }
        }
       listRes.toList
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

