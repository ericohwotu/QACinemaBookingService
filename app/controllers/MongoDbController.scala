package controllers

import javax.inject.Inject

import business.SeatGenerator.seatHistory
import helpers.SessionHelper
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.play.json.collection._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models.{ApiKey, Seat}
import reactivemongo.play.json._
import reactivemongo.api._
import play.api.libs.json._

import scala.concurrent.duration.Duration
import scala.util.parsing.json._
import scala.concurrent.{Await, Future}

class MongoDbController @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends Controller
  with ReactiveMongoComponents with MongoController {

  def seatsCol: Future[JSONCollection] = database.map(_.collection[JSONCollection]("SeatsCollection"))
  def screensCol: Future[JSONCollection] = database.map(_.collection[JSONCollection]("ScreensCollection"))
  def moviesCol: Future[JSONCollection] = database.map(_.collection[JSONCollection]("MoviesCollection"))
  def apiKeyCol: Future[JSONCollection] = database.map(_.collection[JSONCollection]("ApiCollection"))


  //===================================API KEY FUNCTIONS===================================================//
  def getKey: Action[AnyContent] = Action { request: Request[AnyContent] =>
    val key = ApiKey(SessionHelper.getSessionKey(), "blank")
    apiKeyCol.flatMap(_.insert(key))
    Ok(Json.parse("{\"key\":\"" + key.key + "\",\"user\":\"" + key.user + "\"}"))
  }

  def isKeyAvailable(key: String): Boolean = {
    val cursor: Future[Cursor[ApiKey]] = apiKeyCol.map {
      _.find(Json.obj("key" -> key))
        .sort(Json.obj("id" -> -1))
        .cursor[ApiKey](ReadPreference.primary)
    }

    val list: Future[List[ApiKey]] = cursor.flatMap(_.collect[List]())

    Await.result(list, Duration.Inf).length match {
      case 0 => false
      case _ => true
    }
  }

  //==================================== SEAT SELECTION AND BOOKING =================================================//
  def bookSeat(seat: Seat) ={
    isSeatInDb(seat.id) match {
      case false =>
        println("seat is not in database")
        seatsCol.flatMap(_.insert(seat))
      case true =>
        println("seat is in database")
        seatsCol.map {_.findAndRemove(Json.obj("id"->seat.id,"author"->seat.author))}
    }
  }

  def submitBooking(key: String) = {
    seatsCol.map {
      _.findAndUpdate(Json.obj("author"->key),Json.obj("booked"->true))
    }
  }

  def isSeatInDb(id: Long): Boolean = {
    val cursor: Future[Cursor[Seat]] = seatsCol.map {
      _.find(Json.obj("id" -> id))
        .cursor[Seat](ReadPreference.primary)
    }

    val list: Future[List[Seat]] = cursor.flatMap(_.collect[List]())

    Await.result(list, Duration.Inf) match {
      case x if x.isEmpty => false
      case x => true
    }
  }

  def getSeats(key: String): String = {
    val cursor: Future[Cursor[Seat]] = seatsCol.map {
      _.find(Json.obj()).sort(Json.obj("id" -> -1))
        .cursor[Seat](ReadPreference.primary)
    }

    val futureSeats: Future[List[Seat]] = cursor.flatMap(_.collect[List]())

    val seats = Await.result(futureSeats, Duration.Inf)

    getJsonString(seats, key)
  }

  def getJsonString(seats: List[Seat], key: String): String = {

    def getJsonHelper(tempSeats: List[Seat])(jsonString: String): String = tempSeats.length match {
      case 0 => jsonString.dropRight(1) + "]"
      case _ =>
        val bookedBy = tempSeats.head.author==key
        val newStr = "{\"seatid\":" + tempSeats.head.id + "," +
          "\"available\": \"false\", " +
          "\"bookedBy\": \""+ bookedBy +"\"},"
        getJsonHelper(tempSeats.tail)(jsonString + newStr)
    }

    getJsonHelper(seats)("[")

  }
}
