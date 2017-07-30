package models

import java.util.Calendar

import play.api.libs.json.Json

case class DateSlot(name: String, timeSlots: List[TimeSlot])

object DateSlot{

  implicit val dateSlotFormat = Json.format[DateSlot]

  val dateRange = 7
  val months = List("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","NOV","DEC")

  def dateSlotNames: List[String] = {
    def getNamesHelper(position: Int)(result: List[String]): List[String] = position match {
      case 0 => result
      case _ =>
        val date = Calendar.getInstance()
        date.add(Calendar.DAY_OF_MONTH,position-1)
        val day = date.get(Calendar.DAY_OF_MONTH)
        val month = months(date.get(Calendar.MONTH))
        getNamesHelper(position - 1)(result :+ s"$day $month 2017")
    }
    getNamesHelper(dateRange)(List())
  }

  def getDateSlots: List[DateSlot] = {
    def getSlotsHelper(slots: List[String])(result: List[DateSlot]): List[DateSlot] = slots.length match{
      case 0 => result
      case _ => getSlotsHelper(slots.tail)(result :+ DateSlot(slots.head,TimeSlot.getTimeSlots))
    }
    getSlotsHelper(dateSlotNames)(List())
  }

  val getIndex = (x: String) => dateSlotNames.indexOf(x)
}