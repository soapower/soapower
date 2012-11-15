package models

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

case class Service (
    id: Pk[Long], 
    description: String,
    localTarget: String, 
    remoteTarget: String,
    environmentId: Option[Long]
)

/**
 * Helper for pagination.
 */
case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
  lazy val prev = Option(page - 1).filter(_ >= 0)
  lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
}

object Service {
  // -- Parsers
  
  /**
   * Parse a Service from a ResultSet
   */
  val simple = {
    get[Pk[Long]]("service.id") ~
    get[String]("service.description") ~
    get[String]("service.localTarget") ~
    get[String]("service.remoteTarget") ~
    get[Option[Long]]("service.environment_id") map {
      case id~description~localTarget~remoteTarget~environmentId => Service(id, description, localTarget, remoteTarget, environmentId)
    }
  }
  
  // -- Queries
    
  /**
   * Retrieve a Service from id.
   */
  def findById(id: Long): Option[Service] = {
    DB.withConnection { implicit connection =>
      SQL("select * from service where id = {id}").on(
        'id -> id
      ).as(Service.simple.singleOpt)
    }
  } 

  /**
   * Retrieve a Service from localTarget.
   */
  def findByLocalTargetAndEnvironmentName(localTarget: String, environmentName: String): Option[Service] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
        select * from service
          left join environment on service.environment_id = environment.id
          where service.localTarget like {localTarget}
          and environment.name like {environmentName}
        """).on(
        'localTarget -> localTarget,
        'environmentName -> environmentName
      ).as(Service.simple.singleOpt)
    }
  }

  /**
   * Insert a new service.
   *
   * @param service The service values.
   */
  def insert(service: Service) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into service values (
            (select next value for service_seq), 
            {description}, {localTarget}, {remoteTarget}, {environment_id}
          )
        """
      ).on(
        'description -> service.description,
        'localTarget -> service.localTarget,
        'remoteTarget -> service.remoteTarget,
        'environment_id -> service.environmentId
      ).executeUpdate()
    }
  }


  /**
   * Update a service.
   *
   * @param id The service id
   * @param service The service values.
   */
  def update(id: Long, service: Service) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          update service
          set description = {description}, localTarget = {localTarget}, remoteTarget = {remoteTarget}, environment_id = {environment_id} 
          where id = {id}
        """
      ).on(
        'id -> id,
        'description -> service.description,
        'localTarget -> service.localTarget,
        'remoteTarget -> service.remoteTarget,
        'environment_id -> service.environmentId
      ).executeUpdate()
    }
  }

  /**
   * Delete a service.
   *
   * @param id Id of the service to delete.
   */
  def delete(id: Long) = {
    DB.withConnection { implicit connection =>
      SQL("delete from service where id = {id}").on('id -> id).executeUpdate()
    }
  }

  /**
   * Parse a (Service,Environment) from a ResultSet
   */
  val withEnvironment = Service.simple ~ (Environment.simple ?) map {
    case service~environment => (service,environment)
  }

   /**
   * Return a page of (Service).
   *
   * @param page Page to display
   * @param pageSize Number of services per page
   * @param orderBy Service property used for sorting
   * @param filter Filter applied on the name column
   */
  def list(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%"): Page[(Service, Option[Environment])] = {
    
    val offest = pageSize * page
    
    DB.withConnection { implicit connection =>
      
      val services = SQL(
        """
          select * from service
          left join environment on service.environment_id = environment.id
          where service.description like {filter}
          order by {orderBy} nulls last
          limit {pageSize} offset {offset}
        """
      ).on(
        'pageSize -> pageSize, 
        'offset -> offest,
        'filter -> filter,
        'orderBy -> orderBy
      ).as(Service.withEnvironment *)

      val totalRows = SQL(
        """
          select count(*) from service 
          left join environment on service.environment_id = environment.id
          where service.description like {filter}
        """
      ).on(
        'filter -> filter
      ).as(scalar[Long].single)

      Page(services, page, offest, totalRows)
      
    }
    
  }

}