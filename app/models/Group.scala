package models

import anorm._
import anorm.SqlParser._
import play.api._
import play.api.Play.current
import play.api.cache._
import play.api.db._

/**
 * @author Ronan Quintin - ronan.quintin@gmail.com
 *
 *         A group  can contain environments, and an environment must be contained by a group. A group does have a name.
 */
// Defining a case class
case class Group(id: Long, name: String)

object Group {

  val ID_DEFAULT_GROUP = 1

  /**
   * Group caches keys which are used in order to declare and manage a DB cache
   */
  private val keyCacheAllOptions = "group-options"
  private val keyCacheById = "group-by-id"
  private val keyCacheByName = "group-by-name"
  private val GROUP_NAME_PATTERN = "[a-zA-Z0-9]{1,200}"

  /**
   * SQL anorm row parser. This operation indicate how to parse a sql row.
   */
  val simple = {
    get[Long]("groups.id") ~
      get[String]("groups.name") map {
      case id ~ name
      => Group(id, name)
    }
  }

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "name" -> 2)

  val csvKey = "group"

  /**
   * Csv format.
   */
  val csv = {
    get[Long]("groups.id") ~
      get[String]("groups.name") map {
      case id ~ name =>
        id + ";" + name + ";" + "\n"
    }
  }


  /**
   * Get all groups from the database in csv format
   * @return List of groups, csv format
   */
  def fetchCsv(): List[String] = DB.withConnection {
    implicit c => SQL("select * from groups").as(Group.csv *)
  }


  /**
   * Construct the Map[String,String] needed to fill a select options set.
   */
  def options: Seq[(String, String)] = DB.withConnection {
    implicit connection =>
      val groups = Cache.getOrElse[Seq[(String, String)]](keyCacheAllOptions) {
        Logger.debug("Groups not found in cache: loading from db")
        SQL("select * from groups order by name").as(Group.simple *).map(c => c.id.toString -> c.name)
      }
      groups
  }

  /**
   * Retrieve an group from id.
   */
  def findById(id: Long): Option[Group] = {
    DB.withConnection {
      implicit connection =>
        Cache.getOrElse[Option[Group]](keyCacheById + id) {
          SQL("select * from groups where id = {id}").on('id -> id).as(Group.simple.singleOpt)
        }
    }
  }

  /**
   * Retrieve an group from it's name .
   */
  def findByName(name: String): Option[Group] = DB.withConnection {
    implicit connection =>
      Cache.getOrElse[Option[Group]](keyCacheByName + name) {
        SQL("select * from groups where name = {name}").on('name -> name).as(Group.simple.singleOpt)
      }
  }

  /**
   * Persist a new group to database.
   *
   * @param group The group to persist.
   */
  def insert(group: Group) = {
    // Clear the cache in order to ???
    clearCache

    if (!group.name.trim.matches(GROUP_NAME_PATTERN)) {
      throw new Exception("Group name invalid:" + group.name.trim)
    }

    // Insert the new group
    if (options.exists{g => g._2.equals(group.name.trim)}) {
      throw new Exception("Group with name " + group.name.trim + " already exist")
    }

    DB.withConnection {
      implicit connection =>
        SQL( """	insert into groups (id, name) values (null, {name})""").on('name -> group.name.trim).executeUpdate()
    }
  }

  /**
   * Update a group.
   *
   * @param group The group group.
   */
  def update(group: Group) = {
    clearCache

    if (!group.name.trim.matches(GROUP_NAME_PATTERN)) {
      throw new Exception("Group name invalid:" + group.name.trim)
    }

    if (options.exists{g => g._2.equals(group.name.trim) && g._1.toLong != group.id}) {
      throw new Exception("Group with name " + group.name.trim + " already exist")
    }

    Cache.remove(keyCacheById + group.id)
    DB.withConnection {
      implicit connection =>
        SQL("update groups set name = {name} where id = {id}").on(
          'id -> group.id,
          'name -> group.name.trim
        ).executeUpdate()
    }
  }

  /**
   * Delete a group.
   *
   * @param group group to delete
   */
  def delete(group: Group) = {
    clearCache
    Cache.remove(keyCacheById + group.id)
    DB.withConnection {
      implicit connection =>
        SQL("delete from groups where id = {id}").on('id -> group.id).executeUpdate()
    }
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
  def findAll: List[Group] = {
    DB.withConnection {
      implicit connection =>
        val groups = SQL(
          "select * from groups order by groups.name").as(Group.simple *)
        groups
    }
  }

}


