package models

import java.util.{ Date }

import play.api.db._
import play.api.Play.current
import play.api._

import anorm._
import anorm.SqlParser._

case class RequestData(
  id: Pk[Long],

  localTarget: String,
  remoteTarget: String,

  request: String,
  startTime: Date,

  var response: String,
  var timeInMillis: Long,
  var state: String)
