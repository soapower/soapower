package models

import play.api.Play.current
import play.api.cache._
import reactivemongo.bson._
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import play.modules.reactivemongo.json.BSONFormats._
import play.api.libs.json.Json
import reactivemongo.bson.BSONInteger
import reactivemongo.bson.BSONString
import play.api.Logger
import org.apache.http.HttpStatus

case class Mock(_id: Option[BSONObjectID],
                name: String,
                description: String,
                timeoutms: Int = 0,
                httpStatus: Int = 0,
                httpHeaders: String,
                criteria: String,
                response: String,
                mockGroupName: Option[String]) {
  def this(mockDoc: BSONDocument, mockGroupName: Option[String]) =
    this(
      mockDoc.getAs[BSONObjectID]("_id"),
      mockDoc.getAs[String]("name").get,
      mockDoc.getAs[String]("description").get,
      mockDoc.getAs[Int]("timeoutms").get,
      mockDoc.getAs[Int]("httpStatus").get,
      mockDoc.getAs[String]("httpHeaders").get,
      mockDoc.getAs[String]("criteria").get,
      mockDoc.getAs[String]("response").get,
      mockGroupName)
}

case class Mocks(mocks: List[Mock])

object Mock {

  implicit val mockFormat = Json.format[Mock]
  implicit val mocksFormat = Json.format[Mocks]

  private val keyCacheAllOptions = "mock-options"
  private val keyCacheById = "mock-all"

  implicit object MocksBSONReader extends BSONDocumentReader[Mocks] {
    def read(doc: BSONDocument): Mocks = {
      if (doc.getAs[List[BSONDocument]]("mocks") isDefined) {
        val list = doc.getAs[List[BSONDocument]]("mocks").get.map(
          s => new Mock(s, doc.getAs[String]("name"))
        )
        Mocks(list)
      } else {
        Mocks(List())
      }
    }
  }

  implicit object MockBSONReader extends BSONDocumentReader[Mock] {
    def read(doc: BSONDocument): Mock = {
      val s = doc.getAs[List[BSONDocument]]("mocks").get.head
      new Mock(s, doc.getAs[String]("name"))
    }
  }

  implicit object MockBSONWriter extends BSONDocumentWriter[Mock] {
    def write(mock: Mock): BSONDocument =
      BSONDocument(
        "_id" -> mock._id,
        "name" -> BSONString(mock.name),
        "description" -> BSONString(mock.description),
        "timeoutms" -> BSONInteger(mock.timeoutms),
        "httpStatus" -> BSONInteger(mock.httpStatus),
        "httpHeaders" -> BSONString(mock.httpHeaders),
        "criteria" -> BSONString(mock.criteria),
        "response" -> BSONString(mock.response))
  }

  /**
   * Title of csvFile. The value is the order of title.
   */
  val csvTitle = Map("key" -> 0, "id" -> 1, "name" -> 2, "description" -> 3, "timeoutms" -> 4, "httpStatus" -> 5, "httpHeaders" -> 6, "criteria" -> 7, "response" -> 8, "groupName" -> 9)

  val csvKey = "mock"

  /**
   * Csv format of one mock.
   * @param m mock
   * @return csv format of the mock (String)
   */
  def csv(m: Mock) = {
    csvKey + ";" + m._id.get.stringify + ";" + m.name + ";" + m.description + ";" + m.timeoutms + ";" + m.httpStatus + ";" + m.httpHeaders + ";" + m.criteria + ";" + m.response + ";" + ";" + m.mockGroupName + "\n"
  }

  /**
   * Get All mock, csv format.
   * @return List of Mocks, csv format
   */
  def fetchCsv(): Future[List[String]] = {
    ???
    //TODO
    //findAll.map(mocks => mocks.map(m => csv(m)))
  }

  /**
   * Retrieve a Mock.
   * @param mockGroupName Name of mock Group
   * @param mockId ObjectID of mock
   * @return Option of mock
   */
  def findById(mockGroupName: String, mockId: String): Future[Option[Mock]] = {
    val query = BSONDocument("name" -> mockGroupName)
    val projection = BSONDocument("mocks" -> BSONDocument(
      "$elemMatch" -> BSONDocument("_id" -> BSONObjectID(mockId))))
    MockGroup.collection.find(query, projection).cursor[Mock].headOption
  }

