package helpers

import javax.inject.Singleton

@Singleton
class SessionHelper {

  private val spectrum = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz123456789"

  def getSessionKey(len: Int = 32): String ={

    def sessionKeyHelper(pos: Int)(key: String): String=pos match{
      case 0 => key
      case _ =>
        val charAtIndex: Int = Math.floor(Math.random()*spectrum.length).toInt
        sessionKeyHelper(pos - 1)(key + spectrum.charAt(charAtIndex))
    }

    sessionKeyHelper(len)("")
  }

}
