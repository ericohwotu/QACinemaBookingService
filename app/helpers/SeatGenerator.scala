package business

import scala.collection.mutable.ListBuffer

/**
  * Created by Eric on 22/07/2017.
  */

object SeatGenerator {

  type ClientAddress = String
  type JsonOutcome = String
  type JsonResults = List[(Long,Boolean,Boolean)]


  var seatHistory: ListBuffer[Int] = new ListBuffer()
  var sessionKeys: ListBuffer[String] = new ListBuffer()


  def getLayout(currentAccess: String): String = {
    //design the web page seats
    var html = "<br><table class=\"col-sm-12 seatsTable\"><tbody>"

    var currentSeat = 1
    var btnClass = "available"

    for (i <- 0 until 10) {
      html+="<tr>" // create new row
      for (j <- 0 until 20) {
        var enabled = ""
        if (seatHistory.contains(currentSeat)) {
          btnClass = "booked"
          val index = seatHistory.indexOf(currentSeat)
          if (!sessionKeys(index).equals(currentAccess)) {
            enabled = "disabled"
            btnClass = "unavailable"
          }
          else enabled = ""
        }
        else btnClass = "available"

        html+=addButton(currentSeat, btnClass, enabled)
        currentSeat += 1
      }
      html += "</tr>"
    }
    html += "</tbody></table>"
    html
  }

  private def addButton(seatNo: Int, cls: String, en: String): String = {
    "<td>"+
      "<form style=\"display: inline;\" action="+
      "post " +
      "method=POST>"+
      "<input type=hidden style=\"display: none !important;\" size=20 name=bookingid value=" + seatNo + ">" +
      "<input type=hidden style=\"display: none !important;\" size=20 name=seatid value=" + seatNo + ">" +
      "<input type=submit class=\"fsSubmitButton " + cls + " \"" +  en + " \" value=\"\"  id=\"seat-" + seatNo + "\">" +
      "</form>"+
      "</td>"
  }

  // json actions
  def isSeatAvailable(seat: Long): String ={
    "{\"available\": \"" + seatHistory.contains(seat) + " \"}"
  }

  def getSeats(client: String): String ={

    def getSeatsHelper(position: Long)(result: String): String= position match{
      case 0 => result.dropRight(1) + "]"
      case _ =>
        val posForAccess = seatHistory.indexOf(position)

        val bookedBy = (x: Int) => if(x>=0) (sessionKeys(x)==client).toString else "false"

        val newStr = "{\"seatid\":" + position + "," +
          "\"available\": \"false\", " +
          "\"bookedBy\": \""+ bookedBy(posForAccess) +"\"},"


        getSeatsHelper(position-1)(result+newStr)
    }

    getSeatsHelper(200)("[")
  }

  def bookSeats(id: Int, client: String): String = seatHistory.contains(id) match{
    case true if sessionKeys(seatHistory.indexOf(id))==client=>
      seatHistory -= id
      sessionKeys -= client
      "{\"outcome\": \"success\",\"message\": \"seat unbooked\"}"
    case false =>
      seatHistory += id
      sessionKeys += client
      "{\"outcome\": \"success\",\"message\": \"seat booked\"}"
    case _ =>
      "{\"outcome\": \"failure\",\"message\": \"Seat already booked by someone else\"}"
  }

}
