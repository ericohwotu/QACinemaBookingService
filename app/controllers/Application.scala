package controllers

import play.api._
import play.api.mvc._
import javax.inject._

import play.api.data.format.Formats._
import play.api.i18n._
import business.SeatGenerator
import helpers.SessionHelper
import models.DateSelector
import play.api.data.{Form, Forms}
import play.api.data.Forms._
import play.api.libs.json.Json

class Application @Inject()(implicit val messagesApi: MessagesApi) extends Controller with I18nSupport{

  val seatsForm = Form[(Int, Int)](
    Forms.tuple(
      "bookingid" -> Forms.of[Int],
      "seatid" -> Forms.of[Int]
    )
  )

  def index(name: String) = Action { request: Request[AnyContent] =>
    Ok(views.html.index(name)(DateSelector.dsForm, SeatGenerator.getLayout(request.remoteAddress))).withSession(
      "sessionKey" -> SessionHelper.getSessionKey()
    )
  }

  def postIndex = Action(parse.form(seatsForm)) { implicit request =>
    SeatGenerator.seatHistory += request.body._2
    SeatGenerator.accessHistory += request.remoteAddress
    Ok(views.html.index("Booking")(DateSelector.dsForm, SeatGenerator.getLayout(request.remoteAddress)) )
  }




}