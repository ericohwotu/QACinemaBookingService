package util

import play.api.{Application, GlobalSettings}


object Global extends GlobalSettings{
  override def onStart(app: Application): Unit = ???
}
