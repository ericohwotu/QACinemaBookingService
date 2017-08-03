package util

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.actor.ActorSystem
import controllers.ScreeningsDbController
import models.Seat
import play.api.{Application, Configuration, GlobalSettings}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.duration.Duration
class StartUpSchedular @Inject()(mongoDbController: ScreeningsDbController){
  val undoBooking = ActorSystem("unbookingService")


  def checkDbHelper: Unit ={
    val duration = Duration.create(Seat.checkDuration,TimeUnit.MILLISECONDS)
    val start = Duration.create(0,TimeUnit.MILLISECONDS)
    undoBooking.scheduler.schedule(start, duration){
      println("[info] starting database checks")
      mongoDbController.unbookRunner
      println("[info] database checks complete")
    }
  }

  checkDbHelper
}
