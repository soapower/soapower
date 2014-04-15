package models

import play.api.db._
import play.api.Play.current
import play.api.cache._
import play.api._

import anorm._
import anorm.SqlParser._


case class ServiceAction(id: Long, name: String, thresholdms: Long)

object ServiceAction {
  // -- Parsers

  /**
   * Parse a ServiceAction from a ResultSet
   */
  val simple = {
    get[Long]("serviceaction.id") ~
    get[String]("serviceaction.name") ~
    get[Long]("serviceaction.thresholdms") map {
      case id ~ name ~ thresholdms =>
        ServiceAction(id, name, thresholdms)
    }
  }

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "name" -> 2, "thresholdms" -> 3)

  val csvKey = "serviceaction"

  /**
   * Csv format.
   */
  val csv = {
    get[Pk[Long]]("serviceaction.id") ~
    get[String]("serviceaction.name") ~
    get[Long]("serviceaction.thresholdms") map {
      case id ~ name ~ thresholdms =>
        id + ";" + name + ";" + thresholdms + "\n"
    }
  }

  /**
   * Get All serviceAction, csv format.
   * @return List of serviceAction, csv format
   */
  def fetchCsv(): List[String] = DB.withConnection {
    implicit c => SQL("select * from serviceaction").as(ServiceAction.csv *)
  }

  /**
   * Retrieve a ServiceAction from id.
   */
  def findById(id: Long): Option[ServiceAction] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from serviceaction where id = {id}").on('id -> id).as(ServiceAction.simple.singleOpt)
    }
  }

  /**
   * Retrieve a ServiceAction from name.
   */
  def findByName(name: String): Option[ServiceAction] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from serviceaction where name = {name}").on('name -> name).as(ServiceAction.simple.singleOpt)
    }
  }

	/**
	* Insert a new ServiceAction.
	*
	* @param serviceAction The ServiceAction values.
	*/
  def insert(serviceAction: ServiceAction) = {
    DB.withConnection {
      implicit connection =>
        SQL(
          """
            insert into serviceaction values (
              null,
              {name},
              {thresholdms}
            )
          """).on(
          'name -> serviceAction.name,
          'thresholdms -> serviceAction.thresholdms).executeUpdate()
    }
  }

  /**
   * Update a ServiceAction : update only threshold
   *
   * @param serviceAction The ServiceAction values.
   */
  def update(serviceAction: ServiceAction) = {
    DB.withConnection {
      implicit connection =>
        SQL(
          """
          update serviceaction
          set thresholdms = {thresholdms}
          where id = {id}
          """).on(
          'id -> serviceAction.id,
          'thresholdms -> serviceAction.thresholdms).executeUpdate()
    }
  }

   /**
   * Return a list of (ServiceAction).
   */
  def list: List[ServiceAction] = {

    DB.withConnection {
      implicit connection =>

        val serviceactions = SQL(
          """
          select * from serviceaction
          order by name
          """).as(ServiceAction.simple *)

        serviceactions
    }
  }
  
  def loadAll(): List[ServiceAction] = {
    DB.withConnection { implicit connection =>
        SQL("select * from serviceaction").as(ServiceAction.simple *)
    }
  }

  /**
   * Upload a csvLine => insert serviceAction.
   *
   * @param csvLine line in csv file
   * @return nothing
   */
  def upload(csvLine: String) = {

    val dataCsv = csvLine.split(";")

    if (dataCsv.size != csvTitle.size)
      throw new Exception("Please check csvFile, " + csvTitle.size + " fields required")

    if (dataCsv(csvTitle.get("key").get) == csvKey) {
      uploadServiceAction(dataCsv)
    } else {
      Logger.info("Line does not match with " + csvKey + " of csvLine - ignored")
    }
  }

  /**
   * Check if serviceAction already exist (with same name). Insert or do nothing if exist.
   *
   * @param dataCsv line in csv file
   * @return serviceAction (new or not)
   */
  private def uploadServiceAction(dataCsv: Array[String]) = {

    val name = dataCsv(csvTitle.get("name").get)
    val s = findByName(name)

    s.map {
      serviceAction =>
        Logger.warn("ServiceAction " + serviceAction.name + " already exist")
        throw new Exception("Warning : ServiceAction " + serviceAction.name + " already exist")
    }.getOrElse {

      val serviceAction = new ServiceAction(
        -1,
        dataCsv(csvTitle.get("name").get).trim,
        dataCsv(csvTitle.get("thresholdms").get).toInt)
      ServiceAction.insert(serviceAction)
      Logger.info("Insert ServiceAction " + serviceAction.name)
    }
  }

}