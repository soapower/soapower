package models

import play.api.db._
import play.api.cache._
import play.api.Play.current
import play.api._

import anorm._
import anorm.SqlParser._
import scala.collection.mutable.{ Map, HashMap }

case class Service(
  id: Pk[Long],
  description: String,
  localTarget: String,
  remoteTarget: String,
  timeoutms: Long,
  environmentId: Long) {
}

object Service {
  // -- Parsers

  /**
   * Parse a Service from a ResultSet
   */
  val simple = {
    get[Pk[Long]]("service.id") ~
    get[String]("service.description") ~
    get[String]("service.localTarget") ~
    get[String]("service.remoteTarget") ~
    get[Long]("service.timeoutms") ~
    get[Long]("service.environment_id") map {
      case id ~ description ~ localTarget ~ remoteTarget ~ timeoutms ~ environmentId =>
        Service(id, description, localTarget, remoteTarget, timeoutms, environmentId)
    }
  }

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "description" -> 2, "localTarget" -> 3, "remoteTarget" -> 4, "timeoutms" -> 5, "environmentName" -> 6)

  val csvKey = "service";

  /**
   * Csv format.
   */
  val csv = {
    get[Pk[Long]]("service.id") ~
      get[String]("service.description") ~
      get[String]("service.localTarget") ~
      get[String]("service.remoteTarget") ~
      get[Long]("service.timeoutms") ~
      get[String]("environment.name") map {
        case id ~ description ~ localTarget ~ remoteTarget ~ timeoutms ~ environmentName =>
          id + ";" + description + ";" + localTarget + ";" + remoteTarget + ";" + timeoutms + ";" + environmentName + "\n"
      }
  }

  /**
   * 2 Caches :
   * - one with key "environmentName + localTarget" and value : service
   * -> fill in findByLocalTargetAndEnvironmentName
   * -> clear in update and delete
   * - another with key cacheKey + service.id and value : environmentName + localTarget
   * -> used to update and delete from first cache
   */
  private val cacheKey = "servicekey-"

  // -- Queries

  /**
   * Retrieve a Service from id.
   */
  def findById(id: Long): Option[Service] = {
    Cache.getOrElse[Option[Service]](cacheKey + id) {
      Logger.debug("Service " + id + " not found in cache: loading from db")
      DB.withConnection {
        implicit connection =>
          SQL("select * from service where id = {id}").on(
            'id -> id).as(Service.simple.singleOpt)
      }
    }
  }

  /**
   * Retrieve a Service from localTarget / environmentName
   *
   * @param localTarget localTarget
   * @param environmentName Name of environment
   * @return service
   */
  def findByLocalTargetAndEnvironmentName(localTarget: String, environmentName: String): Option[Service] = {
    val serviceKey = localTarget + environmentName
    val service = Cache.getOrElse[Option[Service]](serviceKey) {
      Logger.debug("Service " + environmentName + "/" + localTarget + " not found in cache: loading from db")

      var serviceInDb = DB.withConnection {
        implicit connection =>
          SQL(
            """
            select * from service
            left join environment on service.environment_id = environment.id
            where service.localTarget like {localTarget}
            and environment.name like {environmentName}
            """).on(
              'localTarget -> localTarget,
              'environmentName -> environmentName).as(Service.simple.singleOpt)
      }
      if (serviceInDb isDefined) {
        Cache.set(cacheKey + serviceInDb.get.id, serviceKey)
        Logger.debug("Service: " + environmentName + "/" + localTarget + " put in cache")
      }
      serviceInDb
    }

    return service
  }
  

  /**
   * Insert a new service.
   *
   * @param service The service values.
   */
  def insert(service: Service) = {
    try {
      var localTarget = checkLocalTarget(service.localTarget)
      DB.withConnection {
        implicit connection =>
          SQL(
            """
            insert into service 
              (description, localTarget, remoteTarget, timeoutms, environment_id) values (
              {description}, {localTarget}, {remoteTarget}, {timeoutms}, {environment_id}
            )
            """).on(
              'description -> service.description,
              'localTarget -> localTarget,
              'remoteTarget -> service.remoteTarget,
              'timeoutms -> service.timeoutms,
              'environment_id -> service.environmentId).executeUpdate()
      }

      val serviceKey = localTarget + Environment.options.find(t => t._1 == service.environmentId.toString).get._2
      val inst = Cache.get(serviceKey)
      if (inst isDefined) {
        Logger.debug("Insert new service - Delete from cache key:" + serviceKey)
        Cache.remove(serviceKey)
      }

    } catch {
      //case e:SQLException => Logger.error("Database error")
      //case e:MalformedURLException => Logger.error("Bad URL")
      case e: Exception => Logger.error("Caught an exception! ", e)
    }
  }

  /**
   * Update a service.
   *
   * @param id The service id
   * @param service The service values.
   */
  def update(id: Long, service: Service) = {
    deleteFromCache(id)
    DB.withConnection {
      implicit connection =>
        SQL(
          """
          update service
          set description = {description}, 
          localTarget = {localTarget}, 
          remoteTarget = {remoteTarget}, 
          timeoutms = {timeoutms},
          environment_id = {environment_id} 
          where id = {id}
          """).on(
            'id -> id,
            'description -> service.description,
            'localTarget -> checkLocalTarget(service.localTarget),
            'remoteTarget -> service.remoteTarget,
            'timeoutms -> service.timeoutms,
            'environment_id -> service.environmentId).executeUpdate()
    }
  }

  /**
   * Delete a service from cache.
   *
   * @param id id of service
   */
  private def deleteFromCache(id: Long) {
    val serviceKey = Cache.get(cacheKey + id);
    if (serviceKey isDefined) {
      Logger.debug("remove " + serviceKey.get.toString + " from cache")
      Cache.remove(serviceKey.get.toString)
      Cache.remove(cacheKey + id)
    }
  }

  /**
   * Delete a service.
   *
   * @param id Id of the service to delete.
   */
  def delete(id: Long) = {
    deleteFromCache(id)
    DB.withConnection {
      implicit connection =>
        SQL("delete from service where id = {id}").on('id -> id).executeUpdate()
    }
  }

  /**
   * Parse a (Service,Environment) from a ResultSet
   */
  val withEnvironment = Service.simple ~ Environment.simple map {
    case service ~ environment => (service, environment)
  }

  /**
   * Get All service, csv format.
   * @return List of Service, csv format
   */
  def fetchCsv(): List[String] = DB.withConnection {
    implicit c =>
      SQL("select * from service left join environment on service.environment_id = environment.id")
        .as(Service.csv *)
  }

  /**
   * Return a list of (Service, Environment).
   */
  def list: List[(Service, Environment)] = {
    DB.withConnection {
      implicit connection =>

        val services = SQL(
          """
          select * from service
          left join environment on service.environment_id = environment.id
          order by environment_id asc, description asc
          """).as(Service.withEnvironment *)

        services
    }
  }

  /**
   * Upload a csvLine => insert service & environment.
   *
   * @param csvLine line in csv file
   * @return nothing
   */
  def upload(csvLine: String) = {

    val dataCsv = csvLine.split(";")

    if (dataCsv.size != csvTitle.size)
      throw new Exception("Please check csvFile, " + csvTitle.size + " fields required")

    if (dataCsv(csvTitle.get("key").get) == csvKey) {
      val environment = uploadEnvironment(dataCsv)
      uploadService(dataCsv, environment)
    } else {
      Logger.info("Line does not match with " + csvKey + " of csvLine - ignored")
    }
  }

  /**
   * Check if service already exist (with localTarget and Environment). Insert or do nothing if exist.
   *
   * @param dataCsv line in csv file
   * @param environment service's environment
   * @return service (new or not)
   */
  private def uploadService(dataCsv: Array[String], environment: Environment) = {

    val localTarget = dataCsv(csvTitle.get("localTarget").get)
    val s = findByLocalTargetAndEnvironmentName(localTarget, environment.name)

    s.map {
      service =>
        Logger.warn("Service " + environment.name + "/" + localTarget + " already exist")
        throw new Exception("Warning : Service " + environment.name + "/" + localTarget + " already exist")
    }.getOrElse {

      val service = new Service(
        NotAssigned,
        dataCsv(csvTitle.get("description").get).trim,
        dataCsv(csvTitle.get("localTarget").get).trim,
        dataCsv(csvTitle.get("remoteTarget").get).trim,
        dataCsv(csvTitle.get("timeoutms").get).toLong,
        environment.id.get)
      Service.insert(service)
      Logger.info("Insert Service " + environment.name + "/" + localTarget)
    }
  }

  /**
   * Check if environment exist and insert it if not
   *
   * @param dataCsv List of string
   * @return environment
   */
  private def uploadEnvironment(dataCsv: Array[String]): Environment = {
    val environmentName = dataCsv(csvTitle.get("environmentName").get)
    Logger.debug("environmentName:" + environmentName)

    var environment = Environment.findByName(environmentName)
    if (environment == None) {
      Logger.debug("Insert Environment " + environmentName)
      Environment.insert(new Environment(NotAssigned, environmentName))
      environment = Environment.findByName(environmentName)
      if (environment.get == null) Logger.error("Environment insert failed : " + environmentName)
    } else {
      Logger.debug("Environment already exist : " + environment.get.name)
    }
    environment.get
  }

  /**
   * Remove first / in localTarget
   */
  private def checkLocalTarget(localTarget: String) = {
    if (localTarget.startsWith("/")) localTarget.substring(1) else localTarget
  }

}
