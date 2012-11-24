package models

import play.api.db._
import play.api.cache._
import play.api.Play.current
import play.api._

import anorm._
import anorm.SqlParser._
import scala.collection.mutable.{Map, HashMap}

case class Service (
    id: Pk[Long], 
    description: String,
    localTarget: String, 
    remoteTarget: String,
    timeoutms: Long,
    user: Option[String],
    password: Option[String], 
    environmentId: Option[Long]
)

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
    get[Long]("service.timeoutms") ~
    get[Option[String]]("service.user") ~
    get[Option[String]]("service.password") ~
    get[Option[Long]]("service.environment_id") map {
      case id~description~localTarget~remoteTarget~timeoutms~user~password~environmentId => 
        Service(id, description, localTarget, remoteTarget, timeoutms, user, password, environmentId)
    }
  }
  
  /**
   * 2 Caches : 
   *   - one with key "environmentName + localTarget" and value : service
   *     -> fill in findByLocalTargetAndEnvironmentName
   *     -> clear in update and delete
   *   - a other with key cacheKey + service.id and value : environmentName + localTarget
   *     -> used to update and delete from first cache
   */
  private val cacheKey = "servicekey-"

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
    val serviceKey = localTarget + environmentName
    val service = Cache.getOrElse[Option[Service]](serviceKey) {
      Logger.debug("Service for " + environmentName + localTarget + " not found in cache: loading from db")
    
      var serviceInDb = DB.withConnection { implicit connection =>
        SQL(
          """
          select * from service
            left join environment on service.environment_id = environment.id
            where service.localTarget like {localTarget}
            and environment.name like {environmentName}
          """).on(
          'localTarget -> localTarget,
          'environmentName -> environmentName
        ).as(Service.simple.singleOpt
        )
      }
      if (serviceInDb isDefined) {
        Cache.set(cacheKey + serviceInDb.get.id, serviceKey)
        Logger.debug("Service for " + environmentName + localTarget + " put in cache")
      }
      serviceInDb
    }
    
    return service
  }

  /**
   * Insert a new service.
   *
   * @param service The service values.
   */
  def insert(service: Service) = {
    try {
      DB.withConnection { implicit connection =>
        SQL(
          """
            insert into service 
              (id, description, localTarget, remoteTarget, timeoutms, user, password, environment_id) values (
              (select next value for service_seq), 
              {description}, {localTarget}, {remoteTarget}, {timeoutms}, {user}, {password}, {environment_id}
            )
          """
        ).on(
          'description -> service.description,
          'localTarget -> service.localTarget,
          'remoteTarget -> service.remoteTarget,
          'timeoutms -> service.timeoutms,
          'user -> service.user,
          'password -> service.password,
          'environment_id -> service.environmentId
        ).executeUpdate()
      }

    } catch {
      //case e:SQLException => Logger.error("Database error")
      //case e:MalformedURLException => Logger.error("Bad URL")
      case e:Exception => Logger.error("Caught an exception! ", e)
    }
  }


  /**
   * Update a service.
   *
   * @param id The service id
   * @param service The service values.
   */
  def update(id: Long, service: Service) = {
    deleteFromCache(id)
    DB.withConnection { implicit connection =>
      SQL(
        """
          update service
          set description = {description}, 
          localTarget = {localTarget}, 
          remoteTarget = {remoteTarget}, 
          timeoutms = {timeoutms}, 
          user = {user}, 
          password = {password}, 
          environment_id = {environment_id} 
          where id = {id}
        """
      ).on(
        'id -> id,
        'description -> service.description,
        'localTarget -> service.localTarget,
        'remoteTarget -> service.remoteTarget,
        'timeoutms -> service.timeoutms,
        'user -> service.user,
        'password -> service.password,
        'environment_id -> service.environmentId
      ).executeUpdate()
    }
  }

  private def deleteFromCache(id: Long) {
    val serviceKey = Cache.get(cacheKey + id);
    if (serviceKey isDefined) {
      Logger.debug("remove " + serviceKey.get.toString + " from cache")
      Cache.remove(serviceKey.get.toString)
      Cache.remove(cacheKey + id)
    }
  }

  /**
   * Delete a service.
   *
   * @param id Id of the service to delete.
   */
  def delete(id: Long) = {
    deleteFromCache(id)
    DB.withConnection { implicit connection =>
      SQL("delete from service where id = {id}").on('id -> id).executeUpdate()
    }
  }

  /**
   * Parse a (Service,Environment) from a ResultSet
   */
  val withEnvironment = Service.simple ~ (Environment.simple ?) map {
    case service~environment => (service, environment)
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