  /**
   * Get all mocks for one mock Group
   * @param mockGroupName mockGroup Name wich contains mocks
   * @return Mocks Object with empty list of mock if there is no mock
   */
  def findAll(mockGroupName: String): Future[Option[Mocks]] = {
    val query = BSONDocument("name" -> mockGroupName)
    MockGroup.collection.find(query).cursor[Mocks].headOption
  }

  /**
   * Insert a new mock.
   *
   * @param mock The mock values.
   */
  def insert(mock: Mock) = {
    val selector = BSONDocument("name" -> mock.mockGroupName)
    val insert = BSONDocument("$push" -> BSONDocument("mocks" -> mock))
    MockGroup.collection.update(selector, insert)
  }

  /**
   * Update a mock.
   *
   * @param mock The mock values.
   */
  def update(mock: Mock) = {
    val selector = BSONDocument(
      "name" -> mock.mockGroupName,
      "mocks._id" -> mock._id
    )
    val update = BSONDocument("$set" -> BSONDocument("mocks.$" -> mock))
    MockGroup.collection.update(selector, update)
  }

  /**
   * Delete a mock.
   * @param mockGroupName Mock Group name wich contains the mock
   * @param mockId id of the mock to delete
   * @return
   */
  def delete(mockGroupName: String, mockId: String) = {
    val selector = BSONDocument("name" -> mockGroupName)
    val update = BSONDocument("$pull" -> BSONDocument("mocks" -> BSONDocument("_id" -> BSONObjectID(mockId))))
    MockGroup.collection.update(selector, update)
  }

  def clearCache() {
    Cache.remove(keyCacheAllOptions)
  }

  /**
   * Upload a csvLine => insert mock.
   *
   * @param csvLine line in csv file
   * @return nothing
   */
  def upload(csvLine: String) = {

    //TODO
    ???
    /*
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
    */
  }

  /**
   * Check if mock already exist (with same name). Insert or do nothing if exist.
   *
   * @param dataCsv line in csv file
   * @param mockGroup mock's mockGroup
   * @return mock (new or not)
   */
  private def uploadMock(dataCsv: Array[String], mockGroup: MockGroup) = {

    ???
    // TODO
    /*
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
        mockGroup._id.get.stringify,
        dataCsv(csvTitle.get("description").get).trim,
        dataCsv(csvTitle.get("timeoutms").get).toInt,
        dataCsv(csvTitle.get("httpStatus").get).toInt,
        dataCsv(csvTitle.get("httpHeaders").get).trim,
        dataCsv(csvTitle.get("criteria").get).trim,
        dataCsv(csvTitle.get("response").get).trim
      )
      Mock.insert(mock)
      Logger.info("Insert Mock " + mock.name)
    }
    */
  }

  /**
   * Retrieve an Mock from name.
   */
  def findByMockGroupAndContent(mockGroupId: BSONObjectID, requestBody: String): Mock = {
    Logger.debug("requestBody:" + requestBody)
    val query = BSONDocument("_id" -> mockGroupId)
    val mocksGroup = MockGroup.collection.find(query).cursor[Mocks].headOption.value

    //FIXME work with future

    var ret: Mock = null
    if (mocksGroup.isDefined && mocksGroup.get != null && mocksGroup.get.get.isDefined) {
      val mocks = mocksGroup.get.get
      mocks.get.mocks.takeWhile(_ => ret == null).foreach(mock =>
        if (mock.criteria.trim().equals("*") || requestBody.contains(mock.criteria)) {
          Logger.debug("Mock Found : " + mock._id.get.stringify)
          ret = mock
        }
      )
    }
    if (ret == null) {
      ret = new Mock(Some(BSONObjectID.generate), "mockNotFoundName", "mockNotFoundDescription", -1,
        0, HttpStatus.SC_INTERNAL_SERVER_ERROR.toString, "noCriteria", "no mock found in soapower",
        Some("Error getting Mock with mockGroupId " + mockGroupId)
      )
    }
    ret
  }
}
