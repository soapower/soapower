package models

import play.api.db._
import play.api.Play.current
import play.api._

import anorm._
import anorm.SqlParser._

//case class Data(localTarget : String, remoteTarget : String, request : String) 

case class Data (
    id: Pk[Long], 
    localTarget: String,
    remoteTarget: String,
    request: String,
    var response: String, 
    var timems: String,
    var state: String
) {
	var result : play.api.libs.ws.Response = null

    def this(localTarget : String, remoteTarget : String, request : String) = 
        this(NotAssigned, localTarget, remoteTarget, request, "", "", "");
}

object Data {
    
}