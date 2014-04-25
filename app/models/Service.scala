package models

import play.api.db._
import play.api.cache._
import play.api.Play.current
import play.api._

import anorm._
import anorm.SqlParser._
import scala.collection.mutable.{ Map, HashMap }

case class Service(
  id: Long,
  typeRequest: String,
  httpMethod: String,
  description: String,
  localTarget: String,
  remoteTarget: String,
  timeoutms: Long,
  recordContentData: Boolean,
  recordData: Boolean,
  useMockGroup: Boolean,
  environmentId: Long,
  mockGroupId: Long) {
}

object Service {

  /**
   * HTTP methods
   */
  val POST = "post"
  val GET = "get"
  val PUT = "put"
  val DELETE = "delete"

  /**
   * Services
   */
  val REST = "rest"
  val SOAP = "soap"

  // -- Parsers

  /**
   * Parse a Service from a ResultSet
   */
  val simple = {
    get[Long]("service.id") ~
    get[String]("service.typeRequest") ~
    get[String]("service.httpMethod") ~
    get[String]("service.description") ~
    get[String]("service.localTarget") ~
    get[String]("service.remoteTarget") ~
    get[Long]("service.timeoutms") ~
    get[String]("service.recordContentData") ~
    get[String]("service.recordData") ~
    get[String]("service.useMockGroup") ~
    get[Long]("service.environment_id") ~
    get[Long]("service.mockGroupId") map {
      case id ~ typeRequest ~ httpMethod ~ description ~ localTarget ~ remoteTarget ~ timeoutms ~ recordContentData ~ recordData ~ useMockGroup ~ environmentId ~ mockGroupId =>
        Service(id, typeRequest, httpMethod, description, localTarget, remoteTarget, timeoutms, (recordContentData == "true"), (recordData == "true"), (useMockGroup == "true"), environmentId, mockGroupId)
    }
  }

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "typeRequest" -> 2, "httpMethod"-> 3, "description" -> 4, "localTarget" -> 5, "remoteTarget" -> 6, "timeoutms" -> 7, "recordContentData" -> 8, "recordData" -> 9, "useMockGroup" -> 10, "environmentName" -> 11, "mockGroupName" -> 12)

  val csvKey = "service";

  /**
   * Csv format.
   */
  val csv = {
		  get[Long]("service.id") ~
      get[String]("service.typeRequest") ~
      get[String]("service.httpMethod") ~
      get[String]("service.description") ~
      get[String]("service.localTarget") ~
      get[String]("service.remoteTarget") ~
      get[Long]("service.timeoutms") ~
      get[String]("service.recordContentData") ~
      get[String]("service.recordData") ~
      get[String]("service.useMockGroup") ~
      get[String]("environment.name") ~
      get[String]("mock_group.name") map {
        case id ~ typeRequest ~ httpMethod ~ description ~ localTarget ~ remoteTarget ~ timeoutms ~ recordContentData ~ recordData ~ useMockGroup ~ environmentName ~ mockGroupName =>
          id + ";" + typeRequest + ";" + httpMethod + ";" + description + ";" + localTarget + ";" + remoteTarget + ";" + timeoutms + ";" + recordContentData + ";" + recordData + ";" + useMockGroup + ";" + environmentName + ";" + mockGroupName + "\n"
      }
  }

  /**
   * 3 Caches :
   * - one with key "environmentName + localTarget" and value : service
   * -> fill in findByLocalTargetAndEnvironmentName
   * -> clear in update and delete
   * - another with key cacheKey + service.id and value : environmentName + localTarget
   * -> used to update and delete from first cache
   * - another with key servicekeybyid
   * -> fill in findById
   * -> clear in update and delete
   */
  private val cacheKey = "servicekey-"

  private val cacheKeyServiceById = "servicekeybyid-"

  // -- Queries

  /**
   * Retrieve a Service from id.
   */
  def findById(id: Long): Option[Service] = {
    Cache.getOrElse[Option[Service]](cacheKeyServiceById + id) {
      Logger.debug("Service " + id + " not found in cache: loading from db")
      DB.withConnection {
        implicit connection =>
          SQL("select * from service where id = {id}").on(
            'id -> id).as(Service.simple.singleOpt)
      }
    }
  }

  /**
   * Retrieve the Rest Services matching the environment name and the http method
   * @param httpMethod
   * @param environmentName
   * @return
   */
  def findRestByMethodAndEnvironmentName(httpMethod:String, environmentName: String): Seq[(Long, String)] = {
      val services =
        DB.withConnection {
          implicit connection =>
            SQL(
              """
              select * from service
            left join environment on service.environment_id = environment.id
            where service.typeRequest like {typeRequest}
            and service.httpMethod like {httpMethod}
            and environment.name like {environmentName}
              """).on(
            'typeRequest -> "rest",
            'httpMethod -> httpMethod,
            'environmentName -> environmentName
            ).as(Service.simple *)
           .map(s => s.id -> s.localTarget)
        }
      services
  }

  /**
   * Retrieve a Soap Service from localTarget / environmentName
   *
   * @param localTarget localTarget
   * @param environmentName Name of environment
   * @return service
   */
  def findByLocalTargetAndEnvironmentName(typeRequest: String, localTarget: String, environmentName: String, httpMethod: String = Service.POST): Option[Service] = {
    val serviceKey = environmentName + "/" + localTarget
    val service = Cache.getAs[Service](serviceKey);

    if (service nonEmpty) {
      service
    } else {
      Logger.debug("Service " + environmentName + "/" + localTarget + " not found in cache: loading from db")

      val serviceInDb = DB.withConnection {
        implicit connection =>
          SQL(
            """
            select * from service
            left join environment on service.environment_id = environment.id
            where service.typeRequest like {typeRequest}
            and service.localTarget like {localTarget}
            and environment.name like {environmentName}
            and service.httpMethod like {httpMethod}
            """).on(
              'httpMethod -> httpMethod,
              'typeRequest -> typeRequest,
              'localTarget -> localTarget,
              'environmentName -> environmentName
            ).as(Service.simple.singleOpt)
      }
      if (serviceInDb isDefined) {
        Cache.set(serviceKey, serviceInDb.get)
        Cache.set(cacheKey + serviceInDb.get.id, serviceKey)
        Logger.debug("Service " + environmentName + "/" + localTarget + " put in cache with key " + cacheKey + serviceInDb.get.id)
        serviceInDb
      } else {
        None
      }
    }
  }
  

  /**
   * Insert a new service.
   *
   * @param service The service values.
   */
  def insert(service: Service) = {
    try {
      val localTarget = checkLocalTarget(service.localTarget)
      DB.withConnection {
        implicit connection =>
          SQL(
            """
            insert into service 
              (description, typeRequest, httpMethod, localTarget, remoteTarget, timeoutms, recordContentData, recordData, useMockGroup, environment_id, mockGroupId) values (
              {description}, {typeRequest}, {httpMethod}, {localTarget}, {remoteTarget}, {timeoutms}, {recordContentData}, {recordData}, {useMockGroup}, {environment_id}, {mockGroupId}
            )
            """).on(
              'typeRequest -> service.typeRequest,
              'httpMethod -> service.httpMethod,
              'description -> service.description,
              'localTarget -> localTarget,
              'remoteTarget -> service.remoteTarget,
              'timeoutms -> service.timeoutms,
              'recordContentData -> service.recordContentData.toString,
              'recordData -> service.recordData.toString,
              'useMockGroup -> service.useMockGroup.toString,
              'environment_id -> service.environmentId,
              'mockGroupId -> service.mockGroupId).executeUpdate()
      }

      val serviceKey = localTarget + Environment.optionsAll.find(t => t._1 == service.environmentId.toString).get._2
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
   * @param service The service values.
   */
  def update(service: Service) = {
    deleteFromCache(service.id)
    DB.withConnection {
      implicit connection =>
        SQL(
          """
          update service
          set typeRequest = {typeRequest},
          httpMethod = {httpMethod},
          description = {description},
          localTarget = {localTarget}, 
          remoteTarget = {remoteTarget}, 
          timeoutms = {timeoutms},
          recordContentData = {recordContentData},
          recordData = {recordData},
          useMockGroup = {useMockGroup},
          environment_id = {environmentId},
          mockGroupId = {mockGroupId}
          where id = {id}
          """).on(
            'id -> service.id,
            'typeRequest -> service.typeRequest,
            'httpMethod -> service.httpMethod,
            'description -> service.description,
            'localTarget -> checkLocalTarget(service.localTarget),
            'remoteTarget -> service.remoteTarget,
            'timeoutms -> service.timeoutms,
            'recordContentData -> service.recordContentData.toString,
            'recordData -> service.recordData.toString,
            'useMockGroup -> service.useMockGroup.toString,
            'environmentId -> service.environmentId,
            'mockGroupId -> service.mockGroupId).executeUpdate()
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
      Logger.debug("remove key cache 1 " + serviceKey.get.toString + " from cache")
      Cache.remove(serviceKey.get.toString)
      Logger.debug("remove key cache 2 " + cacheKey + id + " from cache")
      Cache.remove(cacheKey + id)
    }
    if (Cache.get(cacheKeyServiceById + id) isDefined) {
      Logger.debug("remove key cache 3 " + cacheKeyServiceById + id + " from cache")
      Cache.remove(cacheKeyServiceById + id)
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
      SQL( """
           select * from service, mock_group, environment
           where service.environment_id = environment.id
           and service.mockGroupId = mock_group.id
           """).as(Service.csv *)
  }

  /**
   * Return a list of Service.
   */
  def list: List[(Service, Environment)] = {
    DB.withConnection {
      implicit connection =>
        val services = SQL(
          """
          select * from service
          left join environment on service.environment_id = environment.id
          order by name asc, description asc
          """).as(Service.withEnvironment *)
        services
    }
  }


  /**
   * Return a list of Service which are linked to an environment which group is the given group
   */
  def list(group : String): List[(Service, Environment)] = {
    DB.withConnection {
      implicit connection =>
        val services = SQL(
          """
          select * from service, environment, groups
          where service.environment_id = environment.id
          and environment.groupId = groups.id
          and groups.name = {group}
          order by environment.name asc, description asc
          """).on('group -> group).as(Service.withEnvironment *)
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

    if (dataCsv.size != csvTitle.size) {
      Logger.error("Please check csvFile, " + csvTitle.size + " fields required")
      throw new Exception("Please check csvFile, " + csvTitle.size + " fields required")
    }

    if (dataCsv(csvTitle.get("key").get) == csvKey) {
      val environment = uploadEnvironment(dataCsv)
      val mockGroup = MockGroup.upload(dataCsv(csvTitle.get("mockGroupName").get), Group.ID_DEFAULT_GROUP)
      uploadService(dataCsv, environment, mockGroup)
    } else {
      Logger.info("Line does not match with " + csvKey + " of csvLine - ignored")
    }
  }

  /**
   * Check if service already exist (with localTarget and Environment). Insert or do nothing if exist.
   *
   * @param dataCsv line in csv file
   * @param environment service's environment
   * @param mockGroup service's Mock Group
   * @return service (new or not)
   */
  private def uploadService(dataCsv: Array[String], environment: Environment, mockGroup : MockGroup) = {

    val localTarget = dataCsv(csvTitle.get("localTarget").get)
    val typeRequest = dataCsv(csvTitle.get("typeRequest").get)
    val s = findByLocalTargetAndEnvironmentName(typeRequest, localTarget, environment.name)

    s.map {
      service =>
        Logger.warn("Service " + environment.name + "/" + localTarget + " already exist")
        throw new Exception("Warning : Service " + environment.name + "/" + localTarget + " already exist")
    }.getOrElse {
      val service = new Service(
        -1,
        dataCsv(csvTitle.get("typeRequest").get).trim,
        dataCsv(csvTitle.get("httpMethod").get).trim,
        dataCsv(csvTitle.get("description").get).trim,
        dataCsv(csvTitle.get("localTarget").get).trim,
        dataCsv(csvTitle.get("remoteTarget").get).trim,
        dataCsv(csvTitle.get("timeoutms").get).toLong,
        (dataCsv(csvTitle.get("recordContentData").get).trim == "true"),
        (dataCsv(csvTitle.get("recordData").get).trim == "true"),
        (dataCsv(csvTitle.get("useMockGroup").get).trim == "true"),
        environment.id,
        mockGroup.id)
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

      // Insert a new group which is linked to the default group
      Environment.insert(new Environment(-1, environmentName))
      
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
