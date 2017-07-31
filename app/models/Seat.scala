package models

import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.Json
import com.typesafe.config.ConfigFactory

case class Seat(id: Long, author: String, booked: Boolean, expiry: Long, kind: String)


object Seat{
  implicit val seatFormat = Json.format[Seat]



  val firstSeat = 1
  val maxSeats = 201
  val expiryDuration = ConfigFactory.load().getLong("seat.duration.expiry") * 60000
  val checkDuration = ConfigFactory.load().getLong("seat.duration.check") * 60000

  def getExpiryDate: Long = {
    DateTime.now(DateTimeZone.UTC).getMillis + expiryDuration
  }

  val getSeats: List[Seat] ={
    def getSeatsHelper(position: Long)(result: List[Seat]): List[Seat] = position match{
      case 201 => result
      case x => x match{
        case x if ((x - 4) % 20 == 0) || ((x + 3) % 20 == 0) =>
          getSeatsHelper(position + 1)(result :+ Seat(position,"",false,0,"EMPTY"))
        case x if x <= 40 => getSeatsHelper(position + 1)(result :+ Seat(position,"",false,0,"STANDARD"))
        case x if x <= 60 => getSeatsHelper(position + 1)(result :+ Seat(position,"",false,0,"EMPTY"))
        case x if x <= 80 => getSeatsHelper(position + 1)(result :+ Seat(position,"",false,0,"DISABLED"))
        case x if x <= 100 => getSeatsHelper(position + 1)(result :+ Seat(position,"",false,0,"VIP"))
        case _ => getSeatsHelper(position + 1)(result :+ Seat(position,"",false,0,"STANDARD"))
      }
    }
    getSeatsHelper(1)(List())
  }
}
