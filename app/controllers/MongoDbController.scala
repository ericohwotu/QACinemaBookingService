package controllers

import javax.inject.Inject

import business.SeatGenerator.seatHistory
import helpers.SessionHelper
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents, json}
import reactivemongo.play.json.collection._
import reactivemongo.play.json.collection.JSONCollection
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models._
import reactivemongo.play.json._
import reactivemongo.api._
import play.api.libs.json._
import reactivemongo.api.commands.Command
import reactivemongo.play.json.commands.JSONAggregationFramework.{Cursor => _, _}

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

  //==================================== MOVIES =====================================================================//
  def isMovieInDb(movieName: String): Boolean = {
    val cursor: Future[Cursor[Movie]] = moviesCol.map {
      _.find(Json.obj("name" -> movieName))
        .cursor[Movie](ReadPreference.primary)
    }

    val list: Future[List[Movie]] = cursor.flatMap(_.collect[List]())

    Await.result(list, Duration.Inf) match {
      case x if x.isEmpty => false
      case x => true
    }
  }

  def addMovie2Db(movie: Movie) = {
    moviesCol.flatMap(_.insert(movie))
  }

  def getSeatsBySlots(name: String, date: String, time: String): Option[List[Seat]] = {

    val agg = moviesCol.map {
      _.aggregate(Match(Json.obj("name" -> name)),
        List(UnwindField("dateSlots"),
          Match(Json.obj("dateSlots.name" -> date)),
          UnwindField("dateSlots.timeSlots"),
          Match(Json.obj("dateSlots.timeSlots.name" -> time)),
          Group(JsString("$_id"))(
            "name" -> First(JsString("$name")),
            "dateSlots" -> First(JsString("$dateSlots")))))
    }

    Await.result(agg, Duration.Inf) match {
      case aggregateResult =>
        val futureResult = Await.result(aggregateResult, Duration.Inf)
        val jsonResult = futureResult.firstBatch.head.value
        val seats = (Json.toJson(jsonResult) \ "dateSlots" \ "timeSlots" \ "seats").validate[List[Seat]]
        seats match {
          case success: JsSuccess[List[Seat]] =>
            Some(success.value)
          case error: JsError => println(JsError.toJson(error).toString())
            None
        }
    }
  }

  //==================================== SEAT SELECTION AND BOOKING =================================================//
  def bookSeat(name: String, date: String, time: String, seat: Seat) = {

    val dateIndex = DateSlot.getIndex(date)
    val timeIndex = TimeSlot.getIndex(time)
    val queryString = s"dateSlots.$dateIndex.timeSlots.$timeIndex.seats.${seat.id - 1}.author"

    def bookHelper(author: String) = moviesCol.map {
      _.update(Json.obj("name" -> name),
        Json.obj("$set" -> Json.obj(s"$queryString" -> author)))
    }

    val seats = getSeatsBySlots(name, date, time).get
    val reqSeats = seats.filter(_.id == seat.id)

    doesSeatExist(reqSeats, seat) match {
      case false => None
      case true => toBook(reqSeats.head, seat) match {
        case true => bookHelper(seat.author)
        case false => bookHelper("")
      }
    }

  }

  def doesSeatExist(checkSeats: List[Seat], seat: Seat): Boolean = {
    checkSeats match {
      case x if x.isEmpty => false
      case x if x.head.author == "" || x.head.author == seat.author => true
      case _ => false
    }
  }

  def toBook(checkSeat: Seat, seat: Seat): Boolean = checkSeat.author match {
    case "" => true
    case seat.author => false
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
      case 0 => jsonString.length match {
        case 1 => jsonString + "]"
        case _ => jsonString.dropRight(1) + "]"
      }
      case _ =>
        val bookedBy = tempSeats.head.author == key
        val newStr = "{\"seatid\":" + tempSeats.head.id + "," +
          "\"available\": \"" + (tempSeats.head.author == "") + "\", " +
          "\"bookedBy\": \"" + bookedBy + "\"},"
        getJsonHelper(tempSeats.tail)(jsonString + newStr)
    }

    getJsonHelper(seats)("[")
  }

  //============================================= Bookings ===============================================/
  def submitBooking(key: String, name: String, date: String, time: String) = {
    val dateIndex = DateSlot.getIndex(date)
    val timeIndex = TimeSlot.getIndex(time)
    val findString = s"dateSlots.$dateIndex.timeSlots.$timeIndex.seats.author"
    val queryString = "dateSlots." + dateIndex + ".timeSlots." + timeIndex + ".seats.booked"
    println("*"*50)
    println("submitting")
    println("*"*50)
    moviesCol.map {
      _.update(Json.obj("name" -> name, findString -> key),
        Json.obj("$set" -> Json.obj(s"$queryString" -> true)), multi = true)
    }
  }
}
