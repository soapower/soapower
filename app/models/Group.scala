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

/**
 * Default group definition
 */
object DefaultGroup {
  /**
   * Default group identifiers
   */
  private val defaultGroupId = 1;
  private val defaultGroupName = "DefaultGroup";

  /**
   * Default group value
   */
  val defaultGroupObject = new Group(defaultGroupId,defaultGroupName);

}

object Group {

  /**
   * Group caches keys which are used in order to declare and manage a DB cache
   */
  private val keyCacheAllOptions = "group-options"
  private val keyCacheById = "group-by-id"
  private val keyCacheByName = "group-by-name"



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
   * Check that the given group doesn't already exist.
   * @param group The group which's unicity has to be checked
   * @return true if the group is unique, false otherwise
   */
  def checkUniqueGroup(group: Group) : Boolean = {
    var existingGroup = findByName(group.name)
    (existingGroup == None)
  }

  /**
   * Persist a new group to database.
   *
   * @param group The group to persist.
   */
  def insert(group: Group) = {
    // A group which looks like the default group cannot be inserted
    if(group.id != DefaultGroup.defaultGroupObject.id && group.name != DefaultGroup.defaultGroupObject.name){

      // Check that the group to insert doesn't already exist
      if(checkUniqueGroup(group)){

        // Clear the cache in order to ???
        clearCache
        // Insert the new group
        DB.withConnection {
          implicit connection =>
            SQL( """	insert into groups values (null, {name})""").on('name -> group.name).executeUpdate()
        }
      } // Unicity check
    } // Default group check
  }

  /**
   * Update a group.
   *
   * @param group The group group.
   */
  def update(group: Group) = {
    //The default group cannot be modified
    if(group.id != DefaultGroup.defaultGroupObject.id){

      // Check that the group to insert doesn't already exist
      if(checkUniqueGroup(group)){

        Cache.remove(keyCacheById + group.id)
        clearCache
        DB.withConnection {
          implicit connection =>
          SQL( """update groups set name = {name} where id = {id}""").on(
            'id -> group.id,
            'name -> group.name
          ).executeUpdate()
        }
      }  // Unicity check
    } // Default group check
  }

  /**
   * Delete a group.
   *
   * @param group group to delete
   */
  def delete(group: Group) = {

    // The default group cannot be delete
    if(group.id != DefaultGroup.defaultGroupObject.id){
      clearCache
      Cache.remove(keyCacheById + group.id)
      DB.withConnection {
        implicit connection =>
          SQL("delete from groups where id = {id}").on('id -> group.id).executeUpdate()
      }
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


