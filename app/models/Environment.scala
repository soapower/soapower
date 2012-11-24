package models

import play.api.db._
import play.api.Play.current
import play.api.cache._
import play.api._

import anorm._
import anorm.SqlParser._

case class Environment(id: Pk[Long], name: String)

object Environment {

  val keyCacheAll = "environment-options"

 /**
   * Parse a Environment from a ResultSet
   */
  val simple = {
    get[Pk[Long]]("environment.id") ~
    get[String]("environment.name") map {
      case id~name => Environment(id, name)
    }
  }

  /**
   * Construct the Map[String,String] needed to fill a select options set.
   */
  def options: Seq[(String, String)] = DB.withConnection { implicit connection =>
    Cache.getOrElse[Seq[(String,String)]](keyCacheAll) {
      Logger.debug("Environments not found in cache: loading from db")
      SQL("select * from environment order by name").as(Environment.simple *).map(c => c.id.toString -> c.name)
    }
  }

 /**
   * Retrieve an Environment from id.
   */
  def findById(id: Long): Option[Environment] = {
    DB.withConnection { implicit connection =>
      SQL("select * from environment where id = {id}").on(
        'id -> id
      ).as(Environment.simple.singleOpt)
    }
  } 

  /**
   * Insert a new environment.
   *
   * @param environment The environment values.
   */
  def insert(environment: Environment) = {
      Cache.remove(keyCacheAll)
      DB.withConnection { implicit connection =>
        SQL(
          """
            insert into environment values (
              (select next value for environment_seq), 
              {name}
            )
          """
        ).on(
          'name -> environment.name
        ).executeUpdate()
      }
  }


  /**
   * Update a environment.
   *
   * @param id The environment id
   * @param environment The environment values.
   */
  def update(id: Long, environment: Environment) = {
    Cache.remove(keyCacheAll)
    DB.withConnection { implicit connection =>
      SQL(
        """
          update environment
          set name = {name}
          where id = {id}
        """
      ).on(
        'id -> id,
        'name -> environment.name
      ).executeUpdate()
    }
  }

  /**
   * Delete a environment.
   *
   * @param id Id of the environment to delete.
   */
  def delete(id: Long) = {
    Cache.remove(keyCacheAll)
    DB.withConnection { implicit connection =>
      SQL("delete from environment where id = {id}").on('id -> id).executeUpdate()
    }
  }

   /**
   * Return a page of (Environment).
   *
   * @param page Page to display
   * @param pageSize Number of environments per page
   * @param orderBy Environment property used for sorting
   * @param filter Filter applied on the name column
   */
  def list(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%"): Page[(Environment)] = {
    
    val offest = pageSize * page
    
    DB.withConnection { implicit connection =>
      
      val environments = SQL(
        """
          select * from environment
          where environment.name like {filter}
          order by {orderBy} nulls last
          limit {pageSize} offset {offset}
        """
      ).on(
        'pageSize -> pageSize, 
        'offset -> offest,
        'filter -> filter,
        'orderBy -> orderBy
      ).as(Environment.simple *)

      val totalRows = SQL(
        """
          select count(*) from environment
          where environment.name like {filter}
        """
      ).on(
        'filter -> filter
      ).as(scalar[Long].single)

      Page(environments, page, offest, totalRows)
      
    }
    
  }
}