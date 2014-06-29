package models

import play.api.Play.current
import play.api.cache._

import java.util.{Calendar, GregorianCalendar}
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.libs.json._
import reactivemongo.api.indexes.{IndexType, Index}
import reactivemongo.bson._
import scala.concurrent.{Await, Future}
import play.modules.reactivemongo.json.BSONFormats._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.core.commands.RawCommand
import play.api.Logger
import reactivemongo.api.collections.default.BSONCollection

case class Environment(_id: Option[BSONObjectID],
                       name: String,
                       groups: List[String],
                       hourRecordContentDataMin: Int = 8,
                       hourRecordContentDataMax: Int = 22,
                       nbDayKeepContentData: Int = 2,
                       nbDayKeepAllData: Int = 5,
                       recordContentData: Boolean = true,
                       recordData: Boolean = true)

object ModePurge extends Enumeration {
  type ModePurge = Value
  val CONTENT, ALL = Value
}

object Environment {

  /*
   * Collection MongoDB
   */
  def collection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("environments")

  def ensureIndexes() {
    Logger.info("Collection environments, ensure index")
    collection.indexesManager.ensure(Index(Seq("groups" -> IndexType.Ascending, "name" -> IndexType.Ascending)))
    collection.indexesManager.ensure(Index(Seq("name" -> IndexType.Ascending)))
  }

  implicit val environmentFormat = Json.format[Environment]

  implicit object EnvironmentBSONReader extends BSONDocumentReader[Environment] {
    def read(doc: BSONDocument): Environment = {
      Environment(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[String]("name").get,
        doc.getAs[List[String]]("groups").toList.flatten,
        doc.getAs[Int]("hourRecordContentDataMin").get,
        doc.getAs[Int]("hourRecordContentDataMax").get,
        doc.getAs[Int]("nbDayKeepContentData").get,
        doc.getAs[Int]("nbDayKeepAllData").get,
        doc.getAs[Boolean]("recordContentData").get,
        doc.getAs[Boolean]("recordData").get
      )
    }
  }

  implicit object EnvironmentBSONWriter extends BSONDocumentWriter[Environment] {
    def write(environment: Environment): BSONDocument =
      BSONDocument(
        "_id" -> environment._id,
        "name" -> BSONString(environment.name),
        "hourRecordContentDataMin" -> BSONInteger(environment.hourRecordContentDataMin),
        "hourRecordContentDataMax" -> BSONInteger(environment.hourRecordContentDataMax),
        "nbDayKeepContentData" -> BSONInteger(environment.nbDayKeepContentData),
        "nbDayKeepAllData" -> BSONInteger(environment.nbDayKeepAllData),
        "recordContentData" -> BSONBoolean(environment.recordContentData),
        "recordData" -> BSONBoolean(environment.recordData),
        "groups" -> environment.groups)
  }

  private val keyCacheAllOptions = "environment-options"
  private val keyCacheByName = "environment-name-"
  private val ENVIRONMENT_NAME_PATTERN = "[a-zA-Z0-9]{1,200}"

  /**
   * Sort the given env option seq
   */
  private def sortEnvs(envs: Seq[(String, String)]): Seq[(String, String)] = {
    val sortedEnvs = envs.sortWith {
      (a, b) =>
        val pattern = """^(.+?)([0-9]+)$""".r

        val matchA = pattern.findAllIn(a._2)
        val matchB = pattern.findAllIn(b._2)

        if (matchA.hasNext && matchB.hasNext) {
          // both names match the regex: compare name then number
          val nameA = matchA.group(1)
          val numberA = matchA.group(2)
          val nameB = matchB.group(1)
          val numberB = matchB.group(2)
          if (nameA != nameB) {
            nameA < nameB
          } else {
            numberA.toInt <= numberB.toInt
          }

        } else if (matchA.hasNext) {
          val nameA = matchA.group(1)
          // only a matches the regex
          nameA < b._2

        } else if (matchB.hasNext) {
          val nameB = matchB.group(1)
          // only b matches the regex
          a._2 < nameB

        } else {
          // none matches the regex
          a._2 < b._2
        }
    }

    sortedEnvs
  }

  /**
   * Retrieve an Environment from id.
   */
  def findById(objectId: BSONObjectID): Future[Option[Environment]] = {
    val query = BSONDocument("_id" -> objectId)
    collection.find(query).one[Environment]
  }

  /**
   * Retrieve an Environment from name.
   */
  def findByName(name: String): Future[Option[Environment]] = {
    Cache.getOrElse(keyCacheByName + name) {
      val query = BSONDocument("name" -> name)
      collection.find(query).one[Environment]
    }
  }

  /**
   * Insert a new environment.
   *
   * @param environment The environment values.
   */
  def insert(environment: Environment) = {
    if (!environment.name.trim.matches(ENVIRONMENT_NAME_PATTERN)) {
      throw new Exception("Environment name invalid:" + environment.name.trim)
    }
    if (options.exists {
      e => e._2.equals(environment.name.trim)
    }) {
      throw new Exception("Environment with name " + environment.name.trim + " already exist")
    }
    clearCache
    collection.insert(environment)
  }

