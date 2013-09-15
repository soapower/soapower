package models

import play.api.db._
import play.api.Play.current
import play.api.cache._
import play.api._

import anorm._
import anorm.SqlParser._

case class Mock(id: Long,
                name: String,
                mockGroupId: Long = 1, // 1 is default group
                description: String,
                timeout: Int = 0,
                criterias: String,
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
      get[Int]("mock.timeout") ~
      get[String]("mock.criterias") ~
      get[String]("mock.response") map {
      case id ~ name ~ mockGroupId ~ description ~ timeout ~ criterias ~ response
      => Mock(id, name, mockGroupId, description, timeout, criterias, response)
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
      get[Int]("mock.timeout") ~
      get[String]("mock.criterias") map {
      case id ~ name ~ mockGroupId ~ description ~ timeout ~ criterias
      => Mock(id, name, mockGroupId, description, timeout, criterias, null)
    }
  }

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "name" -> 2, "groupName" -> 3, "description" -> 4, "timeout" -> 5, "criterias" -> 6, "response" -> 7)

  val csvKey = "mock"

  /**
   * Csv format.
   */
  val csv = {
    get[Pk[Long]]("mock.id") ~
      get[String]("mock.name") ~
      get[String]("groups.name") ~
      get[Int]("mock.description") ~
      get[Int]("mock.timeout") ~
      get[Int]("mock.criterias") ~
      get[Int]("mock.response") map {
      case id ~ name ~ groupName ~ description ~ timeout ~ criterias ~ response =>
        id + ";" + name + ";" + groupName + ";" + description + ";" + timeout + ";" + criterias + ";" + response + "\n"
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
            insert into mock (id, name, description, timeout, criterias, response, mockGroupId)
              values (null, {name}, {description}, {timeout}, {criterias}, {response}, {mockGroupId})
          """).on(
          'name -> mock.name.trim,
          'description -> mock.description,
          'timeout -> mock.timeout,
          'criterias -> mock.criterias,
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
          timeout = {timeout},
          criterias = {criterias},
          response = {response},
          mockGroupId = {mockGroupId}
          where id = {id}
          """).on(
          'id -> mock.id,
          'name -> mock.name.trim,
          'description -> mock.description,
          'timeout -> mock.timeout,
          'criterias -> mock.criterias,
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
          select mock.id, mock.name, mock.mockGroupId, mock.description, mock.timeout, mock.criterias
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
          select id, name, mockGroupId, description, timeout, criterias from mock
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
      val group = Environment.uploadGroup(dataCsv)
      uploadMock(dataCsv, group)
    } else {
      Logger.info("Line does not match with " + csvKey + " of csvLine - ignored")
    }
  }

  /**
   * Check if mock already exist (with same name). Insert or do nothing if exist.
   *
   * @param dataCsv line in csv file
   * @return mock (new or not)
   */
  private def uploadMock(dataCsv: Array[String], group: Group) = {

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
        group.id,
        dataCsv(csvTitle.get("description").get).trim,
        dataCsv(csvTitle.get("timeout").get).toInt,
        dataCsv(csvTitle.get("criterias").get).trim,
        dataCsv(csvTitle.get("response").get).trim
      )
      Mock.insert(mock)
      Logger.info("Insert Mock " + mock.name)
    }
  }
}
