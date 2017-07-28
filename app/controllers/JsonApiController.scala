package controllers

import business.SeatGenerator
import play.api.libs.json.Json
import play.api.mvc._

class JsonApiController extends Controller{

  def getAllSeats = Action { implicit request: Request[AnyContent] =>
    Ok(Json.parse(SeatGenerator.getSeats(request.remoteAddress)))
  }

  def bookSeat(id: Int) = Action { implicit request: Request[AnyContent] =>
    Ok(Json.parse(SeatGenerator.bookSeats(id, request.remoteAddress)))
  }
}
