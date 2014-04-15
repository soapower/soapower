package models

import play.api.Play.current

import scala.collection.mutable.Map
import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import play.api.libs.json.Json
import play.api.Logger

case class Service(_id: Option[BSONObjectID],
                   description: String,
                   localTarget: String,
                   remoteTarget: String,
                   timeoutms: Double,
                   recordXmlData: Boolean,
                   recordData: Boolean,
                   useMockGroup: Boolean,
                   mockGroupId: Option[String],
                   environmentName: Option[String]) {

  def this(serviceDoc: BSONDocument, environmentName: Option[String]) =
    this(
      serviceDoc.getAs[BSONObjectID]("_id"),
      serviceDoc.getAs[String]("description").get,
      serviceDoc.getAs[String]("localTarget").get,
      serviceDoc.getAs[String]("remoteTarget").get,
      serviceDoc.getAs[Double]("timeoutms").get,
      serviceDoc.getAs[Boolean]("recordXmlData").get,
      serviceDoc.getAs[Boolean]("recordData").get,
      serviceDoc.getAs[Boolean]("useMockGroup").get,
      serviceDoc.getAs[String]("mockGroupId"),
      environmentName)
}


case class Services(services: List[Service])

object Service {

  implicit val serviceFormat = Json.format[Service]
  implicit val servicesFormat = Json.format[Services]

  implicit object ServicesReader extends BSONDocumentReader[Services] {
    def read(doc: BSONDocument): Services = {
      Logger.debug("Environemnt: " + BSONDocument.pretty(doc))
      if (doc.getAs[List[BSONDocument]]("services") isDefined) {
        val list = doc.getAs[List[BSONDocument]]("services").get.map(
          s => new Service(s, doc.getAs[String]("name"))
        )
        Services(list)
      } else {
        Services(List())
      }
    }
  }

  implicit object ServiceReader extends BSONDocumentReader[Service] {
    def read(doc: BSONDocument): Service = {
      val s = doc.getAs[List[BSONDocument]]("services").get.head
      new Service(s, doc.getAs[String]("name"))
    }
  }

