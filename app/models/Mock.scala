package models

import play.api.db._
import play.api.Play.current
import play.api.cache._
import play.api._

import anorm._
import anorm.SqlParser._
import org.apache.http.HttpStatus

case class Mock(id: Long,
                name: String,
                mockGroupId: Long = 1, // 1 is default group
                description: String,
                timeoutms: Int = 0,
                httpStatus: Int = 0,
                criteria: String,
                response: String
                 )

object Mock {

  private val keyCacheAllOptions = "mock-options"
  private val keyCacheById = "mock-all"

  /**
   * Parse a Mock from a ResultSet
   */
  val simple = {
    get[Long]("mock.id") ~
      get[String]("mock.name") ~
      get[Long]("mock.mockGroupId") ~
      get[String]("mock.description") ~
      get[Int]("mock.timeoutms") ~
      get[Int]("mock.httpStatus") ~
      get[String]("mock.criteria") ~
      get[String]("mock.response") map {
      case id ~ name ~ mockGroupId ~ description ~ timeoutms ~ httpStatus ~ criteria ~ response
      => Mock(id, name, mockGroupId, description, timeoutms, httpStatus, criteria, response)
    }
  }

  /**
   * Parse a Mock Light from a ResultSet
   */
  val simpleWithoutResponse = {
    get[Long]("mock.id") ~
      get[String]("mock.name") ~
      get[Long]("mock.mockGroupId") ~
      get[String]("mock.description") ~
      get[Int]("mock.timeoutms") ~
      get[Int]("mock.httpStatus") ~
      get[String]("mock.criteria") map {
      case id ~ name ~ mockGroupId ~ description ~ timeoutms ~ httpStatus ~ criteria
      => Mock(id, name, mockGroupId, description, timeoutms, httpStatus, criteria, null)
    }
  }

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "name" -> 2, "groupName" -> 3, "description" -> 4, "timeoutms" -> 5, "httpStatus" -> 6, "criteria" -> 7, "response" -> 8)

  val csvKey = "mock"

  /**
   * Csv format.
   */
  val csv = {
    get[Pk[Long]]("mock.id") ~
      get[String]("mock.name") ~
      get[String]("groups.name") ~
      get[Int]("mock.description") ~
      get[Int]("mock.timeoutms") ~
      get[Int]("mock.httpStatus") ~
      get[String]("mock.criteria") ~
      get[String]("mock.response") map {
      case id ~ name ~ groupName ~ description ~ timeoutms ~ httpStatus ~ criteria ~ response =>
        id + ";" + name + ";" + groupName + ";" + description + ";" + timeoutms + ";" + httpStatus + ";" + criteria + ";" + response + "\n"
    }
  }

  /**
   * Get All mocks, csv format.
   * @return List of Environements, csv format
   */
  def fetchCsv(): List[String] = DB.withConnection {
    implicit c => SQL("select * from mock left join groups on mock.mockGroupId = mock_group.id").as(Mock.csv *)
  }

  /**
   * Construct the Map[String,String] needed to fill a select options set.
   */
  private def optionsAll: Seq[(String, String)] = DB.withConnection {
    implicit connection =>
      val envs = Cache.getOrElse[Seq[(String, String)]](keyCacheAllOptions) {
        Logger.debug("Mocks not found in cache: loading from db")
        SQL("select * from mock order by name").as(Mock.simple *).map(c => c.id.toString -> c.name)
      }
      envs
  }

  /**
   * Retrieve an Mock from id.
   */
  def findById(id: Long): Option[Mock] = {
    DB.withConnection {
      implicit connection =>
        Cache.getOrElse[Option[Mock]](keyCacheById + id) {
          SQL("select * from mock where id = {id}").on(
            'id -> id).as(Mock.simple.singleOpt)
        }
    }
  }

  /**
   * Retrieve an Mock from name.
   */
  def findByName(name: String): Option[Mock] = DB.withConnection {
    implicit connection =>
    // FIXME : add key to clearCache
    //Cache.getOrElse[Option[Mock]](keyCacheByName + name) {
      SQL("select * from mock where name = {name}").on(
        'name -> name).as(Mock.simple.singleOpt)
    //}
  }

  /**
   * Insert a new mock.
   *
   * @param mock The mock values.
   */
  def insert(mock: Mock) = {

    if (optionsAll.exists {
      e => e._2.equals(mock.name.trim)
    }) {
      throw new Exception("Mock with name " + mock.name.trim + " already exist")
    }

    DB.withConnection {
      implicit connection =>
        SQL(
          """
            insert into mock (id, name, description, timeoutms, httpStatus, criteria, response, mockGroupId)
              values (null, {name}, {description}, {timeoutms}, {httpStatus}, {criteria}, {response}, {mockGroupId})
          """).on(
            'name -> mock.name.trim,
            'description -> mock.description,
            'timeoutms -> mock.timeoutms,
            'httpStatus -> mock.httpStatus,
            'criteria -> mock.criteria,
            'response -> mock.response,
            'mockGroupId -> mock.mockGroupId
          ).executeUpdate()
    }
    clearCache
  }

  /**
   * Update a mock.
   *
   * @param mock The mock values.
   */
  def update(mock: Mock) = {

    if (optionsAll.exists {
      e => e._2.equals(mock.name.trim) && e._1.toLong != mock.id
    }) {
      throw new Exception("Mock with name " + mock.name.trim + " already exist")
    }

    Cache.remove(keyCacheById + mock.id)
    DB.withConnection {
      implicit connection =>
        SQL(
          """
          update mock
          set name = {name},
          description = {description},
          timeoutms = {timeoutms},
          httpStatus = {httpStatus},
          criteria = {criteria},
          response = {response},
          mockGroupId = {mockGroupId}
          where id = {id}
          """).on(
            'id -> mock.id,
            'name -> mock.name.trim,
            'description -> mock.description,
            'timeoutms -> mock.timeoutms,
            'httpStatus -> mock.httpStatus,
            'criteria -> mock.criteria,
            'response -> mock.response,
            'mockGroupId -> mock.mockGroupId
          ).executeUpdate()
    }
    clearCache
  }

  /**
   * Delete a mock.
   *
   * @param id Id of the mock to delete.
   */
  def delete(id: Long) = {
    Cache.remove(keyCacheById + id)
    DB.withConnection {
      implicit connection =>
        SQL("delete from mock where id = {id}").on('id -> id).executeUpdate()
    }
    clearCache
  }

  def clearCache() {
    Cache.remove(keyCacheAllOptions)
  }

  /**
   * Return a list of all Mock which are contained into the given group
   *
   */
  def list(mockGroup: String): List[Mock] = {

    DB.withConnection {
      implicit connection =>

        val mocks = SQL(
          """
          select mock.id, mock.name, mock.mockGroupId, mock.description, mock.timeoutms, mock.httpStatus, mock.criteria
          from mock, mock_group
          where mock.mockGroupId = mock_group.id
          and mock_group.name = {mockGroup}
          order by mock.mockGroupId asc, mock.name
          """).on('mockGroup -> mockGroup).as(Mock.simpleWithoutResponse *)

        mocks
    }
  }

  /**
   * Return a list of all Mock
   *
   */
  def list(): List[Mock] = {

    DB.withConnection {
      implicit connection =>

        val mocks = SQL(
          """
          select id, name, mockGroupId, description, timeoutms, criteria from mock
          order by mock.name
          """).as(Mock.simpleWithoutResponse *)

        mocks
    }
  }

  /**
   * Upload a csvLine => insert mock.
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
      val mockGroup = MockGroup.upload(dataCsv(csvTitle.get("mockGroupName").get), Group.ID_DEFAULT_GROUP)
      uploadMock(dataCsv, mockGroup)
    } else {
      Logger.info("Line does not match with " + csvKey + " of csvLine - ignored")
    }
  }

  /**
   * Check if mock already exist (with same name). Insert or do nothing if exist.
   *
   * @param dataCsv line in csv file
   * @param mockGroup mock's mockGroup
   * @return mock (new or not)
   */
  private def uploadMock(dataCsv: Array[String], mockGroup: MockGroup) = {

    val name = dataCsv(csvTitle.get("name").get)
    val s = findByName(name)

    s.map {
      mock =>
        Logger.warn("Warning : Mock " + mock.name + " already exist")
        throw new Exception("Warning : Mock " + mock.name + " already exist")
    }.getOrElse {
      val mock = new Mock(
        -1,
        dataCsv(csvTitle.get("name").get).trim,
        mockGroup.id,
        dataCsv(csvTitle.get("description").get).trim,
        dataCsv(csvTitle.get("timeoutms").get).toInt,
        dataCsv(csvTitle.get("httpStatus").get).toInt,
        dataCsv(csvTitle.get("criteria").get).trim,
        dataCsv(csvTitle.get("response").get).trim
      )
      Mock.insert(mock)
      Logger.info("Insert Mock " + mock.name)
    }
  }

  /**
   * Retrieve an Mock from name.
   */
  def findByMockGroupAndContent(mockGroupId: Long, requestBody: String): Mock = DB.withConnection {
    implicit connection =>

      val mocks: List[Mock] = SQL("select * from mock where mockGroupId = {mockGroupId}").on(
        'mockGroupId -> mockGroupId).as(Mock.simple *)

      // for each mocks in group, find the first eligible
      var ret: Mock = null
      mocks.takeWhile(_ => ret == null).foreach(mock =>
        if (mock.criteria.trim().equals("*") || requestBody.contains(mock.criteria)) {
          ret = mock
        }
      )

      if (ret == null) {
        ret = new Mock(-1, "mockNotFoundName", -1,
          "mockNotFoundDescription", 0, HttpStatus.SC_INTERNAL_SERVER_ERROR, "noCriteria",
          "Error getting Mock with mockGroupId " + mockGroupId
        )
      }
      ret
  }

}
