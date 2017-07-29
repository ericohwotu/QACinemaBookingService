package controllers

import play.api._
import play.api.mvc._
import javax.inject._

import play.api.data.format.Formats._
import play.api.i18n._
import business.SeatGenerator
import helpers.SessionHelper
import models.{DateSelector, Movie}
import play.api.data.{Form, Forms}
import play.api.data.Forms._
import play.api.libs.json.Json

class Application @Inject()(implicit val messagesApi: MessagesApi, val mongoDbController: MongoDbController) extends Controller with I18nSupport{

  val homePage = (name: String,request: Request[AnyContent]) =>
    Ok(views.html.index(name)(DateSelector.dsForm, SeatGenerator.getLayout(request.remoteAddress)))

  val seatsForm = Form[(Int, Int)](
    Forms.tuple(
      "bookingid" -> Forms.of[Int],
      "seatid" -> Forms.of[Int]
    )
  )

  def index(name: String) = Action { request: Request[AnyContent] =>
    //create movie database
    mongoDbController.isMovieInDb(name) match{
      case true => None
      case false =>
        mongoDbController.addMovie2Db(Movie.generateMovie(name))
    }

    //session and result
    request.session.get("sessionKey").getOrElse("") match {
      case "" => homePage(name,request).withSession("sessionKey" -> SessionHelper.getSessionKey(),"movieName"->name)
      case _ =>  homePage(name,request).withSession("movieName"->name)
    }
  }

  def postIndex = Action(parse.form(seatsForm)) { implicit request =>
    SeatGenerator.seatHistory += request.body._2
    SeatGenerator.sessionKeys += request.session.get("sessionKey").get
    Ok(views.html.index("Booking")(DateSelector.dsForm, SeatGenerator.getLayout(request.remoteAddress)))
  }




}