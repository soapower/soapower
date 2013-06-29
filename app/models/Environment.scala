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

  private val keyCacheAllOptions = "environment-options"
  private val keyCacheById = "environment-all"

  /**
   * Parse a Environment from a ResultSet
   */
  val simple = {
    get[Long]("id") ~
    get[String]("name") ~
    get[Int]("hourRecordXmlDataMin") ~
    get[Int]("hourRecordXmlDataMax") ~
    get[Int]("nbDayKeepXmlData") ~
    get[Int]("nbDayKeepAllData") ~
    get[String]("recordXmlData") ~
    get[String]("recordData") map {
        case id ~ name ~ hourRecordXmlDataMin ~ hourRecordXmlDataMax ~ nbDayKeepXmlData ~ nbDayKeepAllData ~ recordXmlData ~ recordData
          => Environment(id, name, hourRecordXmlDataMin, hourRecordXmlDataMax, nbDayKeepXmlData, nbDayKeepAllData, (recordXmlData == "true"), (recordData == "true"))
      }
  }

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "name" -> 2, "hourRecordXmlDataMin" -> 3, "hourRecordXmlDataMax" -> 4, "nbDayKeepXmlData" -> 5, "nbDayKeepAllData" -> 6, "recordXmlData" -> 7, "recordData" -> 8)

  val csvKey = "environment";

  /**
   * Csv format.
   */
  val csv = {
    get[Pk[Long]]("id") ~
    get[String]("name") ~
    get[Int]("hourRecordXmlDataMin") ~
    get[Int]("hourRecordXmlDataMax") ~
    get[Int]("nbDayKeepXmlData") ~
    get[Int]("nbDayKeepAllData") ~
    get[String]("recordXmlData") ~
    get[String]("recordData") map {
      case id ~ name ~ hourRecordXmlDataMin ~ hourRecordXmlDataMax ~ nbDayKeepXmlData ~ nbDayKeepAllData ~ recordXmlData ~ recordData =>
        id + ";" + name + ";" + hourRecordXmlDataMin + ";" + hourRecordXmlDataMax + ";" + nbDayKeepXmlData + ";" + nbDayKeepAllData + ";" + recordXmlData + ";" + recordData + "\n"
    }
  }


  /**
   * Get All environements, csv format.
   * @return List of Environements, csv format
   */
  def fetchCsv(): List[String] = DB.withConnection {
    implicit c => SQL("select * from environment").as(Environment.csv *)
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
              {hourRecordXmlDataMax}, {nbDayKeepXmlData}, {nbDayKeepAllData}, {recordXmlData}, {recordData}
            )
          """).on(
            'name -> environment.name,
            'hourRecordXmlDataMin -> environment.hourRecordXmlDataMin,
            'hourRecordXmlDataMax -> environment.hourRecordXmlDataMax,
            'nbDayKeepXmlData -> environment.nbDayKeepXmlData,
            'nbDayKeepAllData -> environment.nbDayKeepAllData,
            'recordXmlData -> environment.recordXmlData.toString,
            'recordData -> environment.recordData.toString
            ).executeUpdate()
    }
  }

  /**
   * Update a environment.
   *
   * @param environment The environment values.
   */
  def update(environment: Environment) = {
    clearCache
    Cache.remove(keyCacheById + environment.id)
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
          recordData = {recordData}
          where id = {id}
          """).on(
            'id -> environment.id,
            'name -> environment.name,
            'hourRecordXmlDataMin -> environment.hourRecordXmlDataMin,
            'hourRecordXmlDataMax -> environment.hourRecordXmlDataMax,
            'nbDayKeepXmlData -> environment.nbDayKeepXmlData,
            'nbDayKeepAllData -> environment.nbDayKeepAllData,
            'recordXmlData -> environment.recordXmlData.toString,
            'recordData -> environment.recordData.toString
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
   * Return a list of Environment.
   *
   */
  def list: List[Environment] = {

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

    if (dataCsv.size != csvTitle.size)
      throw new Exception("Please check csvFile, " + csvTitle.size + " fields required")

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
    val s = findByName(name)

    s.map {
      environment =>
        Logger.warn("Warning : Environment " + environment.name + " already exist")
        throw new Exception("Warning : Environment " + environment.name + " already exist")
    }.getOrElse {

      val environment = new Environment(
        -1,
        dataCsv(csvTitle.get("name").get).trim,
        dataCsv(csvTitle.get("hourRecordXmlDataMin").get).toInt,
        dataCsv(csvTitle.get("hourRecordXmlDataMax").get).toInt,
        dataCsv(csvTitle.get("nbDayKeepXmlData").get).toInt,
        dataCsv(csvTitle.get("nbDayKeepAllData").get).toInt,
        (dataCsv(csvTitle.get("recordXmlData").get).trim == "true"),
        (dataCsv(csvTitle.get("recordData").get).trim == "true"))
      Environment.insert(environment)
      Logger.info("Insert Environment " + environment.name)
    }
  }
}
