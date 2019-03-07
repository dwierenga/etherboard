package com.cj.nan.etherboard.endpoints

import java.io.OutputStream

import com.cj.nan.etherboard.Services.BoardService
import com.cj.nan.etherboard.utilities.Util
import com.cj.nan.etherboard.{BoardDaoImpl, BoardDao, ApplicationWiring, utilities}
import com.fasterxml.jackson.databind.ObjectMapper
import org.httpobjects.DSL._
import org.httpobjects.header.request.RequestHeader
import org.httpobjects.header.response.LocationField
import org.httpobjects.path.Path
import org.httpobjects._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{GivenWhenThen, Matchers, FunSuite}

@RunWith(classOf[JUnitRunner])
class BoardResourceTest extends FunSuite with Matchers with GivenWhenThen {


  class MockBoardService extends BoardService {

    override def makeBoard(requestBodyString: String): Response = {
      SEE_OTHER (new LocationField ("/?board=" + "test board") )
    }

    override def listBoards(): Response = OK(Text("listBoards was here"))
  }


  def setupWiring():ApplicationWiring = {
    val boardDao:BoardDao = new BoardDaoImpl()
    val boardService:BoardService = new MockBoardService
    val util = new Util()
    val wiring = ApplicationWiring(
      util,
      boardDao,
      boardService
    )
    wiring
  }

  test("BoardResource get delegates to BoardServiceImpl.makeBoard(repString)") {

    // given
    val wiring = setupWiring()
    val boardResource = new BoardResource(wiring)
    val request = new Request {override def immutableCopy(): Request = ???

      override def header(): RequestHeader = ???

      override def hasRepresentation: Boolean = ???

      override def representation(): Representation = {
        new Representation(){
          override def contentType(): String = "text/random"

          override def write(out: OutputStream): Unit = {}
        }
      }

      override def path(): Path = ???

      override def query(): Query = ???
    }


    val response = boardResource.post(request)
    val responseCodeString = response.code().toString()

    // then
    response should not be null
    responseCodeString should equal ("SEE_OTHER(303)")
  }

  test("BoardResource get delegates to BoardServiceImpl.listBoards") {

    // given
    val wiring = setupWiring()
    val boardResource = new BoardResource(wiring)
    val request = new Request {override def immutableCopy(): Request = ???

      override def header(): RequestHeader = ???

      override def hasRepresentation: Boolean = ???

      override def representation(): Representation = {
        new Representation(){
          override def contentType(): String = "text/random"

          override def write(out: OutputStream): Unit = {}
        }
      }

      override def path(): Path = ???

      override def query(): Query = ???
    }


    val response = boardResource.get(request)
    val responseCodeString = response.code().toString()

    // then
    response should not be null
    responseCodeString should equal ("OK(200)")
  }
}
