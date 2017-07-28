package models

import play.api.libs.json.Json

case class ApiKey(key: String, user: String)

object ApiKey{
  implicit val keyFormat = Json.format[ApiKey]
}
