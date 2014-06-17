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
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "name" -> 2, "groups" -> 3)

  val csvKey = "mockGroup"

  /**
   * Csv format.
   */
  def csv(m: MockGroup) = {
    csvKey + ";" + m._id.get.stringify + ";" + m.name + ";" + m.groups.mkString("|") + "\n"
  }

  /**
   * Get All mockGroups, csv format.
   * @return List of MockGroups, csv format
   */
  def fetchCsv(): Future[List[String]] = {
    findAll.map(mockGroup => mockGroup.map(e => csv(e)))
  }


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

  /**
   * Upload a csvLine => insert mockGroup.
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
      uploadMockGroup(dataCsv)
    } else {
      Logger.info("Line does not match with " + csvKey + " of csvLine - ignored")
    }
  }

  /**
   * Check if mockGroup already exist (with same name). Insert or do nothing if exist.
   *
   * @param dataCsv line in csv file
   * @return mockGroup (new or not)
   */
  private def uploadMockGroup(dataCsv: Array[String]) = {

    val name = dataCsv(csvTitle.get("name").get)
    Logger.debug("upload mockGroup:" + name)

    findByName(name).map {
      mockGroup => {
        if (mockGroup == None) {
          Logger.debug("Insert new mockGroup with name " + name)
          val newMockGroup = new MockGroup(Some(BSONObjectID.generate),
            dataCsv(csvTitle.get("name").get).trim,
            dataCsv(csvTitle.get("groups").get).split('|').toList // single quote of split is important
          )
          insert(newMockGroup).map {
            lastError =>
              if (lastError.ok) {
                Logger.debug("OK Insert new mockGroup with name " + name)
              } else {
                Logger.error("Detected error:%s".format(lastError))
                throw new Exception("Error while inserting new group with name : " + name)
              }
          }
        } else {
          Logger.warn("Warning : MockGroup " + name + " already exist")
          throw new Exception("Warning : MockGroup " + name + " already exist")
        }
      }
    }
  }
}
