package models

import play.api.Play.current
import play.api.cache._

import java.util.{Calendar, GregorianCalendar}
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.libs.json._
import reactivemongo.bson._
import scala.concurrent.{Await, Future}
import play.modules.reactivemongo.json.BSONFormats._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.core.commands.RawCommand
import play.api.Logger
import reactivemongo.api.collections.default.BSONCollection

case class Environment(_id: Option[BSONObjectID],
                       name: String,
                       groups: List[String],
                       hourRecordXmlDataMin: Int = 8,
                       hourRecordXmlDataMax: Int = 22,
                       nbDayKeepXmlData: Int = 2,
                       nbDayKeepAllData: Int = 5,
                       recordXmlData: Boolean = true,
                       recordData: Boolean = true)

object ModePurge extends Enumeration {
  type ModePurge = Value
  val XML, ALL = Value
}

object Environment {

  /*
   * Collection MongoDB
   */
  def collection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("environments")

  implicit val environmentFormat = Json.format[Environment]

  implicit object EnvironmentBSONReader extends BSONDocumentReader[Environment] {
    def read(doc: BSONDocument): Environment = {
      Logger.debug("Doc : " + BSONDocument.pretty(doc))
      Environment(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[String]("name").get,
        doc.getAs[List[String]]("groups").toList.flatten,
        doc.getAs[Int]("hourRecordXmlDataMin").get,
        doc.getAs[Int]("hourRecordXmlDataMax").get,
        doc.getAs[Int]("nbDayKeepXmlData").get,
        doc.getAs[Int]("nbDayKeepAllData").get,
        doc.getAs[Boolean]("recordXmlData").get,
        doc.getAs[Boolean]("recordData").get
      )
    }
  }

  implicit object EnvironmentBSONWriter extends BSONDocumentWriter[Environment] {
    def write(environment: Environment): BSONDocument =
      BSONDocument(
        "_id" -> environment._id,
        "name" -> BSONString(environment.name),
        "hourRecordXmlDataMin" -> BSONInteger(environment.hourRecordXmlDataMin),
        "hourRecordXmlDataMax" -> BSONInteger(environment.hourRecordXmlDataMax),
        "nbDayKeepXmlData" -> BSONInteger(environment.nbDayKeepXmlData),
        "nbDayKeepAllData" -> BSONInteger(environment.nbDayKeepAllData),
        "recordXmlData" -> BSONBoolean(environment.recordXmlData),
        "recordData" -> BSONBoolean(environment.recordData),
        "groups" -> environment.groups)
  }

