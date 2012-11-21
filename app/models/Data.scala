package models

import play.api.db._
import play.api.Play.current
import play.api._

import anorm._
import anorm.SqlParser._


case class Data (
    id: Pk[Long], 
    localUrl: String,
    remoteUrl: String,
    request: String,
    reponse: String, 
    timems: String,
    state: String
)

object Data {
    
}