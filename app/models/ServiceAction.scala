package models

import play.api.Play.current

import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.libs.json._
import reactivemongo.bson._
import scala.concurrent.{Await, Future}
import play.modules.reactivemongo.json.BSONFormats._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.core.commands.{Count, RawCommand}
import play.api.Logger
import reactivemongo.api.collections.default.BSONCollection

case class ServiceAction(_id: Option[BSONObjectID],
                         name: String,
                         groups: List[String],
                         thresholdms: Int)

object ServiceAction {

  /*
   * Collection MongoDB
   */
  def collection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("serviceActions")

  implicit val serviceActionFormat = Json.format[ServiceAction]

  implicit object ServiceActionBSONReader extends BSONDocumentReader[ServiceAction] {
    def read(doc: BSONDocument): ServiceAction =
      ServiceAction(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[String]("name").get,
        doc.getAs[List[String]]("groups").toList.flatten,
        doc.getAs[Int]("thresholdms").get
      )
  }

  implicit object ServiceActionBSONWriter extends BSONDocumentWriter[ServiceAction] {
    def write(serviceAction: ServiceAction): BSONDocument =
      BSONDocument(
        "_id" -> serviceAction._id,
        "name" -> BSONString(serviceAction.name),
        "groups" -> serviceAction.groups,
        "thresholdms" -> BSONInteger(serviceAction.thresholdms))
  }

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "name" -> 2, "groups" -> 3, "thresholdms" -> 4)

  val csvKey = "serviceAction"

  /**
   * Csv format.
   */
  def csv(m: ServiceAction) = {
    csvKey + ";" + m._id.get.stringify + ";" + m.name + ";" + m.thresholdms + ";"+ m.groups.mkString("|") + "\n"
  }

  /**
   * Get All serviceActions, csv format.
   * @return List of ServiceActions, csv format
   */
  def fetchCsv(): Future[List[String]] = {
    findAll.map(serviceAction => serviceAction.map(e => csv(e)))
  }


  /**
   * Retrieve an ServiceAction from id.
   */
  def findById(objectId: BSONObjectID): Future[Option[ServiceAction]] = {
    val query = BSONDocument("_id" -> objectId)
    collection.find(query).one[ServiceAction]
  }

  /**
   * Retrieve an ServiceAction from name.
   */
  def findByName(name: String): Future[Option[ServiceAction]] = {
    val query = BSONDocument("name" -> BSONString(name))
    collection.find(query).one[ServiceAction]
  }

  /**
   * Retrieve a ServiceAction from name and groups
   * @param name
   * @param groups
   * @return
   */
  def findByNameAndGroups(name: String, groups: List[String]): Future[Option[ServiceAction]] = {
    val query = BSONDocument("name" -> BSONString(name), "groups" -> groups)
    collection.find(query).one[ServiceAction]
  }

  /**
   * Count ServiceAction by name. Just to check if serviceAction already exist in collection.
   * @return 0 ou 1
   */
  def countByName(name: String): Int = {
    val futureCount = ReactiveMongoPlugin.db.command(Count(collection.name, Some(BSONDocument("name" -> BSONString(name)))))
    futureCount.map {
      count => // count is an Int
        Logger.debug("COUNT:" + count)
    }
    Await.result(futureCount.map(c => c), 1.seconds)
  }

  def countByNameAndGroups(name: String, groups: List[String]): Int = {
    val futureCount = ReactiveMongoPlugin.db.command(Count(collection.name, Some(BSONDocument("name" -> BSONString(name), "groups" -> groups))))
    futureCount.map {
      count => // count is an Int
        Logger.debug("COUNT:" + count)
    }
    Await.result(futureCount.map(c => c), 1.seconds)
  }

  /**
   * Insert a new serviceAction.
   *
   * @param serviceAction The serviceAction values.
   */
  def insert(serviceAction: ServiceAction) = {
    if (Await.result(findByNameAndGroups(serviceAction.name.trim, serviceAction.groups).map(e => e), 1.seconds).isDefined) {
      throw new Exception("ServiceAction with name " + serviceAction.name.trim + " and groups "+ serviceAction.groups.toString + " already exist")
    }
    collection.insert(serviceAction)
  }

  /**
   * Update a serviceAction.
   *
   * @param serviceAction The serviceAction values.
   */
  def update(serviceAction: ServiceAction) = {

    if (Await.result(findByName(serviceAction.name.trim).map(e => e), 1.seconds).isDefined) {
      throw new Exception("ServiceAction with name " + serviceAction.name.trim + " already exist")
    }

    val selector = BSONDocument("_id" -> serviceAction._id)

    val modifier = BSONDocument(
      "$set" -> BSONDocument(
        "name" -> serviceAction.name,
        "thresholdms" -> serviceAction.thresholdms)
    )

    collection.update(selector, modifier)
  }

  /**
   * Delete a serviceAction.
   *
   * @param id Id of the serviceAction to delete.
   */
  def delete(id: String) = {
    val objectId = new BSONObjectID(id)
    collection.remove(BSONDocument("_id" -> objectId))
  }

  /**
   * Return a list of all serviceActions.
   */
  def findAll: Future[List[ServiceAction]] = {
    collection.
      find(BSONDocument()).
      sort(BSONDocument("name" -> 1)).
      cursor[ServiceAction].
      collect[List]()
  }

  /**
   * Return a list of all serviceActions in some groups.
   */
  def findInGroups(groups: String): Future[List[ServiceAction]] = {
    if ("all".equals(groups)) return findAll
    val find = BSONDocument("groups" -> BSONDocument("$in" -> groups.split(',')))
    collection.
      find(find).
      sort(BSONDocument("name" -> 1)).
      cursor[ServiceAction].
      collect[List]()
  }

  /**
   * Find all distinct groups in serviceActions collections.
   *
   * @return all distinct groups
   */
  def findAllGroups(): Future[BSONDocument] = {
    val command = RawCommand(BSONDocument("distinct" -> collection.name, "key" -> "groups"))
    collection.db.command(command) // result is Future[BSONDocument]
  }

  /**
   * Upload a csvLine => insert serviceAction.
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
    val groups = dataCsv(csvTitle.get("groups").get).split('|').toList

    Logger.debug("upload serviceAction:" + name)

    findByNameAndGroups(name, groups).map {
      serviceAction => {
        if (serviceAction == None) {
          Logger.debug("Insert new serviceAction with name " + name)
          val newServiceAction = new ServiceAction(Some(BSONObjectID.generate),
            dataCsv(csvTitle.get("name").get).trim,
            dataCsv(csvTitle.get("groups").get).split('|').toList,
            dataCsv(csvTitle.get("thresholdms").get).trim.toInt
          )
          insert(newServiceAction).map {
            lastError =>
              if (lastError.ok) {
                Logger.debug("OK Insert new serviceAction with name " + name)
              } else {
                Logger.error("Detected error:%s".format(lastError))
                throw new Exception("Error while inserting new group with name : " + name)
              }
          }
        } else {
          Logger.warn("Warning : ServiceAction " + name + " already exist")
          throw new Exception("Warning : ServiceAction " + name + " already exist")
        }
      }
    }
  }
}
