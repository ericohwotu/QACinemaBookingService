package models

import play.api.libs.json.Json



case class Screening(name: String, dateSlots: List[DateSlot])

object Screening{
  implicit val moviesFormat = Json.format[Screening]

  def generateMovie(movieName:String): Screening ={
    Screening(movieName,DateSlot.getDateSlots)
  }
}