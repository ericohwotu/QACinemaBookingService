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
import models.{DateSelector, Screening, Seat}
import play.api.data.{Form, Forms}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.duration.Duration

@Singleton
class ScreeningsController @Inject()(implicit val messagesApi: MessagesApi,
                                     val mongoDbController: ScreeningsDbController
                          ) extends Controller with I18nSupport{

  val paymentUrl = ConfigFactory.load().getString("payment.server")

  val hiddenMultips = (str: String) =>  str.split(",").toList

  val homePage = (name: String, vals: List[String], request: Request[AnyContent]) =>
    Ok(views.html.bookings(name, vals)(DateSelector.dsForm, SeatGenerator.getLayout(request.remoteAddress)))


  val seatsForm = Form[(Int, Int)](
    Forms.tuple(
      "bookingid" -> Forms.of[Int],
      "seatid" -> Forms.of[Int]
    )
  )

  def index(name: String, vals: String) = Action { request: Request[AnyContent] =>
    //create movie database
    mongoDbController.isMovieInDb(name) match {
      case true =>
        None
      case false =>
        mongoDbController.addMovie2Db(Screening.generateMovie(name))
    }

    homePage(name,hiddenMultips(vals),request).withSession("sessionKey" -> SessionHelper.getSessionKey(),"movieName"->name)
  }

  def toPayment(amount: String) = Action{ request: Request[AnyContent] =>
    println(request.headers)
    Redirect(paymentUrl + amount).withCookies(Cookie("time","time"))
  }

  def toSubmitBooking() = Action{ request: Request[AnyContent] =>

    val tDate = request.session.get("date").getOrElse("none")
    val tTime = request.session.get("time").getOrElse("none")

    println(s"$tDate and $tTime")
    Redirect(routes.ScreeningsApiController.submitBooking(date = tDate, time = tTime))
  }
}