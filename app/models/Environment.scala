package models

import play.api.db._
import play.api.Play.current
import play.api.cache._
import play.api._

import anorm._
import anorm.SqlParser._
import java.util.{Date, Calendar, GregorianCalendar}

case class Environment(id: Pk[Long],
  name: String,
  hourRecordXmlDataMin: Int ,
  hourRecordXmlDataMax: Int ,
  nbDayKeepXmlData: Int ,
  nbDayKeepAllData: Int ,
  recordXmlData: Boolean,
  recordData: Boolean,
  groupId: Long)

object ModePurge extends Enumeration {
  type ModePurge = Value
  val XML, ALL = Value
}

object Environment {

  private val keyCacheAllOptions = "environment-options"
  private val keyCacheById = "environment-all"

  /**
   * Create an environment with the classical default values and the given name and group id.
   */
  def createEnvironmentWithDefaultValues(name: String, groupId: Long): Environment = {
    return new Environment(NotAssigned, name, 8, 22, 2, 5, true, true, groupId)
  }
    
  /**
   * Parse a Environment from a ResultSet
   */
  val simple = {
    get[Pk[Long]]("id") ~
    get[String]("name") ~
    get[Int]("hourRecordXmlDataMin") ~
    get[Int]("hourRecordXmlDataMax") ~
    get[Int]("nbDayKeepXmlData") ~
    get[Int]("nbDayKeepAllData") ~
    get[String]("recordXmlData") ~
    get[String]("recordData") ~ 
    get[Long]("groupId")  map {
        case id ~ name ~ hourRecordXmlDataMin ~ hourRecordXmlDataMax ~ nbDayKeepXmlData ~ nbDayKeepAllData ~ recordXmlData ~ recordData ~ groupId
          => Environment(id, name, hourRecordXmlDataMin, hourRecordXmlDataMax, nbDayKeepXmlData, nbDayKeepAllData, (recordXmlData == "true"), (recordData == "true"), groupId)
      }
  }

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "name" -> 2, "hourRecordXmlDataMin" -> 3, "hourRecordXmlDataMax" -> 4, "nbDayKeepXmlData" -> 5, "nbDayKeepAllData" -> 6, "recordXmlData" -> 7, "recordData" -> 8, "groupName" ->9 )

  val csvKey = "environment";

  /**
   * Csv format.
   */
  val csv = {
    get[Pk[Long]]("environment.id") ~
    get[String]("environment.name") ~
    get[Int]("environment.hourRecordXmlDataMin") ~
    get[Int]("environment.hourRecordXmlDataMax") ~
    get[Int]("environment.nbDayKeepXmlData") ~
    get[Int]("environment.nbDayKeepAllData") ~
    get[String]("environment.recordXmlData") ~
    get[String]("environment.recordData") ~
    get[String] ("environment_group.groupName") map {
      case id ~ name ~ hourRecordXmlDataMin ~ hourRecordXmlDataMax ~ nbDayKeepXmlData ~ nbDayKeepAllData ~ recordXmlData ~ recordData ~ groupName=>
        id + ";" + name + ";" + hourRecordXmlDataMin + ";" + hourRecordXmlDataMax + ";" + nbDayKeepXmlData + ";" + nbDayKeepAllData + ";" + recordXmlData + ";" + recordData + ";" + groupName + "\n"
    }
  }


  /**
   * Get All environements, csv format.
   * @return List of Environements, csv format
   */
  def fetchCsv(): List[String] = DB.withConnection {
    implicit c => SQL("select * from environment left join environment_group on environment.groupId = environment_group.groupId").as(Environment.csv *)
  }

  /**
   * Construct the Map[String,String] needed to fill a select options set.
   */
  def options: Seq[(String, String)] = DB.withConnection {
    implicit connection =>
      val envs = Cache.getOrElse[Seq[(String, String)]](keyCacheAllOptions) {
        Logger.debug("Environments not found in cache: loading from db")
        SQL("select * from environment order by name").as(Environment.simple *).map(c => c.id.toString -> c.name)
      }

      val sortedEnvs = envs.sortWith { (a, b) =>
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
   * Insert a new environment.
   *
   * @param environment The environment values.
   */
  def insert(environment: Environment) = {
    clearCache
    DB.withConnection {
      implicit connection =>
        SQL(
          """
            insert into environment values (
              null, {name}, {hourRecordXmlDataMin},
        		{hourRecordXmlDataMax}, {nbDayKeepXmlData}, {nbDayKeepAllData}, {recordXmlData}, {recordData},
        		{groupId}
            )
          """).on(
            'name -> environment.name,
            'hourRecordXmlDataMin -> environment.hourRecordXmlDataMin,
            'hourRecordXmlDataMax -> environment.hourRecordXmlDataMax,
            'nbDayKeepXmlData -> environment.nbDayKeepXmlData,
            'nbDayKeepAllData -> environment.nbDayKeepAllData,
            'recordXmlData -> environment.recordXmlData.toString,
            'recordData -> environment.recordData.toString,
            'groupId -> environment.groupId
            ).executeUpdate()
    }
  }

  /**
   * Update a environment.
   *
   * @param id The environment id
   * @param environment The environment values.
   */
  def update(id: Long, environment: Environment) = {
    clearCache
    Cache.remove(keyCacheById + id)
    DB.withConnection {
      implicit connection =>
        SQL(
          """
          update environment
          set name = {name},
          hourRecordXmlDataMin = {hourRecordXmlDataMin},
          hourRecordXmlDataMax = {hourRecordXmlDataMax},
          nbDayKeepXmlData = {nbDayKeepXmlData},
          nbDayKeepAllData = {nbDayKeepAllData},
          recordXmlData = {recordXmlData},
          recordData = {recordData},
          groupId = {groupId}
          where id = {id}
          """).on(
            'id -> id,
            'name -> environment.name,
            'hourRecordXmlDataMin -> environment.hourRecordXmlDataMin,
            'hourRecordXmlDataMax -> environment.hourRecordXmlDataMax,
            'nbDayKeepXmlData -> environment.nbDayKeepXmlData,
            'nbDayKeepAllData -> environment.nbDayKeepAllData,
            'recordXmlData -> environment.recordXmlData.toString,
            'recordData -> environment.recordData.toString,
            'groupId -> environment.groupId
          ).executeUpdate()
    }
  }

  /**
   * Delete a environment.
   *
   * @param id Id of the environment to delete.
   */
  def delete(id: Long) = {
    clearCache
    Cache.remove(keyCacheById + id)
    DB.withConnection {
      implicit connection =>
        SQL("delete from environment where id = {id}").on('id -> id).executeUpdate()
    }
  }

  def clearCache() {
    Cache.remove(keyCacheAllOptions)
  }
  
  
    /**
   * Parse a (Environment,Group) from a ResultSet
   */
  val withGroup = Environment.simple ~ Group.simple map {
    case environment ~ group => (environment, group)
  }


  /**
   * Return a list of (Environment, Group).
   *
   */
  def list: List[(Environment, Group)] = {

    DB.withConnection {
      implicit connection =>

        val environments = SQL(
          """
          select * from environment
          left join environment_group on environment.groupId = environment_group.groupId
          order by environment.groupId asc, environment.name
          """).as(Environment.withGroup *)

        environments
    }
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
        val days = RequestData.findDayNotCompileStats(e._1.toLong)

        days.foreach{minDate =>
          gcal.setTimeInMillis(minDate.getTime + UtilDate.v1d)
          val maxDate = gcal.getTime

          val result = RequestData.loadAvgResponseTimesByAction(e._1, minDate, maxDate, false)
          result.foreach{(r) =>
            Logger.debug("env:" + e._2 + " SoapAction:"+r._1+ " timeAverage:" + r._2)
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
    val today = new GregorianCalendar(gcal.get(Calendar.YEAR),gcal.get(Calendar.MONTH),gcal.get(Calendar.DATE))

    Environment.options.foreach {
      (e) =>
        val env = Environment.findById(e._1.toInt).get
        var nbDay = 100

        val maxDate = new GregorianCalendar
        if (mode == ModePurge.XML)
          nbDay = env.nbDayKeepXmlData
        else
          nbDay = env.nbDayKeepAllData

        maxDate.setTimeInMillis(today.getTimeInMillis - UtilDate.v1d * nbDay)
        Logger.debug("Purge env: " + env.name + " NbDaysKeep: " + nbDay + " MinDate:" + minDate + " MaxDate:" + maxDate.getTime)
        val user = "Soapower Akka Scheduler (keep "+mode+" data for " + nbDay + " days for this env " + env.name + ")"
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

    if (dataCsv.size != csvTitle.size){
      throw new Exception("Please check csvFile, " + csvTitle.size + " fields required")
    }

    if (dataCsv(csvTitle.get("key").get) == csvKey) {
      val group = uploadGroup(dataCsv)
      uploadEnvironment(dataCsv, group)
    } else {
      Logger.info("Line does not match with " + csvKey + " of csvLine - ignored")
    }
  }

    /**
   * Check if group exist and insert it if not
   *
   * @param dataCsv List of string
   * @return group
   */
  private def uploadGroup(dataCsv: Array[String]): Group = {
    val groupName = dataCsv(csvTitle.get("groupName").get)
    Logger.debug("groupName:" + groupName)

    var group = Group.findByName(groupName)
    if (group == None) {
      Logger.debug("Insert Environment " + groupName)
      Group.insert(new Group(NotAssigned, groupName))
      group = Group.findByName(groupName)
      if (group.get == null) Logger.error("Group insert failed : " + groupName)
    } else {
      Logger.debug("Group already exist : " + group.get.groupName)
    }
    group.get
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
        NotAssigned,
        dataCsv(csvTitle.get("name").get).trim,
        dataCsv(csvTitle.get("hourRecordXmlDataMin").get).toInt,
        dataCsv(csvTitle.get("hourRecordXmlDataMax").get).toInt,
        dataCsv(csvTitle.get("nbDayKeepXmlData").get).toInt,
        dataCsv(csvTitle.get("nbDayKeepAllData").get).toInt,
        (dataCsv(csvTitle.get("recordXmlData").get).trim == "true"),
        (dataCsv(csvTitle.get("recordData").get).trim == "true"),
        group.groupId.get
      )
      Environment.insert(environment)
      Logger.info("Insert Environment " + environment.name)
    }
  }
}