  implicit object ServiceBSONWriter extends BSONDocumentWriter[Service] {
    def write(service: Service): BSONDocument =
      BSONDocument(
        "_id" -> service._id,
        "description" -> service.description,
        "localTarget" -> service.localTarget,
        "remoteTarget" -> service.remoteTarget,
        "timeoutms" -> service.timeoutms,
        "recordXmlData" -> service.recordXmlData,
        "recordData" -> service.recordData,
        "useMockGroup" -> service.useMockGroup,
        "mockGroupId" -> service.mockGroupId)
  }

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "description" -> 2, "localTarget" -> 3, "remoteTarget" -> 4, "timeoutms" -> 5, "recordXmlData" -> 6, "recordData" -> 7, "environmentName" -> 8, "mockGroupName" -> 9)

  val csvKey = "service"

  /**
   * Csv format of one service.
   * @param s service
   * @return csv format of the service (String)
   */
  def csv(s: Service) = {
    csvKey + ";" + s._id.get.stringify + ";" + s.description + ";" + s.localTarget + ";" + s.remoteTarget + ";" + s.timeoutms + ";" + s.recordXmlData + ";" + s.recordData + ";" + s.useMockGroup + ";" + s.environmentName + ";" + s.mockGroupId + "\n"
  }

  /**
   * Get All service, csv format.
   * @return List of Services, csv format
   */
  def fetchCsv(): Future[List[String]] = {
    findAll.map(service => service.map(s => csv(s)))
  }

  /**
   * Retrieve a Service.
   * @param environmentName Name of environement
   * @param serviceId ObjectID of service
   * @return Option of service
   */
  def findById(environmentName: String, serviceId: String): Future[Option[Service]] = {
    val query = BSONDocument("name" -> environmentName)
    val projection = BSONDocument("services" -> BSONDocument(
      "$elemMatch" -> BSONDocument("_id" -> BSONObjectID(serviceId))))
    Environment.collection.find(query, projection).cursor[Service].headOption
  }

  /**
   * Retrieve a Service from localTarget / environmentName
   *
   * @param localTarget localTarget
   * @param environmentName Name of environment
   * @return service
   */
  def findByLocalTargetAndEnvironmentName(localTarget: String, environmentName: String): Option[Service] = {
    ???
  }

  /**
   * Insert a new service.
   *
   * @param service The service values.
   */
  def insert(service: Service) = {
    // TODO Call checkLocalTarget(service.localTarget) ?

    val selectorEnv = BSONDocument("name" -> service.environmentName)
    val insert = BSONDocument("$push" -> BSONDocument("services" -> service))
    Environment.collection.update(selectorEnv, insert)
  }

  /**
   * Update a service.
   *
   * @param service The service values.
   */
  def update(service: Service) = {
    val selector = BSONDocument(
      "name" -> service.environmentName,
      "services._id" -> service._id
    )
    val update = BSONDocument("$set" -> BSONDocument("services.$" -> service))
    Environment.collection.update(selector, update)
  }

  /**
   * Delete a service.
   * @param environmentName environment name wich contains the service
   * @param serviceId id of the service to delete
   * @return
   */
  def delete(environmentName: String, serviceId: String) = {
    val selector = BSONDocument("name" -> environmentName)
    val update = BSONDocument("$pull" -> BSONDocument("services" -> BSONDocument("_id" -> BSONObjectID(serviceId))))
    Environment.collection.update(selector, update)
  }

  def findAll(environmentName: String): Future[Option[Services]] = {
    val query = BSONDocument("name" -> environmentName)
    Environment.collection.find(query).cursor[Services].headOption
  }

  /**
   * Return a list of all services.
   */
  def findAll: Future[List[Service]] = {
    ???
    /*
    collection.
      find(Json.obj()).
      sort(Json.obj("description" -> 1)).
      cursor[Service].
      collect[List]()
      */
  }

  /**
   * Return a list of Service.
   */
  def list: List[(Service, Environment)] = {
    ???
    /*DB.withConnection {
      implicit connection =>
        val services = SQL(
          """
          select * from service
          left join environment on service.environment_id = environment.id
          order by name asc, description asc
          """).as(Service.withEnvironment *)
        services
    }*/
  }


  /**
   * Return a list of Service which are linked to an environment which group is the given group
   */
  def list(group: String): List[(Service, Environment)] = {
    ???
    /*
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
    */
  }

  /**
   * Upload a csvLine => insert service & environment.
   *
   * @param csvLine line in csv file
   * @return nothing
   */
  def upload(csvLine: String) = {

    // TODO
    ???

    /*val dataCsv = csvLine.split(";")

    if (dataCsv.size != csvTitle.size)
      throw new Exception("Please check csvFile, " + csvTitle.size + " fields required")

    if (dataCsv(csvTitle.get("key").get) == csvKey) {
      val environment = uploadEnvironment(dataCsv)

      val mockGroup = MockGroup.upload(dataCsv(csvTitle.get("mockGroupName").get), Group.ID_DEFAULT_GROUP)
      uploadService(dataCsv, environment, mockGroup)
    } else {
      Logger.info("Line does not match with " + csvKey + " of csvLine - ignored")
    }*/
  }

  /**
   * Check if service already exist (with localTarget and Environment). Insert or do nothing if exist.
   *
   * @param dataCsv line in csv file
   * @param environment service's environment
   * @param mockGroup service's Mock Group
   * @return service (new or not)
   */
  private def uploadService(dataCsv: Array[String], environment: Environment, mockGroup: MockGroup) = {

    ???

    /*
    val localTarget = dataCsv(csvTitle.get("localTarget").get)
    val s = findByLocalTargetAndEnvironmentName(localTarget, environment.name)

    s.map {
      service =>
        Logger.warn("Service " + environment.name + "/" + localTarget + " already exist")
        throw new Exception("Warning : Service " + environment.name + "/" + localTarget + " already exist")
    }.getOrElse {

      val service = new Service(
        -1,
        dataCsv(csvTitle.get("description").get).trim,
        dataCsv(csvTitle.get("localTarget").get).trim,
        dataCsv(csvTitle.get("remoteTarget").get).trim,
        dataCsv(csvTitle.get("timeoutms").get).toLong,
        (dataCsv(csvTitle.get("recordXmlData").get).trim == "true"),
        (dataCsv(csvTitle.get("recordData").get).trim == "true"),
        (dataCsv(csvTitle.get("useMockGroup").get).trim == "true"),
        environment._id.get.stringify,
        mockGroup.id)
      Service.insert(service)
      Logger.info("Insert Service " + environment.name + "/" + localTarget)
    }
    */
  }

  /**
   * Check if environment exist and insert it if not
   *
   * @param dataCsv List of string
   * @return environment
   */
  private def uploadEnvironment(dataCsv: Array[String]): Environment = {
    ???
    /*
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
    */
  }

  /**
   * Remove first / in localTarget
   */
  private def checkLocalTarget(localTarget: String) = {
    if (localTarget.startsWith("/")) localTarget.substring(1) else localTarget
  }

}
