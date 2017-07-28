package models

import java.util.Calendar

import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.Json

case class Seat(id: Long, movie: String, author: String, booked: Boolean, expirey: Long)

case class ExposedSeat(id: Long, bookedBy: Boolean, available: Boolean = false)

object Seat{
  implicit val seatFormat = Json.format[Seat]
  implicit val exposedSeatFormat = Json.format[ExposedSeat]

  val durationMillis = 600000 //duration in milliseconds

  def getExpiryDate: Long = {
    DateTime.now(DateTimeZone.UTC).getMillis + durationMillis
  }
}
