package models

import play.api.libs.json.Json

case class TimeSlot(name: String, seats: List[Seat])

object TimeSlot{
  implicit val dateSlotFormat = Json.format[TimeSlot]
  val timeSlotNames: List[String] = List("0:00","3:00","6:00","9:00","12:00","15:00","18:00","21:00")
  val getTimeSlots: List[TimeSlot] = {
    def getSlotsHelper(slots: List[String])(result: List[TimeSlot]): List[TimeSlot] = slots.length match{
      case 0 => result
      case _ => getSlotsHelper(slots.tail)(result :+ TimeSlot(slots.head,Seat.getSeats))
    }
    getSlotsHelper(timeSlotNames)(List())
  }
}
