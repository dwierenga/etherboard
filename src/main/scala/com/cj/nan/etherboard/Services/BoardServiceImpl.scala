package com.cj.nan.etherboard.Services

import com.cj.nan.etherboard.{ApplicationWiring, Board, PivotalTrackerBoard}
import com.fasterxml.jackson.databind.ObjectMapper
import org.httpobjects.DSL._
import org.httpobjects.{Request, Response}
import org.httpobjects.header.response.LocationField

trait BoardService {

  var wiring: ApplicationWiring = null

  def setWiring(initWiring:ApplicationWiring) = {
    wiring = initWiring
  }

  def listBoards(): Response ={
    val jackson = new ObjectMapper()
    OK(Json(jackson.writeValueAsString(wiring.boardDao.listBoards())));
  }

   def makeBoard(requestBodyString:String):Response = {

    try {

      val parsedBody:Map[String, String] = wiring.util.parseHttpForm(requestBodyString)

      val newBoard = parsedBody ("syncType") match {
        case "pivotalTracker" => new PivotalTrackerBoard (parsedBody ("name"), parsedBody ("pivotalProjectId"), parsedBody ("pivotalDevKey") )
        case _ => new Board (parsedBody ("name") )
      }

      wiring.boardDao.saveBoard (newBoard)
      val result = SEE_OTHER (new LocationField ("/?board=" + newBoard.name) )
      result

    } catch {
      case t:Throwable => {
        BAD_REQUEST
      }
    }
  }

}

class BoardServiceImpl() extends BoardService {


}
