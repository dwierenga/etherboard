/**
 * Copyright (C) 2011, 2012 Commission Junction
 *
 * This file is part of etherboard.
 *
 * etherboard is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * etherboard is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with etherboard; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */

package com.cj.nan.etherboard

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{Matchers, FunSpec, GivenWhenThen, Spec}
import java.io.FileInputStream
import java.io.File
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.junit.After
import util.Random
import com.fasterxml.jackson.databind.ObjectMapper
import scala.beans.BeanProperty

@RunWith(classOf[JUnitRunner])
class BoardDaoSpecification extends FunSpec with Matchers with GivenWhenThen {
//class BoardDaoSpecification extends FunSpec with ShouldMatchers with GivenWhenThen {


    describe("A BoardDao") {
        it("should save a Board object") {
            Given("A BoardDao and a new Board to save")
                val boardObject: BoardObject = new BoardObject(1, 0, "", "", "", "some name", "some extra note", "some kind", new Position())
                val boardName:String = Random.alphanumeric take 30 mkString
    
                val aBoard = new Board(boardName, boardObject)
                val boardDao = new BoardDaoImpl()
            When("saving the board")
                boardDao.saveBoard(aBoard)
            Then("a text file with the board contents should be saved")
                val file:File = new File("target/test-data", boardName)
                file.exists() should  be (true)
                val jackson = new ObjectMapper()
                val retrievedBoard = jackson.readValue(new FileInputStream(file), classOf[Board])
                retrievedBoard.name should equal (aBoard.name)
                file.delete() should  be (true)
        }

        it("should be able to retrieve an existing Board") {
            Given("a saved board")
                val boardObject: BoardObject = new BoardObject(1, 0, "", "", "",  "some name", "some extra note", "some kind", new Position())
                val boardName:String = Random.alphanumeric take 30 mkString

                val aBoard = new Board(boardName, boardObject)
                new BoardDaoImpl().saveBoard(aBoard)
            When("getting the saved board")
                val retrievedBoard:Board = new BoardDaoImpl().getBoard(boardName)
            Then("the board should be retrieved successfully")
                retrievedBoard.name should equal (aBoard.name)
                val retrievedBoardObject:BoardObject = retrievedBoard.findObject(1).get
                retrievedBoardObject.name should equal ("some name")
                retrievedBoardObject.kind should equal ("some kind")
                val file:File = new File("target/test-data", boardName)
                file.exists() should  be (true)
                file.delete() should  be (true)
        }

        it("should import stickie as sticky") {
            Given("a saved board")
                val boardName:String = Random.alphanumeric take 30 mkString

                val boardObject: BoardObject = new BoardObject(1, 0, "", "", "","some name", "some extra note", "stickie", new Position())
                boardObject.kind should equal ("sticky")

                boardObject.kind = "stickie"
                boardObject.kind should equal ("stickie")

                val aBoard: Board = new Board(boardName, boardObject)
                new BoardDaoImpl().saveBoard(aBoard)
            When("getting the saved board")
                val retrievedBoard:Board = new BoardDaoImpl().getBoard(boardName)
            Then("the board's stickie should now be a sticky")
                val retrievedBoardObject:BoardObject = retrievedBoard.findObject(1).get
                retrievedBoardObject.kind should equal ("sticky")
                val file:File = new File("target/test-data", boardName)
                file.exists() should  be (true)
                file.delete() should  be (true)
        }
    }

  @After
  def cleanup() {
    val dataDirectory:File = new File("target")
  }

}
