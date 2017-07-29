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

  //temporary

  def getSeatsBySlots(key: String, name: String, date: String, time: String): Option[String] = {

    val agg = moviesCol.map{_.aggregate(Match(Json.obj("name"->name)),
      List(UnwindField("dateSlots"),
        Match(Json.obj("dateSlots.name" -> date)),
        UnwindField("dateSlots.timeSlots"),
        Match(Json.obj("dateSlots.timeSlots.name"-> time)),
        Group(JsString("$_id"))(
          "name" -> First(JsString("$name")),
          "dateSlots"-> First(JsString("$dateSlots")))))
      }

    Await.result(agg, Duration.Inf) match {
      case aggregateResult =>
          val futureResult = Await.result(aggregateResult,Duration.Inf)
          val jsonResult = futureResult.firstBatch.head.value
          val movieName = Json.toJson(jsonResult.get("name")).as[String]
          val timeSlot = (Json.toJson(jsonResult) \ "dateSlots" \ "timeSlots" ).validate[TimeSlot]
          val seats = (Json.toJson(jsonResult) \ "dateSlots" \ "timeSlots" \ "seats" ).validate[List[Seat]]
          val dateSlotName = (Json.toJson(jsonResult) \ "dateSlots" \ "name" ).as[String]

          seats match {
            case success: JsSuccess[List[Seat]] =>
              Some(getJsonString(success.value, key))
            case error: JsError => println(JsError.toJson(error).toString())
              None
          }
      }
  }

  //==================================== SEAT SELECTION AND BOOKING =================================================//
  def bookSeat(seat: Seat) = {
    isSeatInDb(seat.id) match {
      case false =>
        println("seat is not in database")
        seatsCol.flatMap(_.insert(seat))
      case true =>
        println("seat is in database")
        seatsCol.map {
          _.findAndRemove(Json.obj("id" -> seat.id, "author" -> seat.author))
        }
    }
  }

  def submitBooking(key: String) = {
    seatsCol.map {
      _.findAndUpdate(Json.obj("author" -> key), Json.obj("booked" -> true))
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
      case _ => true
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
      case 0 => jsonString.length match {
        case 1 => jsonString + "]"
        case _ => jsonString.dropRight(1) + "]"
      }
      case _ =>
        val bookedBy = tempSeats.head.author == key
        val newStr = "{\"seatid\":" + tempSeats.head.id + "," +
          "\"available\": \"" + !tempSeats.head.booked + "\", " +
          "\"bookedBy\": \"" + bookedBy + "\"},"
        getJsonHelper(tempSeats.tail)(jsonString + newStr)
    }

    getJsonHelper(seats)("[")
  }
}
