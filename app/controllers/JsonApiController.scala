package controllers

import business.SeatGenerator
import play.api.libs.json.Json
import play.api.mvc._

class JsonApiController extends Controller{

  def getAllSeats(key: Option[String]) = Action { implicit request: Request[AnyContent] =>

    jsonApiHelper(key, request) match {
      case "Unauthorised" => Unauthorized("Sorry you are not authorised")
      case bookingKey => Ok(Json.parse(SeatGenerator.getSeats(bookingKey)))
    }


  }

  def bookSeat(id: Int, key: Option[String]) = Action { implicit request: Request[AnyContent] =>
    jsonApiHelper(key, request) match {
      case "Unauthorised" => Unauthorized("Sorry you are not authorised")
      case bookingKey => Ok (Json.parse (SeatGenerator.bookSeats (id, bookingKey) ) )
    }

  }

  def jsonApiHelper(key: Option[String], request: Request[AnyContent]): String ={
    request.session.get("sessionKey").getOrElse("") match {
      case "" => key match {
        case None => "Unauthorised"
        case apiKey =>
          apiKey.get
      }
      case sessionKey => sessionKey
    }
  }
}
