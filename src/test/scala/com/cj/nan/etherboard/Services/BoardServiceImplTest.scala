package com.cj.nan.etherboard.Services

import com.cj.nan.etherboard.utilities.Util
import com.cj.nan.etherboard.{Board, BoardDaoImpl, BoardDao, ApplicationWiring}
import org.httpobjects.Response
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{GivenWhenThen, Matchers, FunSuite}


@RunWith(classOf[JUnitRunner])
class BoardServiceImplTest extends FunSuite with Matchers with GivenWhenThen{


  case class MockBoardDaoImpl() extends BoardDao {

    var board: Board = null

    override def getBoard(id: String): Board = board

    override def saveBoard(board: Board): Unit = { this.board = board }

    override def listBoards(): Array[String] = Array[String]("foo","bar")
  }

  def setupWiring():ApplicationWiring = {
    val boardDao:BoardDao = MockBoardDaoImpl()
    val boardService:BoardService = new BoardServiceImpl()
    val util = new Util()
    val wiring = ApplicationWiring(
      util,
      boardDao,
      boardService
    )
    wiring
  }

  test("BoardService should create new board and redirection"){
    //given
    val wiring = setupWiring()
    val service = wiring.boardService
    val repString = "name=Test1&syncType=simple"
    val response = service.makeBoard(repString)

    //when

    val responseCodeString = response.code().toString()

    // then
    response should not be null
    responseCodeString should equal ("SEE_OTHER(303)")

  }

  test("BoardService should return bad request if cannot make board"){
    //given
    val wiring = setupWiring()
    val service = wiring.boardService
    val repString = "foo=quux&bar=baz"
    val response = service.makeBoard(repString)

    //when

    val responseCodeString = response.code().toString()

    // then
    response should not be null
    responseCodeString should equal ("BAD_REQUEST(400)")

  }

  test("BoardService should return board list"){
    //given
    val wiring = setupWiring()
    val service = wiring.boardService

    //when
    val response:Response = service.listBoards()
    val responseDataString = wiring.util.getResponseDataString(response)

    // then
    response should not be null
    responseDataString should equal ("[\"foo\",\"bar\"]")
    response.code.toString() should equal ("OK(200)")
  }
}
