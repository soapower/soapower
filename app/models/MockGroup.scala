package models

import play.api.Play.current
import play.api.cache._

import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.libs.json._
import reactivemongo.bson._
import scala.concurrent.{Await, Future}
import play.modules.reactivemongo.json.BSONFormats._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.core.commands.RawCommand
import play.api.Logger
import reactivemongo.api.collections.default.BSONCollection

case class MockGroup(_id: Option[BSONObjectID],
                     name: String,
                     groups: List[String])

object MockGroup {

  /*
   * Collection MongoDB
   */
  def collection: BSONCollection = ReactiveMongoPlugin.db.collection[BSONCollection]("mockGroups")

  implicit val mockGroupFormat = Json.format[MockGroup]

  implicit object MockGroupBSONReader extends BSONDocumentReader[MockGroup] {
    def read(doc: BSONDocument): MockGroup =
      MockGroup(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[String]("name").get,
        doc.getAs[List[String]]("groups").toList.flatten
      )
  }

  implicit object MockGroupBSONWriter extends BSONDocumentWriter[MockGroup] {
    def write(mockGroup: MockGroup): BSONDocument =
      BSONDocument(
        "_id" -> mockGroup._id,
        "name" -> BSONString(mockGroup.name),
        "groups" -> mockGroup.groups)
  }

  private val keyCacheAllOptions = "mockGroup-options"
  private val MOCKGROUP_NAME_PATTERN = "[a-zA-Z0-9]{1,200}"


  /**
   * Retrieve an MockGroup from id.
   */
  def findById(objectId: BSONObjectID): Future[Option[MockGroup]] = {
    val query = BSONDocument("_id" -> objectId)
    collection.find(query).one[MockGroup]
  }

  /**
   * Retrieve an MockGroup from name.
   */
  def findByName(name: String): Future[Option[MockGroup]] = {
    val query = BSONDocument("name" -> name)
    collection.find(query).one[MockGroup]
  }

  /**
   * Insert a new mockGroup.
   *
   * @param mockGroup The mockGroup values.
   */
  def insert(mockGroup: MockGroup) = {
    if (!mockGroup.name.trim.matches(MOCKGROUP_NAME_PATTERN)) {
      throw new Exception("MockGroup name invalid:" + mockGroup.name.trim)
    }

    if (Await.result(findByName(mockGroup.name.trim).map(e => e), 1.seconds).isDefined) {
      throw new Exception("MockGroup with name " + mockGroup.name.trim + " already exist")
    }

    clearCache
    collection.insert(mockGroup)
  }

  /**
   * Update a mockGroup.
   *
   * @param mockGroup The mockGroup values.
   */
  def update(mockGroup: MockGroup) = {
    if (!mockGroup.name.trim.matches(MOCKGROUP_NAME_PATTERN)) {
      throw new Exception("MockGroup name invalid:" + mockGroup.name.trim)
    }
    val existingGroup = Await.result(findByName(mockGroup.name.trim).map(e => e), 1.seconds)

    if (existingGroup.isDefined && mockGroup._id.equals(existingGroup.get._id.get)) {
      throw new Exception("MockGroup with name " + mockGroup.name.trim + " already exist")
    }

    val selector = BSONDocument("_id" -> mockGroup._id)

    val modifier = BSONDocument(
      "$set" -> BSONDocument(
        "name" -> mockGroup.name,
        "groups" -> mockGroup.groups)
    )

    clearCache
    collection.update(selector, modifier)
  }

  /**
   * Delete a mockGroup.
   *
   * @param id Id of the mockGroup to delete.
   */
  def delete(id: String) = {
    val objectId = new BSONObjectID(id)
    collection.remove(BSONDocument("_id" -> objectId))
  }

  def clearCache() {
    Cache.remove(keyCacheAllOptions)
  }

  /**
   * Return a list of all mockGroups.
   */
  def findAll: Future[List[MockGroup]] = {
    collection.
      find(BSONDocument()).
      sort(BSONDocument("name" -> 1)).
      cursor[MockGroup].
      collect[List]()
  }

  /**
   * Return a list of all mockGroups in some groups.
   */
  def findInGroups(groups: String): Future[List[MockGroup]] = {
    if ("all".equals(groups)) return findAll
    val find = BSONDocument("groups" -> BSONDocument("$in" -> groups.split(',')))
    collection.
      find(find).
      sort(BSONDocument("name" -> 1)).
      cursor[MockGroup].
      collect[List]()
  }

  /**
   * Find all distinct groups in mockGroups collections.
   *
   * @return all distinct groups
   */
  def findAllGroups(): Future[BSONDocument] = {
    val command = RawCommand(BSONDocument("distinct" -> "mockGroups", "key" -> "groups"))
    collection.db.command(command) // result is Future[BSONDocument]
  }

}
