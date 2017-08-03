package controllers

import javax.inject.{Inject, Singleton}

import helpers.SessionHelper
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents, json}
import reactivemongo.play.json.collection._
import reactivemongo.play.json.collection.JSONCollection
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import models._
import org.joda.time.{DateTime, DateTimeZone}
import reactivemongo.play.json._
import reactivemongo.api._
import play.api.libs.json._
import reactivemongo.api.commands.Command
import reactivemongo.play.json.commands.JSONAggregationFramework.{Cursor => _, _}

import scala.concurrent.duration.Duration
import scala.util.parsing.json._
import scala.concurrent.{Await, Future}

@Singleton
class ScreeningsDbController @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends Controller
  with ReactiveMongoComponents with MongoController {

  @Deprecated
  def seatsCol: Future[JSONCollection] = database.map(_.collection[JSONCollection]("SeatsCollection"))

  @Deprecated
  def screensCol: Future[JSONCollection] = database.map(_.collection[JSONCollection]("ScreensCollection"))

  def moviesCol: Future[JSONCollection] = database.map(_.collection[JSONCollection]("MoviesCollectionCustom"))

  def apiKeyCol: Future[JSONCollection] = database.map(_.collection[JSONCollection]("ApiCollection"))


  //===================================API KEY FUNCTIONS===================================================//
  def getKey: Action[AnyContent] = Action {
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
    val cursor: Future[Cursor[Screening]] = moviesCol.map {
      _.find(Json.obj("name" -> movieName))
        .cursor[Screening](ReadPreference.primary)
    }

    val list: Future[List[Screening]] = cursor.flatMap(_.collect[List]())

    Await.result(list, Duration.Inf) match {
      case x if x.isEmpty => false
      case x => true
    }
  }

  def getMoviesInDb: List[Screening] = {
    val cursor: Future[Cursor[Screening]] = moviesCol.map {
      _.find(Json.obj())
        .cursor[Screening](ReadPreference.primary)
    }

    val movies: Future[List[Screening]] = cursor.flatMap(_.collect[List]())

    Await.result(movies, Duration.Inf)
  }

  def addMovie2Db(movie: Screening) = {
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

        futureResult.firstBatch.isEmpty match {
          case true => None
          case false => getSeatsHelper(futureResult.firstBatch)
        }
    }
  }

  def getSeatsHelper(firstBatch: List[JsObject]): Option[List[Seat]] = {
    val jsonResult = firstBatch.head.value
    val seats = (Json.toJson(jsonResult) \ "dateSlots" \ "timeSlots" \ "seats").validate[List[Seat]]

    seats match {
      case success: JsSuccess[List[Seat]] =>
        Some(success.value)
      case error: JsError => println(JsError.toJson(error).toString())
        None
    }
  }

  //==================================== SEAT SELECTION =================================================//
  def bookSeat(name: String, date: String, time: String, seat: Seat) = {

    val dateIndex = DateSlot.getIndex(date)
    val timeIndex = TimeSlot.getIndex(time)
    val setAuthor = s"dateSlots.$dateIndex.timeSlots.$timeIndex.seats.${seat.id - 1}.author"
    val setExpiry = s"dateSlots.$dateIndex.timeSlots.$timeIndex.seats.${seat.id - 1}.expiry"
    val seats = getSeatsBySlots(name, date, time).get
    val reqSeats = seats.filter(_.id == seat.id)

    def bookHelper(author: String) = moviesCol.map {
      _.update(Json.obj("name" -> name),
        Json.obj("$set" -> Json.obj(s"$setAuthor" -> author,
          s"$setExpiry" -> Seat.getExpiryDate)))
    }

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
          "\"type\": \"" + tempSeats.head.kind + "\", " +
          "\"bookedBy\": \"" + bookedBy + "\"},"
        getJsonHelper(tempSeats.tail)(jsonString + newStr)
    }

    getJsonHelper(seats)("[")
  }

  //============================================= Bookings ===============================================/
  def submitBooking(key: String, name: String, date: String, time: String) = {
    val dateIndex = DateSlot.getIndex(date)
    val timeIndex = TimeSlot.getIndex(time)

    val findAuthor = s"dateSlots.$dateIndex.timeSlots.$timeIndex.seats.author"
    val findBooked = s"dateSlots.$dateIndex.timeSlots.$timeIndex.seats.booked"
    val updateString = "dateSlots." + dateIndex + ".timeSlots." + timeIndex + ".seats.$.booked"

    def submitHelper(position: Long): String = position match {
      case 0 => "done"
      case _ => Await.result(Await.result(moviesCol.map {
        _.update(Json.obj("name" -> name, findAuthor -> key, findBooked -> false),
          Json.obj("$set" -> Json.obj(s"$updateString" -> true)), multi = true)
      }, Duration.Inf),Duration.Inf)

        submitHelper(position - 1)
    }

    getSeatsBySlots(name, date, time).fold {} {
      seats =>
        val count = seats.filter(seat => seat.author == key && !seat.booked).length
        println(s"[info] count is {$count} seat auhor is {$key} while name of movie is {$name}")
        submitHelper(count + 1)
    }

  }

  //=============================================== Unbook Seats =============================//

  def unbookRunner = {
    getMoviesInDb.foreach { movie =>
      dateSlotUpdater(movie)
      movie.dateSlots.zipWithIndex.foreach { case (dateSlot, dateIndex) =>
        dateSlot.timeSlots.zipWithIndex.foreach { case (timeSlot, timeIndex) =>

          timeSlot.seats.zipWithIndex.filter {
            case (seat, seatIndex) =>
              seat.expiry > 0 && seat.expiry < DateTime.now(DateTimeZone.UTC).getMillis
          }.foreach { case (seat, seatIndex) =>
            val updateExpiry = s"dateSlots.$dateIndex.timeSlots.$timeIndex.seats.$seatIndex.expiry"
            val updateAuthor = s"dateSlots.$dateIndex.timeSlots.$timeIndex.seats.$seatIndex.author"
            seat.booked match {
              case true => moviesCol.map {
                _.update(Json.obj("name" -> movie.name),
                  Json.obj("$set" -> Json.obj(updateExpiry -> 0)), multi = true)
              }
              case false => moviesCol.map {
                _.update(Json.obj("name" -> movie.name),
                  Json.obj("$set" -> Json.obj(updateExpiry -> 0, updateAuthor -> "")), multi = true)
              }
            }
          }
        }
      }
    }
  }

  def dateSlotUpdater(movie: Screening) = {
    val uniqueList = DateSlot.getDateSlots.filter{ newSlot => movie.dateSlots.forall{
        dateSlot => dateSlot.name != newSlot.name
      }
    }

    def dateSlotHelper(dateSlots: List[DateSlot]): Unit = dateSlots.isEmpty match {
      case true => None
      case false =>
        Await.result(Await.result(moviesCol.map {
          _.update(Json.obj("name"->movie.name),
            Json.obj("$push" -> Json.obj("dateSlots" ->
              Json.obj("$each"->List(dateSlots.head),"$position"->0))))
        },Duration.Inf),Duration.Inf)
        dateSlotHelper(dateSlots.tail)
    }
    dateSlotHelper(uniqueList.reverse)
  }

}
