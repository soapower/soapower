package models


import scala.collection.mutable.Map
import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.{Await, Future}
import play.api.libs.json.Json
import play.api.Logger
import reactivemongo.bson.BSONBoolean
import reactivemongo.bson.BSONString
import scala.Some
import reactivemongo.bson.BSONInteger
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class Service(_id: Option[BSONObjectID],
                   description: String,
                   typeRequest: String,
                   httpMethod: String,
                   localTarget: String,
                   remoteTarget: String,
                   timeoutms: Int,
                   recordContentData: Boolean,
                   recordData: Boolean,
                   useMockGroup: Boolean,
                   mockGroupId: Option[BSONObjectID],
                   environmentName: Option[String]) {

  def this(serviceDoc: BSONDocument, environmentName: Option[String]) =
    this(
      serviceDoc.getAs[BSONObjectID]("_id"),
      serviceDoc.getAs[String]("description").get,
      serviceDoc.getAs[String]("typeRequest").get,
      serviceDoc.getAs[String]("httpMethod").get,
      serviceDoc.getAs[String]("localTarget").get,
      serviceDoc.getAs[String]("remoteTarget").get,
      serviceDoc.getAs[Int]("timeoutms").get,
      serviceDoc.getAs[Boolean]("recordContentData").get,
      serviceDoc.getAs[Boolean]("recordData").get,
      serviceDoc.getAs[Boolean]("useMockGroup").get,
      serviceDoc.getAs[BSONObjectID]("mockGroupId"),
      environmentName)
}


case class Services(services: List[Service])

object Service {

  implicit val serviceFormat = Json.format[Service]
  implicit val servicesFormat = Json.format[Services]

  implicit object ServicesBSONReader extends BSONDocumentReader[Services] {
    def read(doc: BSONDocument): Services = {
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

  implicit object ServiceBSONReader extends BSONDocumentReader[Service] {
    def read(doc: BSONDocument): Service = {
      if (doc.getAs[List[BSONDocument]]("services").isDefined) {
        val s = doc.getAs[List[BSONDocument]]("services").get.head
        new Service(s, doc.getAs[String]("name"))
      } else {
        null
      }
    }
  }

  implicit object ServiceBSONWriter extends BSONDocumentWriter[Service] {
    def write(service: Service): BSONDocument =
      BSONDocument(
        "_id" -> service._id,
        "description" -> BSONString(service.description),
        "typeRequest" -> BSONString(service.typeRequest),
        "httpMethod" -> BSONString(service.httpMethod),
        "localTarget" -> BSONString(checkLocalTarget(service.localTarget)),
        "remoteTarget" -> BSONString(service.remoteTarget),
        "timeoutms" -> BSONInteger(service.timeoutms),
        "recordContentData" -> BSONBoolean(service.recordContentData),
        "recordData" -> BSONBoolean(service.recordData),
        "useMockGroup" -> BSONBoolean(service.useMockGroup),
        "mockGroupId" -> service.mockGroupId)
  }

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "description" -> 2, "typeRequest" -> 3, "httpMethod" -> 4, "localTarget" -> 5, "remoteTarget" -> 6, "timeoutms" -> 7, "recordContentData" -> 8, "recordData" -> 9, "useMockGroup" -> 10, "environmentName" -> 11, "mockGroupName" -> 12)

  val csvKey = "service"

  /**
   * Csv format of one service.
   * @param s service
   * @return csv format of the service (String)
   */
  def csv(s: Service) = {
    csvKey + ";" + s._id.get.stringify + ";" + s.description + ";" + s.typeRequest + ";" + s.httpMethod + ";" + s.localTarget + ";" + s.remoteTarget + ";" + s.timeoutms + ";" + s.recordContentData + ";" + s.recordData + ";" + s.useMockGroup + ";" + s.environmentName.get + ";" + s.mockGroupId + "\n"
  }

  /**
   * Get All service, csv format.
   * @return List of Services, csv format
   */
  def fetchCsv(): Future[List[String]] = {
    findAll.map(services => services.map(service => csv(service)))
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
   * Retrieve the Rest Services matching the environment name and the http method
   * @param httpMethod
   * @param environmentName
   * @return
   */
  def findRestByMethodAndEnvironmentName(httpMethod: String, environmentName: String): Seq[(Long, String)] = {
    ???
    // TODO
    /*
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
    */
  }

  /**
   * Retrieve a Soap Service from localTarget / environmentName
   *
   * @param localTarget localTarget
   * @param environmentName Name of environment
   * @return service
   */
  def findByLocalTargetAndEnvironmentName(localTarget: String, environmentName: String): Future[Option[Service]] = {
    val query = BSONDocument("name" -> environmentName)
    val projection = BSONDocument("services" -> BSONDocument(
      "$elemMatch" -> BSONDocument("localTarget" -> BSONString(localTarget))))
    Environment.collection.find(query, projection).cursor[Service].headOption
  }

  /**
   * Insert a new service.
   *
   * @param service The service values
   */
  def insert(service: Service) = {
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
    val query = BSONDocument()
    Environment.collection.find(query).cursor[Services].collect[List]().map(l => l.flatMap(s => s.services))
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
    val dataCsv = csvLine.split(";")
    if (dataCsv.size != csvTitle.size) {
      throw new Exception("Please check csvFile, " + csvTitle.size + " fields required")
    }
    if (dataCsv(csvTitle.get("key").get) == csvKey) {
      uploadService(dataCsv)
    } else {
      Logger.info("Line does not match with " + csvKey + " of csvLine - ignored")
    }
  }

  /**
   * Check if service already exist (with localTarget and Environment). Insert or do nothing if exist.
   *
   * @param dataCsv line in csv file
   * @return service (new or not)
   */
  private def uploadService(dataCsv: Array[String]) = {

    val environmentName = dataCsv(csvTitle.get("environmentName").get)
    val localTarget = dataCsv(csvTitle.get("localTarget").get)
    val f = findByLocalTargetAndEnvironmentName(localTarget, environmentName)
    val service = Await.result(f, 1.seconds)
    if (service.get != null) {
      // null comes from ServiceBSONReader
      Logger.warn("Service " + environmentName + "/" + localTarget + " already exist")
      throw new Exception("Warning : Service " + environmentName + "/" + localTarget + " already exist")
    } else {
      var mockGroupId: Option[BSONObjectID] = None
      if (dataCsv(csvTitle.get("mockGroupName").get) != "None") {
        val m = Await.result(MockGroup.findByName(dataCsv(csvTitle.get("mockGroupName").get)), 1.seconds)
        if (m.isDefined) mockGroupId = m.get._id
      }
      val service = new Service(
        Some(BSONObjectID.generate),
        dataCsv(csvTitle.get("description").get).trim,
        dataCsv(csvTitle.get("typeRequest").get).trim,
        dataCsv(csvTitle.get("httpMethod").get).trim,
        dataCsv(csvTitle.get("localTarget").get).trim,
        dataCsv(csvTitle.get("remoteTarget").get).trim,
        dataCsv(csvTitle.get("timeoutms").get).toInt,
        (dataCsv(csvTitle.get("recordContentData").get).trim == "true"),
        (dataCsv(csvTitle.get("recordData").get).trim == "true"),
        (dataCsv(csvTitle.get("useMockGroup").get).trim == "true"),
        mockGroupId,
        Some(environmentName))
      Service.insert(service)
      Logger.info("Insert Service " + environmentName + "/" + localTarget)
    }
  }


  /**
   * Remove first / in localTarget.
   */
  private def checkLocalTarget(localTarget: String) = {
    if (localTarget.startsWith("/")) localTarget.substring(1) else localTarget
  }

}
