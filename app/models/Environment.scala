package models

import play.api.db._
import play.api.Play.current
import play.api.cache._
import play.api._

import anorm._
import anorm.SqlParser._
import java.util.{Date, Calendar, GregorianCalendar}

case class Environment(id: Long,
                       name: String,
                       groupId: Long = 1, // 1 is default group
                       hourRecordContentDataMin: Int = 8,
                       hourRecordContentDataMax: Int = 22,
                       nbDayKeepContentData: Int = 2,
                       nbDayKeepAllData: Int = 5,
                       recordContentData: Boolean = true,
                       recordData: Boolean = true
                        )

object ModePurge extends Enumeration {
  type ModePurge = Value
  val XML, ALL = Value
}

object Environment {

  private val keyCacheAllOptions = "environment-options"
  private val keyCacheById = "environment-all"
  private val ENVIRONMENT_NAME_PATTERN = "[a-zA-Z0-9]{1,200}"

  /**
   * Parse a Environment from a ResultSet
   */
  val simple = {
    get[Long]("environment.id") ~
      get[String]("environment.name") ~
      get[Long]("environment.groupId") ~
      get[Int]("environment.hourRecordContentDataMin") ~
      get[Int]("environment.hourRecordContentDataMax") ~
      get[Int]("environment.nbDayKeepContentData") ~
      get[Int]("environment.nbDayKeepAllData") ~
      get[String]("environment.recordContentData") ~
      get[String]("environment.recordData") map {
      case id ~ name ~ groupId ~ hourRecordContentDataMin ~ hourRecordContentDataMax ~ nbDayKeepContentData ~ nbDayKeepAllData ~ recordContentData ~ recordData
      => Environment(id, name, groupId, hourRecordContentDataMin, hourRecordContentDataMax, nbDayKeepContentData, nbDayKeepAllData, (recordContentData == "true"), (recordData == "true"))
    }
  }

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "name" -> 2, "groupName" -> 3, "hourRecordContentDataMin" -> 4, "hourRecordContentDataMax" -> 5, "nbDayKeepContentData" -> 6, "nbDayKeepAllData" -> 7, "recordContentData" -> 8, "recordData" -> 9)

  val csvKey = "environment"

  /**
   * Csv format.
   */
  val csv = {
    get[Pk[Long]]("environment.id") ~
      get[String]("environment.name") ~
      get[String]("groups.name") ~
      get[Int]("environment.hourRecordContentDataMin") ~
      get[Int]("environment.hourRecordContentDataMax") ~
      get[Int]("environment.nbDayKeepContentData") ~
      get[Int]("environment.nbDayKeepAllData") ~
      get[String]("environment.recordContentData") ~
      get[String]("environment.recordData") map {
      case id ~ name ~ groupName ~ hourRecordContentDataMin ~ hourRecordContentDataMax ~ nbDayKeepContentData ~ nbDayKeepAllData ~ recordContentData ~ recordData =>
        id + ";" + name + ";" + groupName + ";" + hourRecordContentDataMin + ";" + hourRecordContentDataMax + ";" + nbDayKeepContentData + ";" + nbDayKeepAllData + ";" + recordContentData + ";" + recordData + "\n"
    }
  }

  /**
   * Get All environments, csv format.
   * @return List of Environements, csv format
   */
  def fetchCsv(): List[String] = DB.withConnection {
    implicit c => SQL("select * from environment left join groups on environment.groupId = groups.id").as(Environment.csv *)
  }


  /**
   * Construct the Map[String,String] needed to fill a select options set. Only environments which are into the given group name are retrieved
   */
  def options(group: String): Seq[(String, String)] = DB.withConnection {
    implicit connection =>
      val envs = Cache.getOrElse[Seq[(String, String)]](keyCacheAllOptions) {
        Logger.debug("Environments not found in cache: loading from db")
        SQL("select * from environment, groups where environment.groupId = groups.id and groups.name = {groupName} order by environment.name").on(
          'groupName -> group).as(Environment.simple *).map(c => c.id.toString -> c.name)
      }
      sortEnvs(envs)
  }


  /**
   * Construct the Map[String,String] needed to fill a select options set.
   */
  def optionsAll: Seq[(String, String)] = DB.withConnection {
    implicit connection =>
      val envs = Cache.getOrElse[Seq[(String, String)]](keyCacheAllOptions) {
        Logger.debug("Environments not found in cache: loading from db")
        SQL("select * from environment order by name").as(Environment.simple *).map(c => c.id.toString -> c.name)
      }
      envs
  }


  /**
   * Construct the Map[String,String] which are contained to the given group, needed to fill a select options set
   */
  def optionsAll(group: String): Seq[(String, String)] = DB.withConnection {
    implicit connection =>
      val envs = Cache.getOrElse[Seq[(String, String)]](keyCacheAllOptions + group) {
        Logger.debug("Environments not found in cache: loading from db")
        SQL("select * from environment, groups where environment.groupId = groups.id and groups.name = {group} order by environment.name ")
          .on('group -> group)
          .as(Environment.simple *)
          .map(c => c.id.toString -> c.name)
      }
      envs
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
  def findById(id: Long): Option[Environment] = {
    DB.withConnection {
      implicit connection =>
        Cache.getOrElse[Option[Environment]](keyCacheById + id) {
          SQL("select * from environment where id = {id}").on(
            'id -> id).as(Environment.simple.singleOpt)
        }
    }
  }

  /**
   * Retrieve an Environment from name.
   */
  def findByName(name: String): Option[Environment] = DB.withConnection {
      implicit connection =>
      // FIXME : add key to clearCache
      //Cache.getOrElse[Option[Environment]](keyCacheByName + name) {
        SQL("select * from environment where name = {name}").on(
          'name -> name).as(Environment.simple.singleOpt)
      //}
    }


  /**
   * Retrieve an Environment from name.
   */
  def findByGroupAndByName(group :String, name: String): Option[Environment] = DB.withConnection {
    implicit connection =>
    // FIXME : add key to clearCache
    //Cache.getOrElse[Option[Environment]](keyCacheByName + name) {
      SQL("select * from environment e, groups g where e.groupId = g.id and e.name = {name} and g.name = {group}").on(
        'name -> name, 'group -> group).as(Environment.simple.singleOpt)
    //}
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

    if (optionsAll.exists{e => e._2.equals(environment.name.trim)}) {
      throw new Exception("Environment with name " + environment.name.trim + " already exist")
    }

    DB.withConnection {
      implicit connection =>
        SQL(
          """
            insert into environment (
              id, name, hourRecordContentDataMin, hourRecordContentDataMax, nbDayKeepContentData,
              nbDayKeepAllData, recordContentData, recordData, groupId)
             values (
              null, {name}, {hourRecordContentDataMin}, {hourRecordContentDataMax}, {nbDayKeepContentData},
              {nbDayKeepAllData}, {recordContentData}, {recordData}, {groupId}
            )
          """).on(
          'name -> environment.name.trim,
          'hourRecordContentDataMin -> environment.hourRecordContentDataMin,
          'hourRecordContentDataMax -> environment.hourRecordContentDataMax,
          'nbDayKeepContentData -> environment.nbDayKeepContentData,
          'nbDayKeepAllData -> environment.nbDayKeepAllData,
          'recordContentData -> environment.recordContentData.toString,
          'recordData -> environment.recordData.toString,
          'groupId -> environment.groupId
        ).executeUpdate()
    }
    clearCache
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

    if (optionsAll.exists{e => e._2.equals(environment.name.trim) && e._1.toLong != environment.id}) {
      throw new Exception("Environment with name " + environment.name.trim + " already exist")
    }

    Cache.remove(keyCacheById + environment.id)
    DB.withConnection {
      implicit connection =>
        SQL(
          """
          update environment
          set name = {name},
          hourRecordContentDataMin = {hourRecordContentDataMin},
          hourRecordContentDataMax = {hourRecordContentDataMax},
          nbDayKeepContentData = {nbDayKeepContentData},
          nbDayKeepAllData = {nbDayKeepAllData},
          recordContentData = {recordContentData},
          recordData = {recordData},
          groupId = {groupId}
          where id = {id}
          """).on(
          'id -> environment.id,
          'name -> environment.name.trim,
          'hourRecordContentDataMin -> environment.hourRecordContentDataMin,
          'hourRecordContentDataMax -> environment.hourRecordContentDataMax,
          'nbDayKeepContentData -> environment.nbDayKeepContentData,
          'nbDayKeepAllData -> environment.nbDayKeepAllData,
          'recordContentData -> environment.recordContentData.toString,
          'recordData -> environment.recordData.toString,
          'groupId -> environment.groupId
        ).executeUpdate()
    }
    clearCache
  }

  /**
   * Delete a environment.
   *
   * @param id Id of the environment to delete.
   */
  def delete(id: Long) = {
    Cache.remove(keyCacheById + id)
    DB.withConnection {
      implicit connection =>
        SQL("delete from environment where id = {id}").on('id -> id).executeUpdate()
    }
    clearCache
  }

  def clearCache() {
    Cache.remove(keyCacheAllOptions)
  }

  /**
   * Return a list of all Environment which are contained into the given group
   *
   */
  def list(group: String): List[Environment] = {

    DB.withConnection {
      implicit connection =>

        val environments = SQL(
          """
          select * from environment, groups
          where environment.groupId = groups.id
          and groups.name = {group}
          order by environment.groupId asc, environment.name
          """).on('group -> group).as(Environment.simple *)

        environments
    }
  }

  /**
   * Return a list of all Environment
   *
   */
  def list(): List[Environment] = {

    DB.withConnection {
      implicit connection =>

        val environments = SQL(
          """
          select * from environment
          order by environment.name
          """).as(Environment.simple *)

        environments
    }
  }

  /*
   * Compile stats for each env / day
   */
  def compileStats() {
    Logger.info("Compile Stats")
    val gcal = new GregorianCalendar

    Environment.optionsAll.foreach {
      (e) =>
        Logger.debug("Compile Stats env:" + e._2)
        val days = RequestData.findDayNotCompileStats(e._1.toLong)

        days.foreach {
          minDate =>
            Logger.debug("Compile Stats minDate:" + minDate + " env:" + e._2)
            gcal.setTimeInMillis(minDate.getTime + UtilDate.v1d)
            val maxDate = gcal.getTime

            val result = RequestData.loadAvgResponseTimesByAction("all", e._1, "200", minDate, maxDate, false)
            result.foreach {
              (r) =>
                Logger.debug("call insertStats env:" + e._2 + " ServiceAction:" + r._1 + " timeAverage:" + r._2 + " date:" + minDate)
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

    Environment.optionsAll.foreach {
      (e) =>
        val env = Environment.findById(e._1.toInt).get
        var nbDay = 100

        val maxDate = new GregorianCalendar
        if (mode == ModePurge.XML)
          nbDay = env.nbDayKeepContentData
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
      val group = Group.upload(dataCsv(csvTitle.get("groupName").get))
      uploadEnvironment(dataCsv, group)
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
  private def uploadEnvironment(dataCsv: Array[String], group: Group) = {

    val name = dataCsv(csvTitle.get("name").get)
    val s = findByName(name)

    s.map {
      environment =>
        Logger.warn("Warning : Environment " + environment.name + " already exist")
        throw new Exception("Warning : Environment " + environment.name + " already exist")
    }.getOrElse {

      val environment = new Environment(
        -1,
        dataCsv(csvTitle.get("name").get).trim,
        group.id,
        dataCsv(csvTitle.get("hourRecordContentDataMin").get).toInt,
        dataCsv(csvTitle.get("hourRecordContentDataMax").get).toInt,
        dataCsv(csvTitle.get("nbDayKeepContentData").get).toInt,
        dataCsv(csvTitle.get("nbDayKeepAllData").get).toInt,
        (dataCsv(csvTitle.get("recordContentData").get).trim == "true"),
        (dataCsv(csvTitle.get("recordData").get).trim == "true")
      )
      Environment.insert(environment)
      Logger.info("Insert Environment " + environment.name)
    }
  }
}
