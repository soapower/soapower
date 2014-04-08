package models

import play.api._
import play.api.Play.current
import play.api.cache._
import play.api.libs.json.Json
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.bson.{BSONDocumentWriter, BSONDocumentReader, BSONObjectID, BSONDocument}
import scala.concurrent.{Await, Future}
import play.modules.reactivemongo.json.BSONFormats._
import scala.concurrent.duration._


/**
 *
 * A group  can contain environments, and an environment must be contained by a group. A group does have a name.
 */
case class Group(_id: Option[BSONObjectID], name: String) {

}

object Group {

  /*
   * Collection MongoDB
   */
  def collection: JSONCollection = ReactiveMongoPlugin.db.collection[JSONCollection]("group")

  implicit val groupFormat = Json.format[Group]

  implicit object GroupBSONReader extends BSONDocumentReader[Group] {
    def read(doc: BSONDocument): Group =
      Group(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[String]("name").get)
  }

  implicit object GroupBSONWriter extends BSONDocumentWriter[Group] {
    def write(group: Group): BSONDocument =
      BSONDocument(
        "_id" -> group._id,
        "name" -> group.name)
  }

  //TODO Default group
  val ID_DEFAULT_GROUP = Long.box(1)

  /**
   * Group caches keys which are used in order to declare and manage a DB cache
   */
  private val keyCacheAllOptions = "group-options"
  private val GROUP_NAME_PATTERN = "[a-zA-Z0-9]{1,200}"

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "name" -> 2)

  val csvKey = "group"

  /**
   * Csv format.
   */
  def csv (g: Group) = csvKey + ";" + g._id.get.stringify + ";" + g.name + ";" + "\n"

  /**
   * Get all groups from the database in csv format
   * @return List of groups, csv format
   */
  def fetchCsv(): Future[List[String]] = {
    findAll.map(groups => groups.map(g => csv(g) ))
  }

  /**
   * Construct the Map[String,String] needed to fill a select options set.
   */
  def options = {
    Cache.getOrElse(keyCacheAllOptions) {
      val f = findAll.map(groups => groups.map(g => (g._id.get.stringify, g.name) ))
      Await result (f, 1.seconds)
    }
  }

  /**
   * Retrieve an group from id.
   */
  def findById(id: String): Future[Option[Group]] = {
    val objectId = new BSONObjectID(id)
    val query = BSONDocument("_id" -> objectId)
    collection.find(query).one[Group]
  }

  /**
   * Retrieve an group from it's name .
   */
  def findByName(name: String): Future[Option[Group]] = {
    val query = BSONDocument("name" -> name)
    collection.find(query).one[Group]
  }

  /**
   * Persist a new group to database.
   *
   * @param group The group to persist.
   */
  def insert(group: Group) = {

    if (!group.name.trim.matches(GROUP_NAME_PATTERN)) {
      throw new Exception("Group name invalid:" + group.name.trim)
    }

    if (options.exists{g => g._2.equals(group.name.trim)}) {
      throw new Exception("Group with name " + group.name.trim + " already exist")
    }

    clearCache
    collection.insert(group)
  }

  /**
   * Update a group.
   *
   * @param group The group group.
   */
  def update(group: Group) = {

    if (!group.name.trim.matches(GROUP_NAME_PATTERN)) {
      throw new Exception("Group name invalid:" + group.name.trim)
    }

    if (options.exists{g => g._2.equals(group.name.trim) && g._1 != group._id.get.stringify}) {
      throw new Exception("Group with name " + group.name.trim + " already exist")
    }

    val selector = BSONDocument("_id" -> group._id)

    val modifier = BSONDocument(
      "$set" -> BSONDocument(
        "name" -> group.name))

    clearCache
    collection.update(selector, modifier)
  }

  /**
   * Delete a group.
   *
   * @param id identifier of group to delete
   */
  def delete(id: String) = {
    val objectId = new BSONObjectID(id)
    collection.remove(BSONDocument("_id" -> objectId))
  }

  /**
   * Remove the all-options cache.
   */
  def clearCache() {
    Cache.remove(keyCacheAllOptions)
  }

  /**
   * Return a list of all groups.
   */
  def findAll: Future[List[Group]] = {
      collection.
        find(Json.obj()).
        sort(Json.obj("name" -> 1)).
        cursor[Group].
        collect[List]()

  }

  def upload(csvLine: String) = {

    val dataCsv = csvLine.split(";")

    if (dataCsv.size != csvTitle.size) {
      throw new Exception("Please check csvFile, " + csvTitle.size + " fields required")
    }

    if (dataCsv(csvTitle.get("key").get) == csvKey) {
      uploadGroup(dataCsv(csvTitle.get("name").get))
    } else {
      Logger.info("Line does not match with " + csvKey + " of csvLine - ignored")
    }
  }

  /**
   * Check if group exist and insert it if not
   *
   * @param name name of group
   * @return group
   */
  def uploadGroup(name : String): Group = {

    Logger.debug("upload groupName:" + name)

    val future = Group.findByName(name).map {
      group => {
        if (group == None) {
          Logger.debug("Insert new group with name " + name)
          val newGroup = new Group(Some(BSONObjectID.generate), name)
          Group.insert(newGroup).map {
            lastError =>
              if (lastError.ok) {
                newGroup
              } else {
                Logger.error("Detected error:%s".format(lastError))
                throw new Exception("Error while inserting new group with name : " + name)
              }
          }
          newGroup
        } else {
          Logger.debug("Group already exist : " + group.get.name)
          group.get
        }
      }
    }
    Await result (future, 1.seconds)
  }

}


