package controllers

import javax.inject.Inject

import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.play.json.collection._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.play.json._
import reactivemongo.api._
import play.api.libs.json._

import scala.util.parsing.json._
import scala.concurrent.Future

class MongoDbController @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends Controller
with ReactiveMongoComponents with MongoController{

  def seatsCol: Future[JSONCollection] = database.map(_.collection[JSONCollection]("SeatsCollection"))
  def screensCol: Future[JSONCollection] = database.map(_.collection[JSONCollection]("ScreensCollection"))
  def moviesCol: Future[JSONCollection] = database.map(_.collection[JSONCollection]("MoviesCollection"))

  //================================================================================================//


}
