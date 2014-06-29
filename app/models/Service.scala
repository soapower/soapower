package models

import play.api.Play.current
import play.api.cache.Cache
import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import play.api.libs.json.Json
import reactivemongo.bson.BSONBoolean
import reactivemongo.bson.BSONString
import reactivemongo.bson.BSONInteger
import org.jboss.netty.handler.codec.http.HttpMethod

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
                   mockGroupId: Option[String],
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
      serviceDoc.getAs[String]("mockGroupId"),
      environmentName)
}


case class Services(services: List[Service])

object Service {

  implicit val serviceFormat = Json.format[Service]
  implicit val servicesFormat = Json.format[Services]

  private val keyCacheRequest = "cacheServiceRequest-"

  implicit object ServicesBSONReader extends BSONDocumentReader[Services] {
    def read(doc: BSONDocument): Services = {
      if (doc.getAs[List[BSONDocument]]("services").isDefined) {
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
   * Services
   */
  val REST = "REST"
  val SOAP = "SOAP"

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
   * Retrieve a Service.
   * @param environmentName Name of environement
   * @param serviceId ObjectID of service
   * @return Option of service
   */
  def findById(environmentName: String, serviceId: String): Future[Option[Service]] = {
    val query = BSONDocument("name" -> environmentName)
    val projection = BSONDocument("name" -> 1, "groups" -> 1, "services" -> BSONDocument(
      "$elemMatch" -> BSONDocument("_id" -> BSONObjectID(serviceId))))
    Environment.collection.find(query, projection).cursor[Service].headOption
  }

  /**
   * Retrieve a Soap Service from localTarget / environmentName
   *
   * @param localTarget localTarget
   * @param environmentName Name of environment
   * @return service
   */
  def findByLocalTargetAndEnvironmentName(typeRequest: String, localTarget: String, environmentName: String, httpMethod: HttpMethod): Future[Option[Service]] = {
    Cache.getOrElse(keyCacheRequest + typeRequest + localTarget + environmentName + httpMethod.toString, 15) {
      val query = BSONDocument("name" -> environmentName)
      val projection = BSONDocument("name" -> 1, "groups" -> 1, "services" -> BSONDocument(
        "$elemMatch" -> BSONDocument(
          "localTarget" -> BSONString(localTarget),
          "httpMethod" -> BSONString(httpMethod.toString),
          "typeRequest" -> BSONString(typeRequest))))
      Environment.collection.find(query, projection).cursor[Service].headOption
    }
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
   * Remove first / in localTarget.
   */
  private def checkLocalTarget(localTarget: String) = {
    if (localTarget.startsWith("/")) localTarget.substring(1) else localTarget
  }

}
