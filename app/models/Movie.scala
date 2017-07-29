package models

import play.api.libs.json.Json



case class Movie(name: String, dateSlots: List[DateSlot])

object Movie{
  implicit val moviesFormat = Json.format[Movie]

  def generateMovie(movieName:String): Movie ={
    Movie(movieName,DateSlot.getDateSlots)
  }
}