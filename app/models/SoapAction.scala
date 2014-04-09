package models

import play.api.db._
import play.api.Play.current
import play.api.cache._
import play.api._

import anorm._
import anorm.SqlParser._

case class SoapAction(id: Long, name: String, thresholdms: Long)

object SoapAction {
  // -- Parsers

  /**
   * Parse a SoapAction from a ResultSet
   */
  val simple = {
    get[Long]("soapaction.id") ~
      get[String]("soapaction.name") ~
      get[Long]("soapaction.thresholdms") map {
      case id ~ name ~ thresholdms =>
        SoapAction(id, name, thresholdms)
    }
  }

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "name" -> 2, "thresholdms" -> 3)

  val csvKey = "soapaction"

  /**
   * Csv format.
   */
  val csv = {
    get[Pk[Long]]("soapaction.id") ~
      get[String]("soapaction.name") ~
      get[Long]("soapaction.thresholdms") map {
      case id ~ name ~ thresholdms =>
        id + ";" + name + ";" + thresholdms + "\n"
    }
  }

  /**
   * Get All soapActions, csv format.
   * @return List of SoapActions, csv format
   */
  def fetchCsv(): List[String] = DB.withConnection {
    implicit c => SQL("select * from soapaction").as(SoapAction.csv *)
  }

  /**
   * Retrieve an SoapAction from id.
   */
  def findById(id: Long): Option[SoapAction] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from soapaction where id = {id}").on('id -> id).as(SoapAction.simple.singleOpt)
    }
  }

  /**
   * Retrieve an SoapAction from name.
   */
  def findByName(name: String): Option[SoapAction] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from soapaction where name = {name}").on('name -> name).as(SoapAction.simple.singleOpt)
    }
  }

  /**
   * Insert a new SoapAction.
   *
   * @param soapAction The SoapAction values.
   */
  def insert(soapAction: SoapAction) = {
    DB.withConnection {
      implicit connection =>
        SQL(
          """
            insert into soapaction values (
              null,
              {name},
              {thresholdms}
            )
          """).on(
            'name -> soapAction.name,
            'thresholdms -> soapAction.thresholdms).executeUpdate()
    }
  }

  /**
   * Update a SoapAction : update only threshold
   *
   * @param soapAction The SoapAction values.
   */
  def update(soapAction: SoapAction) = {
    DB.withConnection {
      implicit connection =>
        SQL(
          """
          update soapaction
          set thresholdms = {thresholdms}
          where id = {id}
          """).on(
            'id -> soapAction.id,
            'thresholdms -> soapAction.thresholdms).executeUpdate()
    }
  }

  /**
   * Return a list of (SoapAction).
   */
  def list: List[SoapAction] = {

    DB.withConnection {
      implicit connection =>

        val soapactions = SQL(
          """
          select * from soapaction
          order by name
          """).as(SoapAction.simple *)

        soapactions
    }
  }

  def loadAll(): List[SoapAction] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from soapaction").as(SoapAction.simple *)
    }
  }

  /**
   * Upload a csvLine => insert soapAction.
   *
   * @param csvLine line in csv file
   * @return nothing
   */
  def upload(csvLine: String) = {

    val dataCsv = csvLine.split(";")

    if (dataCsv.size != csvTitle.size)
      throw new Exception("Please check csvFile, " + csvTitle.size + " fields required")

    if (dataCsv(csvTitle.get("key").get) == csvKey) {
      uploadSoapAction(dataCsv)
    } else {
      Logger.info("Line does not match with " + csvKey + " of csvLine - ignored")
    }
  }

  /**
   * Check if soapAction already exist (with same name). Insert or do nothing if exist.
   *
   * @param dataCsv line in csv file
   * @return soapAction (new or not)
   */
  private def uploadSoapAction(dataCsv: Array[String]) = {

    val name = dataCsv(csvTitle.get("name").get)
    val s = findByName(name)

    s.map {
      soapAction =>
        Logger.warn("SoapAction " + soapAction.name + " already exist")
        throw new Exception("Warning : SoapAction " + soapAction.name + " already exist")
    }.getOrElse {

      val soapAction = new SoapAction(
        -1,
        dataCsv(csvTitle.get("name").get).trim,
        dataCsv(csvTitle.get("thresholdms").get).toInt)
      SoapAction.insert(soapAction)
      Logger.info("Insert SoapAction " + soapAction.name)
    }
  }

}