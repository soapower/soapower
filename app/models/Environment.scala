package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class Environment(id: Pk[Long], name: String)

object Environment {
    
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
  def options: Seq[(String,String)] = DB.withConnection { implicit connection =>
    SQL("select * from environment order by name").as(Environment.simple *).map(c => c.id.toString -> c.name)
  }

}