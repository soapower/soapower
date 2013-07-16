/**
 *
 */
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
 * A group  can contain environments, and an environment must be contained by a group. A group does have a name.
 */
// Defining a case class
case class Group(id: Long, name: String)

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
    get[Long]("groupId") ~
      get[String]("groupName") map {
      case groupId ~ groupName
      => Group(groupId, groupName)
    }
  }

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "groupId" -> 1, "groupName" -> 2)

  val csvKey = "group";

  /**
   * Csv format.
   */
  val csv = {
    get[Long]("groupId") ~
      get[String]("groupName") map {
      case groupId ~ groupName =>
        groupId + ";" + groupName + ";" + "\n"
    }
  }


  /**
   * Get all groups from the database in csv format
   * @return List of groups, csv format
   */
  def fetchCsv(): List[String] = DB.withConnection {
    implicit c => SQL("select * from group").as(Group.csv *)
  }


  /**
   * Construct the Map[String,String] needed to fill a select options set.
   */
  def options: Seq[(String, String)] = DB.withConnection {
    implicit connection =>
      val groups = Cache.getOrElse[Seq[(String, String)]](keyCacheAllOptions) {
        Logger.debug("Groups not found in cache: loading from db")
        SQL("select * from environment_group order by groupName").as(Group.simple *).map(c => c.id.toString -> c.name)
      }

      val sortedGroups = groups.sortWith {
        (a, b) =>
          val pattern = """^(.+?)([0-9]+)$""".r

          val matchA = pattern.findAllIn(a._2)
          val matchB = pattern.findAllIn(b._2)

          if (matchA.hasNext && matchB.hasNext) {
            // both names match the regex: compare name then number
            val nameA = matchA.group(1)
            val numberA = matchA.group(2)
            val nameB = matchB.group(1)
            val numberB = matchB.group(2)
            if (nameA != nameB) {
              nameA < nameB
            } else {
              numberA.toInt <= numberB.toInt
            }

          } else if (matchA.hasNext) {
            val nameA = matchA.group(1)
            // only a matches the regex
            nameA < b._2

          } else if (matchB.hasNext) {
            val nameB = matchB.group(1)
            // only b matches the regex
            a._2 < nameB

          } else {
            // none matches the regex
            a._2 < b._2
          }
      }

      sortedGroups
  }

  /**
   * Retrieve an group from id.
   */
  def findById(id: Long): Option[Group] = {
    DB.withConnection {
      implicit connection =>
        Cache.getOrElse[Option[Group]](keyCacheById + id) {
          SQL("select * from environment_group where groupId = {id}").on('id -> id).as(Group.simple.singleOpt)
        }
    }
  }

  /**
   * Retrieve an group from it's name .
   */
  def findByName(name: String): Option[Group] = DB.withConnection {
    implicit connection =>
      Cache.getOrElse[Option[Group]](keyCacheByName + name) {
        SQL("select * from environment_group where groupName = {name}").on('name -> name).as(Group.simple.singleOpt)
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
    // Insert the new group
    DB.withConnection {
      implicit connection =>
        SQL( """	insert into environment_group values (null, {name})""").on('name -> group.name).executeUpdate()
    }
  }

  /**
   * Update a group.
   *
   * @param group The group group.
   */
  def update(group: Group) = {
    clearCache
    Cache.remove(keyCacheById + group.id)
    DB.withConnection {
      implicit connection =>
        SQL( """update environment_group set name = {name} where id = {id}""").on(
          'id -> group.id,
          'name -> group.name
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
        SQL("delete from environment_group where id = {id}").on('id -> group.id).executeUpdate()
    }
  }

  /**
   * remove the all-options cache
   */
  def clearCache() {
    Cache.remove(keyCacheAllOptions)
  }

  /**
   * Return a list of all groups.
   *
   */
  def allGroups: List[Group] = {
    DB.withConnection {
      implicit connection =>

        val groups = SQL(
          """
							select * from environment_group
							order by environment_group.groupName
          							""").as(Group.simple *)

        groups
    }
  }

}


