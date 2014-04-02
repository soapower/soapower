package models

import anorm._
import anorm.SqlParser._
import play.api._
import play.api.Play.current
import play.api.cache._
import play.api.db._
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.bson.{BSONDocumentWriter, BSONDocumentReader, BSONObjectID, BSONDocument}
import scala.concurrent.Future
import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.core.commands.RawCommand
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._

/**
 *
 * A group  can contain environments, and an environment must be contained by a group. A group does have a name.
 */
case class Group(_id: Option[BSONObjectID], name: String) {

}

object Group {

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
  private val keyCacheById = "group-by-id"
  private val keyCacheByName = "group-by-name"
  private val GROUP_NAME_PATTERN = "[a-zA-Z0-9]{1,200}"

  //implicit val groupFormat = Json.format[Group]

  def collection: JSONCollection = ReactiveMongoPlugin.db.collection[JSONCollection]("group")

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
  def options: Seq[(String, String)] = {
    //TODO
    ???
    /*implicit connection =>
      val groups = Cache.getOrElse[Seq[(String, String)]](keyCacheAllOptions) {
        Logger.debug("Groups not found in cache: loading from db")
        SQL("select * from groups order by name").as(Group.simple *).map(c => c.id.toString -> c.name)
      }
      groups
      */
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
   * @param json The group to persist.
   */
  def insert(json: JsObject) = {

    /*
    //TODOif (!group.name.trim.matches(GROUP_NAME_PATTERN)) {
      throw new Exception("Group name invalid:" + group.name.trim)
    }*/

    // TODO Insert the new group
    /*if (options.exists{g => g._2.equals(group.name.trim)}) {
      throw new Exception("Group with name " + group.name.trim + " already exist")
    }*/

    collection.insert(json)
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

    //TODO
    /*if (options.exists{g => g._2.equals(group.name.trim) && g._1.toLong != group.id}) {
      throw new Exception("Group with name " + group.name.trim + " already exist")
    }*/

    val selector = BSONDocument("_id" -> group._id)

    val modifier = BSONDocument(
      "$set" -> BSONDocument(
        "name" -> group.name))

    Cache.remove(keyCacheById + group._id)
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

  /**
   * Check if group exist and insert it if not
   *
   * @param groupName Name of the group
   * @return group
   */
  def upload(groupName: String): Group = {

    //TODO
    ???
    /*Logger.debug("groupName:" + groupName)

    var group = Group.findByName(groupName)
    if (group == None) {
      Logger.debug("Insert group " + groupName)
      Group.insert(new Group(0, groupName))
      group = Group.findByName(groupName)
      if (group.get == null) Logger.error("Group insert failed : " + groupName)
    } else {
      Logger.debug("Group already exist : " + group.get.name)
    }
    group.get
    */
  }

}


