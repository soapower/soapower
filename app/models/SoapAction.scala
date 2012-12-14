package models

import play.api.db._
import play.api.Play.current
import play.api.cache._
import play.api._

import anorm._
import anorm.SqlParser._

case class SoapAction(id: Pk[Long], name: String, thresholdms: Long)

object SoapAction {
  // -- Parsers

  /**
   * Parse a SoapAction from a ResultSet
   */
  val simple = {
    get[Pk[Long]]("soapaction.id") ~
    get[String]("soapaction.name") ~
    get[Long]("soapaction.thresholdms") map {
    case id ~ name ~ thresholdms =>
        SoapAction(id, name, thresholdms)
    }
  }

  /**
   * Retrieve an SoapAction from id.
   */
  def findById(id: Long): Option[SoapAction] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from soapaction where id = {id}").on('id -> id).as(SoapAction.simple.singleOpt)
    }
  }

  /**
   * Retrieve an SoapAction from name.
   */
  def findByName(name: String): Option[SoapAction] = {
    DB.withConnection {
      implicit connection =>
        SQL("select * from soapaction where name = {name}").on('name -> name).as(SoapAction.simple.singleOpt)
    }
  }

	/**
	* Insert a new SoapAction.
	*
	* @param SoapAction The SoapAction values.
	*/
  def insert(soapAction: SoapAction) = {
    DB.withConnection {
      implicit connection =>
        SQL(
          """
            insert into soapaction values (
              (select next value for soapaction_seq), 
              {name},
              {thresholdms}
            )
          """).on(
          'name -> soapAction.name,
          'thresholdms -> soapAction.thresholdms).executeUpdate()
    }
  }

  /**
   * Update a SoapAction : update only threshold
   *
   * @param id The SoapAction id
   * @param SoapAction The SoapAction values.
   */
  def update(id: Long, soapAction: SoapAction) = {
    DB.withConnection {
      implicit connection =>
        SQL(
          """
          update soapaction
          set thresholdms = {thresholdms}
          where id = {id}
          """).on(
          'id -> id,
          'thresholdms -> soapAction.thresholdms).executeUpdate()
    }
  }

   /**
   * Return a page of (SoapAction).
   *
   * @param page Page to display
   * @param pageSize Number of soapactions per page
   * @param orderBy SoapAction property used for sorting
   * @param filter Filter applied on the name column
   */
  def list(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%"): Page[(SoapAction)] = {

    val offest = pageSize * page

    DB.withConnection {
      implicit connection =>

        val soapactions = SQL(
          """
          select * from soapaction
          where soapaction.name like {filter}
          order by {orderBy} nulls last
          limit {pageSize} offset {offset}
          """).on(
          'pageSize -> pageSize,
          'offset -> offest,
          'filter -> filter,
          'orderBy -> orderBy).as(SoapAction.simple *)

        val totalRows = SQL(
          """
          select count(*) from soapaction
          where soapaction.name like {filter}
          """).on(
          'filter -> filter).as(scalar[Long].single)

        Page(soapactions, page, offest, totalRows)

    }
  }
  
  def loadAll(): List[SoapAction] = {
    DB.withConnection { implicit connection =>
        SQL("select * from soapaction").as(SoapAction.simple *)
    }
  }

}