  private val keyCacheAllOptions = "environment-options"
  private val ENVIRONMENT_NAME_PATTERN = "[a-zA-Z0-9]{1,200}"

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "name" -> 2, "groups" -> 3, "hourRecordXmlDataMin" -> 4, "hourRecordXmlDataMax" -> 5, "nbDayKeepXmlData" -> 6, "nbDayKeepAllData" -> 7, "recordXmlData" -> 8, "recordData" -> 9)

  val csvKey = "environment"

  /**
   * Csv format.
   */
  def csv(e: Environment) = {
    csvKey + ";" + e._id.get.stringify + ";" + e.name + ";" + e.groups.mkString("|") + ";" + e.hourRecordXmlDataMin + ";" + e.hourRecordXmlDataMax + ";" + e.nbDayKeepXmlData + ";" + e.nbDayKeepAllData + ";" + e.recordXmlData + ";" + e.recordData + "\n"
  }

  /**
   * Get All environments, csv format.
   * @return List of Environements, csv format
   */
  def fetchCsv(): Future[List[String]] = {
    findAll.map(environment => environment.map(e => csv(e)))
  }

  /**
   * Construct the Map[String,String] needed to fill a select options set. Only environments which are into the given group name are retrieved
   */
  def options(group: String): Seq[(String, String)] = {
    ???
    /*implicit connection =>
      val envs = Cache.getOrElse[Seq[(String, String)]](keyCacheAllOptions) {
        Logger.debug("Environments not found in cache: loading from db")
        SQL("select * from environment, groups where environment.groupId = groups.id and groups.name = {groupName} order by environment.name").on(
          'groupName -> group).as(Environment.simple *).map(c => c.id.toString -> c.name)
      }
      sortEnvs(envs)
    */
  }


  /**
   * Construct the Map[String,String] needed to fill a select options set.
   */
  def options = {
    Cache.getOrElse(keyCacheAllOptions) {
      val f = findAll.map(environments => environments.map(e => (e._id.get.stringify, e.name)))
      //TODO sortEnvs(Await result(f, 1.seconds))
      Await result(f, 1.seconds)
    }
  }

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
    val query = BSONDocument("name" -> name)
    collection.find(query).one[Environment]
  }

  /**
   * Retrieve an Environment from name.
   */
  def findByGroupAndByName(group: String, name: String): Option[Environment] = {
    ???
    /*implicit connection =>
    //Cache.getOrElse[Option[Environment]](keyCacheByName + name) {
      SQL("select * from environment e, groups g where e.groupId = g.id and e.name = {name} and g.name = {group}").on(
        'name -> name, 'group -> group).as(Environment.simple.singleOpt)
    //}
    */
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
        "hourRecordXmlDataMin" -> environment.hourRecordXmlDataMin,
        "hourRecordXmlDataMax" -> environment.hourRecordXmlDataMax,
        "nbDayKeepXmlData" -> environment.nbDayKeepXmlData,
        "nbDayKeepAllData" -> environment.nbDayKeepAllData,
        "recordXmlData" -> environment.recordXmlData,
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
    val objectId = new BSONObjectID(id)
    collection.remove(BSONDocument("_id" -> objectId))
  }

  def clearCache() {
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
   * Return a list of all Environment which are contained into the given group
   *
   */
  def list(group: String): List[Environment] = {
    ???
  }

  def findAllGroups(): Future[BSONDocument] = {
    val command = RawCommand(BSONDocument("distinct" -> "environments", "key" -> "groups"))
    collection.db.command(command) // result is Future[BSONDocument]
  }

  /*
   * Compile stats for each env / day
   */
  def compileStats() {
    Logger.info("Compile Stats")
    val gcal = new GregorianCalendar

    Environment.options.foreach {
      (e) =>
        Logger.debug("Compile Stats env:" + e._2)
        val days = RequestData.findDayNotCompileStats(e._1)

        days.foreach {
          minDate =>
            Logger.debug("Compile Stats minDate:" + minDate + " env:" + e._2)
            gcal.setTimeInMillis(minDate.getTime + UtilDate.v1d)
            val maxDate = gcal.getTime

            val result = RequestData.loadAvgResponseTimesByAction("all", e._1, "200", minDate, maxDate, false)
            result.foreach {
              (r) =>
                Logger.debug("call insertStats env:" + e._2 + " SoapAction:" + r._1 + " timeAverage:" + r._2 + " date:" + minDate)
                RequestData.insertStats(e._1.toLong, r._1, minDate, r._2)
            }
        }
    }
  }

  import ModePurge._

  def purgeXmlData() {
    purgeData(ModePurge.XML)
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
        if (mode == ModePurge.XML)
          nbDay = env.nbDayKeepXmlData
        else
          nbDay = env.nbDayKeepAllData

        maxDate.setTimeInMillis(today.getTimeInMillis - UtilDate.v1d * nbDay)
        Logger.debug("Purge env: " + env.name + " NbDaysKeep: " + nbDay + " MinDate:" + minDate + " MaxDate:" + maxDate.getTime)
        val user = "Soapower Akka Scheduler (keep " + mode + " data for " + nbDay + " days for this env " + env.name + ")"
        if (mode == ModePurge.XML)
          purgedRequests += RequestData.deleteRequestResponse(env.name, minDate, maxDate.getTime, user)
        else
          purgedRequests += RequestData.delete(env.name, minDate, maxDate.getTime)
      }
    ))
    Logger.info("Purging " + mode + " data: done (" + purgedRequests + " requests purged)")
  }


  /**
   * Upload a csvLine => insert environment.
   *
   * @param csvLine line in csv file
   * @return nothing
   */
  def upload(csvLine: String) = {

    val dataCsv = csvLine.split(";")

    if (dataCsv.size != csvTitle.size) {
      throw new Exception("Please check csvFile, " + csvTitle.size + " fields required")
    }

    if (dataCsv(csvTitle.get("key").get) == csvKey) {
      uploadEnvironment(dataCsv)
    } else {
      Logger.info("Line does not match with " + csvKey + " of csvLine - ignored")
    }
  }

  /**
   * Check if environment already exist (with same name). Insert or do nothing if exist.
   *
   * @param dataCsv line in csv file
   * @return environment (new or not)
   */
  private def uploadEnvironment(dataCsv: Array[String]) = {

    val name = dataCsv(csvTitle.get("name").get)
    Logger.debug("upload environment:" + name)

    findByName(name).map {
      environment => {
        if (environment == None) {
          Logger.debug("Insert new environment with name " + name)
          val newEnvironment = new Environment(Some(BSONObjectID.generate),
            dataCsv(csvTitle.get("name").get).trim,
            dataCsv(csvTitle.get("groups").get).split('|').toList, // single quote of split is important
            dataCsv(csvTitle.get("hourRecordXmlDataMin").get).toInt,
            dataCsv(csvTitle.get("hourRecordXmlDataMax").get).toInt,
            dataCsv(csvTitle.get("nbDayKeepXmlData").get).toInt,
            dataCsv(csvTitle.get("nbDayKeepAllData").get).toInt,
            dataCsv(csvTitle.get("recordXmlData").get).trim == "true",
            dataCsv(csvTitle.get("recordData").get).trim == "true"
          )
          insert(newEnvironment).map {
            lastError =>
              if (lastError.ok) {
                Logger.debug("OK Insert new environment with name " + name)
              } else {
                Logger.error("Detected error:%s".format(lastError))
                throw new Exception("Error while inserting new group with name : " + name)
              }
          }
        } else {
          Logger.warn("Warning : Environment " + name + " already exist")
          throw new Exception("Warning : Environment " + name + " already exist")
        }
      }
    }
  }
}
