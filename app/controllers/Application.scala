package controllers

import java.util.concurrent.TimeUnit

import play.api._
import play.api.mvc._
import javax.inject._

import akka.actor.ActorSystem
import play.api.data.format.Formats._
import play.api.i18n._
import business.SeatGenerator
import com.typesafe.config.ConfigFactory
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
  val paymentUrl = ConfigFactory.load().getString("payment.server")

  val hiddenMultips = (str: String) =>  str.split(",").zipWithIndex.map{
    case (value,index) => s"<data id='data-$index'>$value</data>"
  }

  val homePage = (name: String, request: Request[AnyContent]) =>
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
        None
      case false =>
        mongoDbController.addMovie2Db(Movie.generateMovie(name))
    }

    homePage(name,request).withSession("sessionKey" -> SessionHelper.getSessionKey(),"movieName"->name)
  }

  def toPayment(amount: String) = Action{ request: Request[AnyContent] =>
    println(request.headers)
    Redirect(paymentUrl + amount).withCookies(Cookie("time","time"))
  }

  def toSubmitBooking() = Action{ request: Request[AnyContent] =>

    val tDate = request.session.get("date").getOrElse("none")
    val tTime = request.session.get("time").getOrElse("none")

    println(s"$tDate and $tTime")
    Redirect(routes.JsonApiController.submitBooking(date = tDate, time = tTime))
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