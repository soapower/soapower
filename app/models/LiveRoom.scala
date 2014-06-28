package models

import akka.actor._
import scala.concurrent.duration._

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import akka.util.Timeout
import akka.pattern.ask

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

object Robot {

  var liveRoom: ActorRef = null

  def apply(room: ActorRef) {
    liveRoom = room

    // Create an Iteratee that logs all messages to the console.
    val loggerIteratee = Iteratee.foreach[JsValue](event => Logger("robot").info(event.toString))

    implicit val timeout = Timeout(1 second)
    // Make the robot join the room
    liveRoom ? (Join("Robot", null.asInstanceOf[Criterias])) map {
      case Connected(robotChannel) =>
        // Apply this Enumerator on the logger.
        robotChannel |>> loggerIteratee
    }
  }

  def talkMsg(msg: String, typeMsg: String) {
    Akka.system.scheduler.scheduleOnce(
      0 seconds,
      liveRoom,
      Talk("Robot", msg, typeMsg)
    )
  }

  def talk(requestData: RequestData) {
    Akka.system.scheduler.scheduleOnce(
      0 seconds,
      liveRoom,
      TalkRequestData("Robot", requestData)
    )
  }
}

object LiveRoom {

  implicit val timeout = Timeout(1 second)

  lazy val default = {
    val roomActor = Akka.system.actorOf(Props[LiveRoom])

    // Create a bot user (just for fun)
    Logger.info("Init Robot Live Room")
    Robot(roomActor)

    roomActor
  }

  def init {
    Logger.info("Init LiveRoom")
    default
  }

  def changeCriterias(username: String, criteria: Tuple2[String, String]) = {
    default ! ChangeCriterias(username, criteria)
  }

  def join(username: String, criterias: Criterias): scala.concurrent.Future[(Iteratee[JsValue, _], Enumerator[JsValue])] = {

    (default ? Join(username, criterias)).map {
      case Connected(enumerator) =>
        // Create an Iteratee to consume the feed
        val iteratee = Iteratee.foreach[JsValue] {
          event =>
            default ! Talk(username, (event \ "text").as[String], "talk")
        }.map {
          _ =>
            default ! Quit(username)
        }
        (iteratee, enumerator)

      case CannotConnect(error) =>
        // Connection error
        // A finished Iteratee sending EOF
        val iteratee = Done[JsValue, Unit]((), Input.EOF)
        // Send an error and close the socket
        val enumerator = Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumerator.enumInput(Input.EOF))

        (iteratee, enumerator)
    }
  }
}

class LiveRoom extends Actor {

  var members = Map.empty[String, (Criterias, Concurrent.Channel[JsValue])]

  def receive = {

    case Join(username, criterias) => {
      if (username == "Robot") {
        members = members + ((username, (criterias, null.asInstanceOf[Concurrent.Channel[JsValue]])))
      }
      else {
        if (members.contains(username)) {
          sender ! CannotConnect("You have already a navigator on this page !")
        } else {
          val e = Concurrent.unicast[JsValue] {
            c =>
              members = members + ((username, (criterias, c)))
          }
          sender ! Connected(e)
          self ! NotifyJoin(username)
        }
      }
    }

    case Quit(username) => {
      members = members - username
      notifyAll("quit", username, "has left the room")
    }

    case NotifyJoin(username) => {
      notifyAll("join", username, "has entered the room")
    }

    case Talk(username, text, typeMsg) => {
      notifyAll(typeMsg, username, text)
    }

    case TalkRequestData(username, requestData) => {
      notifyAll("talkRequestData", username, requestData)
    }

    case ChangeCriterias(username, criteria) => {
      if (members.get(username).isDefined) {
        // We retrieve the channel
        val channel = members.get(username).get._2
        // We retrieve the old criterias
        val criterias = members.get(username).get._1

        val newCriterias = criteria._1 match {
          // Create new criterias based on user choice
          case "group" =>
            new Criterias(criteria._2, criterias.environment, criterias.serviceAction, criterias.code, criterias.search, criterias.request, criterias.response)
          case "environment" =>
            new Criterias(criterias.group, criteria._2, criterias.serviceAction, criterias.code, criterias.search, criterias.request, criterias.response)
          case "serviceAction" =>
            new Criterias(criterias.group, criterias.environment, criteria._2, criterias.code, criterias.search, criterias.request, criterias.response)
          case "code" =>
            new Criterias(criterias.group, criterias.environment, criterias.serviceAction, criteria._2, criterias.search, criterias.request, criterias.response)
          case "search" =>
            new Criterias(criterias.group, criterias.environment, criterias.serviceAction, criterias.code, criteria._2, criterias.request, criterias.response)
          case "request" =>
            new Criterias(criterias.group, criterias.environment, criterias.serviceAction, criterias.code, criterias.search, criteria._2.toBoolean, criterias.response)
          case "response" =>
            new Criterias(criterias.group, criterias.environment, criterias.serviceAction, criterias.code, criterias.search, criterias.request, criteria._2.toBoolean)
        }

        members = members - username
        members = members + ((username, (newCriterias, channel)))
      }
    }

  }

  def notifyAll(kind: String, user: String, requestData: RequestData) {
    var usernames = Set.empty[String]
    members.foreach {
      mem =>
        usernames = usernames + mem._1
    }
    // Create the JSON that will be sent
    val msg = JsObject(
      Seq(
        "kind" -> JsString(kind),
        "user" -> JsString(user),
        "message" -> requestData.toSimpleJson,
        "members" -> JsArray(
          usernames.toList.map(JsString)
        )
      )
    )
    // Iterate on each member of the room and check if their criteria match the incoming request
    // If the request data match the criterias, the msg is sent through the channel of the correct client
    members.foreach {
      case (key, value) =>
        if (key != "Robot" && requestData.checkCriterias(value._1)) value._2.push(msg)
    }
  }

  def notifyAll(kind: String, user: String, text: String) {
    var usernames = Set.empty[String]
    members.foreach {
      mem =>
        usernames = usernames + mem._1
    }

    val msg = JsObject(
      Seq(
        "kind" -> JsString(kind),
        "user" -> JsString(user),
        "message" -> JsString(text),
        "members" -> JsArray(
          usernames.toList.map(JsString)
        )
      )
    )
    members.foreach { case (key, value) =>
      if (key != "Robot") value._2.push(msg)
    }
  }

}

case class Join(username: String, criterias: Criterias)

case class Quit(username: String)

case class TalkRequestData(username: String, requestData: RequestData)

case class Talk(username: String, text: String, typeMsg: String)

case class NotifyJoin(username: String)

case class Connected(enumerator: Enumerator[JsValue])

case class CannotConnect(msg: String)

case class ChangeCriterias(username: String, criteria: (String, String))

case class Criterias(group: String, environment: String, serviceAction: String, code: String, search: String, request: Boolean, response: Boolean)