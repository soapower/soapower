package models

import anorm._
import anorm.SqlParser._
import play.api._
import play.api.Play.current
import play.api.cache._
import play.api.db._

case class MockGroup(id: Long, name: String, groupId: Long)

object MockGroup {

  val ID_DEFAULT_NO_MOCK_GROUP = Long.box(1)

  /**
   * Mockgroup caches keys which are used in order to declare and manage a DB cache
   */
  private val keyCacheAllOptions = "mockgroup-options"
  private val keyCacheById = "mockgroup-by-id"
  private val keyCacheByName = "mockgroup-by-name"
  private val MOCKGROUP_NAME_PATTERN = "[a-zA-Z0-9]{1,200}"

  /**
   * SQL anorm row parser. This operation indicate how to parse a sql row.
   */
  val simple = {
    get[Long]("mock_group.id") ~
      get[String]("mock_group.name") ~
      get[Long]("mock_group.groupId") map {
      case id ~ name ~ groupId
      => MockGroup(id, name, groupId)
    }
  }

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "name" -> 2, "groupName" -> 3)

  val csvKey = "mockgroup"

  /**
   * Csv format.
   */
  val csv = {
    get[Long]("mock_group.id") ~
      get[String]("mock_group.name") ~
      get[String]("groups.name") map {
      case id ~ name ~ groupName =>
        id + ";" + name + ";" + groupName + "\n"
    }
  }


  /**
   * Get all mockgroups from the database in csv format
   * @return List of mockgroups, csv format
   */
  def fetchCsv(): List[String] = DB.withConnection {
    implicit c => SQL("select * from mock_group left join groups on mock_group.groupId = groups.id").as(MockGroup.csv *)
  }


  /**
   * Construct the Map[String,String] needed to fill a select options set.
   */
  def options: Seq[(String, String)] = DB.withConnection {
    implicit connection =>
      val mockgroups = Cache.getOrElse[Seq[(String, String)]](keyCacheAllOptions) {
        Logger.debug("Mockgroup not found in cache: loading from db")
        SQL("select * from mock_group order by name").as(MockGroup.simple *).map(c => c.id.toString -> c.name)
      }
      mockgroups
  }

  /**
   * Retrieve an MockGroup from id.
   */
  def findById(id: Long): Option[MockGroup] = {
    DB.withConnection {
      implicit connection =>
        Cache.getOrElse[Option[MockGroup]](keyCacheById + id) {
          SQL("select * from mock_group where id = {id}").on('id -> id).as(MockGroup.simple.singleOpt)
        }
    }
  }

  /**
   * Retrieve an MockGroup from it's name .
   */
  def findByName(name: String): Option[MockGroup] = DB.withConnection {
    implicit connection =>
      Cache.getOrElse[Option[MockGroup]](keyCacheByName + name) {
        SQL("select * from mock_group where name = {name}").on('name -> name).as(MockGroup.simple.singleOpt)
      }
  }

  /**
   * Persist a new MockGroup to database.
   *
   * @param mockGroup The MockGroup to persist.
   */
  def insert(mockGroup: MockGroup) = {
    // Clear the cache in order to ???
    clearCache

    if (!mockGroup.name.trim.matches(MOCKGROUP_NAME_PATTERN)) {
      throw new Exception("MockGroup name invalid:" + mockGroup.name.trim)
    }

    // Insert the new MockGroup
    if (options.exists {
      g => g._2.equals(mockGroup.name.trim)
    }) {
      throw new Exception("MockGroup with name " + mockGroup.name.trim + " already exist")
    }

    DB.withConnection {
      implicit connection =>
        SQL(
          """	insert into mock_group (id, name, groupId)
            values (null, {name}, {groupId}
            )
          """
        ).on(
            'name -> mockGroup.name.trim,
            'groupId -> mockGroup.groupId
          ).executeUpdate()
    }
  }

  /**
   * Update a mockgroup.
   *
   * @param mockGroup The mockgroup mockgroup.
   */
  def update(mockGroup: MockGroup) = {
    clearCache

    if (!mockGroup.name.trim.matches(MOCKGROUP_NAME_PATTERN)) {
      throw new Exception("Mockgroup name invalid:" + mockGroup.name.trim)
    }

    if (options.exists {
      g => g._2.equals(mockGroup.name.trim) && g._1.toLong != mockGroup.id
    }) {
      throw new Exception("Mockgroup with name " + mockGroup.name.trim + " already exist")
    }

    Cache.remove(keyCacheById + mockGroup.id)
    DB.withConnection {
      implicit connection =>
        SQL("update mock_group set name = {name}, groupId = {groupId} " +
          " where id = {id}").on(
            'id -> mockGroup.id,
            'name -> mockGroup.name.trim,
            'groupId -> mockGroup.groupId
          ).executeUpdate()
    }
  }

  /**
   * Delete a mockgroup.
   *
   * @param mockGroup mockgroup to delete
   */
  def delete(mockGroup: MockGroup) = {
    clearCache
    Cache.remove(keyCacheById + mockGroup.id)
    DB.withConnection {
      implicit connection =>
        SQL("delete from mock_group where id = {id}").on('id -> mockGroup.id).executeUpdate()
    }
  }

  /**
   * Remove the all-options cache.
   */
  def clearCache() {
    Cache.remove(keyCacheAllOptions)
  }

  /**
   * Return a list of all mockgroups.
   */
  def findAll: List[MockGroup] = {
    DB.withConnection {
      implicit connection =>
        val mockGroups = SQL(
          " select * from mock_group " +
            " order by mock_group.name"
        ).as(MockGroup.simple *)
        mockGroups
    }
  }

  def list(groupName: String): List[MockGroup] = {

    DB.withConnection {
      implicit connection =>

        val mockGroups = SQL(
          """
          select mock_group.id, mock_group.name, mock_group.groupId
          from mock_group, groups
          where mock_group.groupId = groups.id
          and (groups.name = {groupName}
          or mock_group.id = {defaultMockGroudId})
          order by mock_group.name
          """
        ).on(
            'groupName -> groupName,
            'defaultMockGroudId -> ID_DEFAULT_NO_MOCK_GROUP
          ).as(MockGroup.simple *)

        mockGroups
    }
  }

  /**
   * Check if group exist and insert it if not
   *
   * @param mockGroupName Name of the Mock Group
   * @return mockGroup
   */
  def upload(mockGroupName: String, groupId: Long): MockGroup = {
    Logger.debug("mockGroupName:" + mockGroupName)

    var mockGroup = findByName(mockGroupName)
    if (mockGroup == None) {
      Logger.debug("Insert group " + mockGroupName)
      insert(new MockGroup(0, mockGroupName, groupId))
      mockGroup = findByName(mockGroupName)
      if (mockGroup.get == null) Logger.error("MockGroup insert failed : " + mockGroupName)
    } else {
      Logger.debug("MockGroup already exist : " + mockGroup.get.name)
    }
    mockGroup.get
  }
}
