package models

import java.util.Calendar

import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.Json

case class Seat(id: Long, author: String, booked: Boolean, expirey: Long)


object Seat{
  implicit val seatFormat = Json.format[Seat]


  val firstSeat = 1
  val maxSeats = 201
  val durationMillis = 600000 //duration in milliseconds

  def getExpiryDate: Long = {
    DateTime.now(DateTimeZone.UTC).getMillis + durationMillis
  }

  val getSeats: List[Seat] ={
    def getSeatsHelper(position: Long)(result: List[Seat]): List[Seat] = position match{
      case 201 => result
      case _ => getSeatsHelper(position + 1)(result :+ Seat(position,"",false,0))
    }
    getSeatsHelper(1)(List())
  }
}
