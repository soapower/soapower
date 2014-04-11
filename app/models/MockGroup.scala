package models

import play.api.Play.current
import play.api.cache._

import java.util.{Calendar, GregorianCalendar}
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.libs.json._
import reactivemongo.bson._
import scala.concurrent.{Await, Future}
import play.modules.reactivemongo.json.BSONFormats._
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.core.commands.RawCommand
import play.api.Logger

case class MockGroup(_id: Option[BSONObjectID],
                       name: String,
                       groups: List[String]
                    )

object MockGroup {

  /*
   * Collection MongoDB
   */
  def collection: JSONCollection = ReactiveMongoPlugin.db.collection[JSONCollection]("mockGroups")

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
        "name" -> mockGroup.name,
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
  def csv(e: MockGroup) = {
    csvKey + ";" + e._id.get.stringify + ";" + e.name + ";" + e.groups.mkString("|") + "\n"
  }

  /**
   * Get All mockGroups, csv format.
   * @return List of MockGroups, csv format
   */
  def fetchCsv(): Future[List[String]] = {
    findAll.map(mockGroup => mockGroup.map(e => csv(e)))
  }

  /**
   * Construct the Map[String,String] needed to fill a select options set. Only mockGroups which are into the given group name are retrieved
   */
  def options(group: String): Seq[(String, String)] = {
    ???
    /*implicit connection =>
      val envs = Cache.getOrElse[Seq[(String, String)]](keyCacheAllOptions) {
        Logger.debug("MockGroups not found in cache: loading from db")
        SQL("select * from mockGroup, groups where mockGroup.groupId = groups.id and groups.name = {groupName} order by mockGroup.name").on(
          'groupName -> group).as(MockGroup.simple *).map(c => c.id.toString -> c.name)
      }
      sortEnvs(envs)
    */
  }


  /**
   * Construct the Map[String,String] needed to fill a select options set.
   */
  def options = {
    Cache.getOrElse(keyCacheAllOptions) {
      val f = findAll.map(mockGroups => mockGroups.map(e => (e._id.get.stringify, e.name)))
      Await result(f, 1.seconds)
    }
  }

  /**
   * Retrieve an MockGroup from id.
   */
  def findById(id: String): Future[Option[MockGroup]] = {
    val objectId = new BSONObjectID(id)
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

    if (options.exists {
      e => e._2.equals(mockGroup.name.trim)
    }) {
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

    if (options.exists {
      e => e._2.equals(mockGroup.name.trim) && e._1 != mockGroup._id.get.stringify
    }) {
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
      find(Json.obj()).
      sort(Json.obj("name" -> 1)).
      cursor[MockGroup].
      collect[List]()
  }

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
