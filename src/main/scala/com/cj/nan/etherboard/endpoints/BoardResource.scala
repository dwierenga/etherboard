package com.cj.nan.etherboard.endpoints


import com.cj.nan.etherboard.{ApplicationWiring}
import com.fasterxml.jackson.databind.ObjectMapper
import org.httpobjects.DSL._
import org.httpobjects.{ResponseCode, Response, Request, HttpObject}


class BoardResource(wiring: ApplicationWiring)  extends HttpObject("/board") {

  override def get(req: Request) = {
    val jackson = new ObjectMapper()
    OK(Json(jackson.writeValueAsString(wiring.boardDao.listBoards())));
  }

  override def post(req: Request) = {

    try {

      val repString = wiring.util.getRequestRepresentationString(req)

      val result = wiring.boardService.makeBoard(repString)
      result

    } catch {
      case t:Throwable => {
        BAD_REQUEST
      }
    }



  }


}
