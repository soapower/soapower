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
   * Retrieve an ServiceAction from id.
   */
  def findById(objectId: BSONObjectID): Future[Option[ServiceAction]] = {
    val query = BSONDocument("_id" -> objectId)
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

  def getThreshold(name: String, groups: List[String]) : Long = {
    val f = findByNameAndGroups(name, groups)
    val s = Await.result(f, 2.seconds)
      if (s.isDefined) {
        s.get.thresholdms.toLong
      } else {
        -1
      }
  }

  /**
   * Count ServiceAction by name. Just to check if serviceAction already exist in collection.
   * @return 0 ou 1
   */
  def countByName(name: String): Int = {
    val futureCount = ReactiveMongoPlugin.db.command(Count(collection.name, Some(BSONDocument("name" -> BSONString(name)))))
    Await.result(futureCount.map(c => c), 1.seconds)
  }

  def countByNameAndGroups(name: String, groups: List[String]): Int = {
    val futureCount = ReactiveMongoPlugin.db.command(Count(collection.name, Some(BSONDocument("name" -> BSONString(name), "groups" -> groups))))
    Await.result(futureCount.map(c => c), 1.seconds)
  }

  /**
   * Insert a new serviceAction.
   *
   * @param serviceAction The serviceAction values.
   */
  def insert(serviceAction: ServiceAction) = {
    if (Await.result(findByNameAndGroups(serviceAction.name.trim, serviceAction.groups).map(e => e), 1.seconds).isDefined) {
      throw new Exception("ServiceAction with name " + serviceAction.name.trim + " and groups " + serviceAction.groups.toString + " already exist")
    }
    collection.insert(serviceAction)
  }

  /**
   * Update a serviceAction.
   *
   * @param serviceAction The serviceAction values.
   */
  def update(serviceAction: ServiceAction) = {
    val selector = BSONDocument("_id" -> serviceAction._id)
    val modifier = BSONDocument(
      "$set" -> BSONDocument(
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
    val objectId = BSONObjectID.apply(id)
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

  def findAllNameInGroups(groups: String): Future[List[String]] = {
    var command = RawCommand(BSONDocument("distinct" -> collection.name, "key" -> "name", "query" -> BSONDocument()))
    if (!("all".equals(groups))) {
      command = RawCommand(BSONDocument("distinct" -> collection.name, "key" -> "name", "query" -> BSONDocument("groups" -> BSONDocument("$in" -> groups.split(',')))))
    }
    collection.db.command(command).map {
      list =>
        list.getAs[List[String]]("values").get
    }
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

}