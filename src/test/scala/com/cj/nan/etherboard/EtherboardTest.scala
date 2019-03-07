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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, FunSuite}
import org.scalatest.matchers.ShouldMatchers
import java.lang.String
import com.fasterxml.jackson.databind.ObjectMapper
import scala.xml.Node
import scala.xml.XML

@RunWith(classOf[JUnitRunner])
class EtherboardTest extends FunSuite with Matchers {
//class EtherboardTest extends FunSuite with ShouldMatchers {

    test("Position is initialized with position 0,0") {
        val myPosition = new Position()

        //then
        myPosition.left should equal(0)
        myPosition.top should equal(0)
    }

    test("updating BoardObject from another BoardObject") {
        val originalBoardObject: BoardObject = new BoardObject(1, 0, "", "", "", "original name", "original extra note", "original kind", new Position())
        val newPosition: Position = new Position(34, 87)
        val newBoardObject: BoardObject = new BoardObject(2, 0, "", "", "", "new name", "new extra note", "new kind", newPosition)

        originalBoardObject.updateFrom(newBoardObject)

        //then
        originalBoardObject.id should equal(1)
        originalBoardObject.name should equal("new name")
        originalBoardObject.kind should equal("original kind")
        originalBoardObject.pos should equal(newPosition)
    }
    
    test("finding a board object") {
    	val boardObject: BoardObject = new BoardObject(1, 0, "", "", "", "some name", "some extra note", "some kind", new Position())
    	val board = new Board("Herbert", boardObject)
    	
    	val shouldBeFound = board.findObject(1)
    	assert(shouldBeFound.isDefined === true)
    	assert(shouldBeFound.get === boardObject)
    	
    	val shouldNotBeFound = board.findObject(2)
    	assert(shouldNotBeFound.isDefined === false)
    }

    test("Remove a board object") {
        var a: BoardObject = new BoardObject(3, 0, "", "", "", "a is excellent", "yes it is", "sticky", new Position(0, 0))
        var b: BoardObject = new BoardObject(14, 0, "", "", "", "b is bad", "fo sho", "stinky", new Position(0, 0))
        var bb: Board = new Board("bb", a, b);

        assert(bb.objects.contains(a))
        assert(bb.objects.contains(b))

        bb.removeObject(a.id)

        assert(!bb.objects.contains(a))
        assert(bb.objects.contains(b))
    }

    test("BoardObjects equal") {
        var boardObject: BoardObject = new BoardObject(4, 0, "", "", "", "some name", "some note", "some kind", new Position(37, 928))
        var otherBoardObject: BoardObject = new BoardObject(4, 0, "", "", "", "some name", "some note", "some kind", new Position(37, 928))

        assert(boardObject === otherBoardObject)
        otherBoardObject.pos = new Position(37, 927)
        assert((boardObject == otherBoardObject) === false)
    }

    test("Serializing/Deserializing different board types") {
        val board: Board = new Board("Simpleton Board")
        val syncedBoard: Board = new PivotalTrackerBoard("Not-so-simpleton Board", "123456", "somekey")

        val jackson = new ObjectMapper()

        val boardJson: String = jackson.writeValueAsString(board)
        val syncedBoardJson: String = jackson.writeValueAsString(syncedBoard)

        assert(boardJson === "{\"type\":\"simple\",\"name\":\"Simpleton Board\",\"objects\":[],\"id_sequence\":0,\"boardUpdatesWebSocket\":\"\"}")
        assert(syncedBoardJson === "{\"type\":\"pivotalTracker\",\"name\":\"Not-so-simpleton Board\",\"objects\":[],\"id_sequence\":0,\"boardUpdatesWebSocket\":\"\",\"toolSyncId\":\"123456\",\"toolSyncKey\":\"somekey\"}")

        val readBoard = jackson.readValue(boardJson, classOf[Board])
        val readSyncedBoard = jackson.readValue(syncedBoardJson, classOf[Board])
        
        assert(readBoard.getClass === classOf[Board])
        assert(syncedBoard.getClass === classOf[PivotalTrackerBoard])
        
        val readAmbiguousBoard = jackson.readValue("{\"name\":\"Simpleton Board\",\"objects\":[],\"id_sequence\":0,\"boardUpdatesWebSocket\":\"\"}", classOf[Board])
        assert(readAmbiguousBoard.getClass === classOf[Board])
        assert(readAmbiguousBoard.name === "Simpleton Board")
        
        val xmlString = "<stories><story><description>lolyup</description><name>Joe</name><url>http://cj.com</url><id>1000</id></story><story><description>wtfno</description><name>Sam</name><id>1001</id><url>http://aj.com</url></story></stories>"
        val xmlStories = XML.loadString(xmlString)
        
        val res = xmlStories.child.map(s => {
          val id = s\"id"
          val url = s\"url"
          val name = s\"name"
          val desc = s\"description"
          new PivotalTrackerStory(Integer.parseInt(id.text), url.text, name.text, desc.text)
        })
        
        assert(res.length === 2)
        assert(res(0).id === 1000)
        assert(res(0).url === "http://cj.com")
        assert(res(0).name === "Joe")
        assert(res(0).description === "lolyup")
        assert(res(1).id === 1001)
        assert(res(1).url === "http://aj.com")
        assert(res(1).name === "Sam")
        assert(res(1).description === "wtfno")
    }
}