  /**
   * Update a environment.
   *
   * @param environment The environment values.
   */
  def update(environment: Environment) = {
    if (!environment.name.trim.matches(ENVIRONMENT_NAME_PATTERN)) {
      throw new Exception("Environment name invalid:" + environment.name.trim)
    }

    if (options.exists {
      e => e._2.equals(environment.name.trim) && e._1 != environment._id.get.stringify
    }) {
      throw new Exception("Environment with name " + environment.name.trim + " already exist")
    }
    val selector = BSONDocument("_id" -> environment._id)

    val modifier = BSONDocument(
      "$set" -> BSONDocument(
        "name" -> environment.name,
        "hourRecordContentDataMin" -> environment.hourRecordContentDataMin,
        "hourRecordContentDataMax" -> environment.hourRecordContentDataMax,
        "nbDayKeepContentData" -> environment.nbDayKeepContentData,
        "nbDayKeepAllData" -> environment.nbDayKeepAllData,
        "recordContentData" -> environment.recordContentData,
        "recordData" -> environment.recordData,
        "groups" -> environment.groups)
    )
    clearCache
    collection.update(selector, modifier)
  }

  /**
   * Delete a environment.
   *
   * @param id Id of the environment to delete.
   */
  def delete(id: String) = {
    val objectId = BSONObjectID.apply(id)
    clearCache()
    collection.remove(BSONDocument("_id" -> objectId))
  }

  def clearCache() {
    this.options.map(e => Cache.remove(keyCacheByName + e._2))
    Cache.remove(keyCacheAllOptions)
  }

  /**
   * Return a list of all environments.
   */
  def findAll: Future[List[Environment]] = {
    collection.
      find(BSONDocument()).
      sort(BSONDocument("name" -> 1)).
      cursor[Environment].
      collect[List]()
  }

  /**
   * Return a list of all environments in some groups.
   */
  def findInGroups(groups: String): Future[List[Environment]] = {
    if ("all".equals(groups)) {
      return findAll
    }
    val find = BSONDocument("groups" -> BSONDocument("$in" -> groups.split(',')))
    collection.
      find(find).
      sort(BSONDocument("name" -> 1)).
      cursor[Environment].
      collect[List]()
  }

  /**
   * Construct the Map[String,String] needed to fill a select options set.
   */
  def options = {
    Cache.getOrElse(keyCacheAllOptions) {
      val f = findAll.map(environments => environments.map(e => (e._id.get.stringify, e.name)))
      sortEnvs(Await result(f, 5.seconds))
    }
  }

  /**
   * Construct the Map[String,String] needed to fill a select options set for selected groups.
   */
  def optionsInGroups(groups: String) = {
    if ("all".equals(groups)) {
      options
    } else {
      val f = findInGroups(groups).map(environments => environments.map(e => (e._id.get.stringify, e.name)))
      sortEnvs(Await result(f, 5.seconds))
    }
  }

  /**
   * Find all distinct groups in environments collections.
   *
   * @return all distinct groups
   */
  def findAllGroups(): Future[BSONDocument] = {
    val command = RawCommand(BSONDocument("distinct" -> "environments", "key" -> "groups"))
    collection.db.command(command) // result is Future[BSONDocument]
  }

  /**
   * Find an environment using his name and retrieve it if the groups in parameters match the environment groups
   * @param name name of environment
   * @param groups groups, separated by ',', example group1,group2...
   * @return
   */
  def findByNameAndGroups(name: String, groups: String): Future[Option[Environment]] = {
    val find = BSONDocument("name" -> name, "groups" -> BSONDocument("$in" -> groups.split(',')))
    collection.find(find).one[Environment]
  }

  /**
   * Foreach environment, retrieve his name and his groups
   * @return
   */
  def findNamesAndGroups(): List[(String, List[String])] = {
    val query = collection.find(BSONDocument()).cursor[Environment].collect[List]().map {
      list => list.map { envir => (envir.name, envir.groups)}
    }
    Await.result(query, 1.second)
  }

  import ModePurge._

  def purgeContentData() {
    purgeData(ModePurge.CONTENT)
  }

  def purgeAllData() {
    purgeData(ModePurge.ALL)
  }

  private def purgeData(mode: ModePurge) {

    Logger.info("Purging " + mode + " data...")

    val minDate = UtilDate.getDate("all").getTime
    var purgedRequests = 0

    val gcal = new GregorianCalendar
    val today = new GregorianCalendar(gcal.get(Calendar.YEAR), gcal.get(Calendar.MONTH), gcal.get(Calendar.DATE))

    Environment.findAll.map(environments => environments.map(
      env => {
        var nbDay = 100
        val maxDate = new GregorianCalendar
        if (mode == ModePurge.CONTENT)
          nbDay = env.nbDayKeepContentData
        else
          nbDay = env.nbDayKeepAllData

        maxDate.setTimeInMillis(today.getTimeInMillis - UtilDate.v1d * nbDay)
        Logger.debug("Purge env: " + env.name + " NbDaysKeep: " + nbDay + " MinDate:" + minDate + " MaxDate:" + maxDate.getTime)
        val user = "Soapower Akka Scheduler (keep " + mode + " data for " + nbDay + " days for this env " + env.name + ")"
        if (mode == ModePurge.CONTENT)
          purgedRequests += RequestData.deleteRequestResponse(env.name, minDate, maxDate.getTime, user)
        else
          purgedRequests += RequestData.delete(env.name, minDate, maxDate.getTime)
      }
    ))
    Logger.info("Purging " + mode + " data: done (" + purgedRequests + " requests purged)")
  }

}
