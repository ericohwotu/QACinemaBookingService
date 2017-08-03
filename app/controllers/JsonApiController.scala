package controllers

import javax.inject.Inject

import business.SeatGenerator
import models.Seat
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class JsonApiController @Inject()(val mongoDbController: MongoDbController) extends Controller {

  def getAllSeats(key: Option[String], name: Option[String], date: String, time: String) = Action { implicit request: Request[AnyContent] =>
    jsonApiHelper(key, request) match {
      case "Unauthorised" => Unauthorized("Sorry you are not authorised")
      case bookingKey =>
        mongoDbController.getSeatsBySlots(movieNameHelper(name, request), date, time) match {
          case None => BadRequest("No Seats Available")
          case jsonResult => Ok(Json.parse(mongoDbController.getJsonString(jsonResult.get, bookingKey)))
        }
    }
  }

  def bookSeat(id: Int, key: Option[String], name: Option[String], date: String, time: String) = Action { implicit request: Request[AnyContent] =>
    val movieName = request.session.get("movieName").getOrElse(name.getOrElse("None"))
    jsonApiHelper(key, request) match {
      case "Unauthorised" => Unauthorized("Sorry you are not authorised")
      case bookingKey =>
        mongoDbController.bookSeat(movieName, date, time, Seat(id, bookingKey, false, Seat.getExpiryDate,""))
        Ok(Json.parse(SeatGenerator.bookSeats(id, bookingKey)))
          .withSession(request.session + ("date" -> date) + ("time" -> time))
    }
  }

  def submitBooking(key: Option[String], name: Option[String], date: String, time: String): Action[AnyContent] =
    Action { request: Request[AnyContent] =>
      val movieName = request.session.get("movieName").getOrElse(name.getOrElse("None"))
      jsonApiHelper(key, request) match {
        case "Unauthorised" => Unauthorized("Sorry you are not authorised")
        case bookingKey =>
          mongoDbController.submitBooking(bookingKey,movieName,date,time)
          Redirect("http://www.facebook.com")
      }
    }

  def jsonApiHelper(key: Option[String], request: Request[AnyContent]): String = {
    request.session.get("sessionKey").getOrElse("") match {
      case "" => key match {
        case None => "Unauthorised"
        case apiKey =>
          mongoDbController.isKeyAvailable(apiKey.get) match {
            case true => apiKey.get
            case false => "Unauthorised"
          }
      }
      case sessionKey => sessionKey
    }
  }

  def movieNameHelper(name: Option[String], request: Request[AnyContent]): String = {
    request.session.get("movieName").getOrElse("") match {
      case "" => name match {
        case None => "Unauthorised"
        case movieName => movieName.get
      }
      case movieName => movieName
    }
  }
}
