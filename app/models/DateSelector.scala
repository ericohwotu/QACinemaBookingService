package models

import play.api.data.Forms._
import play.api.data._
import play.api.libs.json._
import play.api.data.format.Formats._

/**
  * Created by Eric on 23/07/2017.
  */
case class DateSelector(date: String, time: String, screen: String)

object DateSelector{

  val dsForm = Form[DateSelector](
    mapping(
      "days" -> Forms.of[String],
      "time" -> Forms.of[String],
      "screen" -> Forms.of[String]
    )(DateSelector.apply _)(DateSelector.unapply _)
  )

  implicit val userFormat = Json.format[DateSelector]
}