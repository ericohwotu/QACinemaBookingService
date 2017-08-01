package controllers

import java.util.concurrent.TimeUnit

import play.api._
import play.api.mvc._
import javax.inject._

import akka.actor.ActorSystem
import play.api.data.format.Formats._
import play.api.i18n._
import business.SeatGenerator
import helpers.SessionHelper
import models.{DateSelector, Movie, Seat}
import play.api.data.{Form, Forms}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.duration.Duration

@Singleton
class Application @Inject()(implicit val messagesApi: MessagesApi,
                            val mongoDbController: MongoDbController
                          ) extends Controller with I18nSupport{

  val undoBooking = ActorSystem("unbookingService")

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
    mongoDbController.isMovieInDb(name) match {
      case true =>
        //checkDbHelper
        None
      case false =>
        //checkDbHelper
        mongoDbController.addMovie2Db(Movie.generateMovie(name))
    }

    homePage(name,request).withSession("sessionKey" -> SessionHelper.getSessionKey(),"movieName"->name)
  }

  @Deprecated
  def postIndex = Action(parse.form(seatsForm)) { implicit request: Request[(Int,Int)] =>
    SeatGenerator.seatHistory += request.body._2
    SeatGenerator.sessionKeys += request.session.get("sessionKey").get
    Ok(views.html.index("Booking")(DateSelector.dsForm, SeatGenerator.getLayout(request.remoteAddress)))
  }

  def checkDbHelper: Unit ={
    val duration = Duration.create(Seat.checkDuration,TimeUnit.MILLISECONDS)
    val start = Duration.create(0,TimeUnit.MILLISECONDS)
    undoBooking.scheduler.schedule(start, duration){
      println("[info] starting database checks")
      mongoDbController.unbookRunner
      println("[info] database checks complete")
    }
  }
}