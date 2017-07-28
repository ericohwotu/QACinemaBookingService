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

  def getAllSeats(key: Option[String]) = Action { implicit request: Request[AnyContent] =>
    jsonApiHelper(key, request) match {
      case "Unauthorised" => Unauthorized("Sorry you are not authorised")
      case bookingKey => Ok(Json.parse(mongoDbController.getSeats(bookingKey)))
    }
  }

  def bookSeat(id: Int, key: Option[String]) = Action { implicit request: Request[AnyContent] =>
    val movieName = request.session.get("movieName").getOrElse("None")
    jsonApiHelper(key, request) match {
      case "Unauthorised" => Unauthorized("Sorry you are not authorised")
      case bookingKey =>
        mongoDbController.bookSeat(Seat(id,movieName,bookingKey,false,Seat.getExpiryDate))
        Ok(Json.parse(SeatGenerator.bookSeats(id, bookingKey)))
    }
  }

  def submitBooking(key: Option[String]): Action[AnyContent] = Action {request: Request[AnyContent] =>
    jsonApiHelper(key,request) match {
      case "Unauthorised" => Unauthorized("Sorry you are not authorised")
      case bookingKey =>
        mongoDbController.submitBooking(bookingKey)
        Ok("Submitting")
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
}
