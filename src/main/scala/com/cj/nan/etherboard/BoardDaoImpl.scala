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

import scala.collection.JavaConversions._
import java.io.{File => Path, FileOutputStream, FileInputStream}
import com.fasterxml.jackson.databind.ObjectMapper

class BoardDaoImpl(dataPath:Path = new Path("target/test-data")) extends BoardDao {
    

    def apply(): BoardDao = {
        dataPath.mkdirs()
        this
    }

    def storagePathForBoard(id: String) = new Path(dataPath, id)

    def unhideContents(board: Board) {
        board.objects.foreach(
            boardObject => {
                val position: Position = boardObject.getPos();
                if (position.getLeft() < 0) {
                    position.setLeft(0);
                }
                if (position.getTop() < 0) {
                    position.setTop(0);
                }

            }
        )
    }
    
    def deduplicate(board: Board) {
        var previousIds: List[Int] = List[Int]();
        board.objects.foreach(
            boardObject => {
                if (previousIds.contains(boardObject.id)) {
                    boardObject.id = board.generateUniqueId()
                }
                previousIds ::= boardObject.id
            }
        )
    }

    def getBoard(id: String): Board = {
        val path = storagePathForBoard(id)

        return if (path.exists()) {
            val jackson = new ObjectMapper()
            val input = new FileInputStream(path)
            try {
                val board: Board = jackson.readValue(input, classOf[Board])
                board.objects.foreach(
                    b => {
                        if (b.kind == "stickie") {
                            b.kind = "sticky";
                        }
                        board.id_sequence = board.id_sequence.max(b.id);
                    }
                )
                
                board.onLoad()
                
                deduplicate(board)
                unhideContents(board)
                return board
            } finally {
                input.close
            }
        } else {
            val newBoard = new Board(id)
            addDefaultColumnsToBoard(newBoard)
            return newBoard
        }
    }

    def saveBoard(board: Board) {
        if(board.objects.isEmpty) {
            addDefaultColumnsToBoard(board)
        }
        
        val path = storagePathForBoard(board.getName())
        val jackson = new ObjectMapper()
        path.getParentFile().mkdirs()
        val output = new FileOutputStream(path);
        try {
            jackson.writeValue(output, board);
            println("Wrote board to " + path)
        } finally {
            output.close
        }

    }

    def listBoards(): Array[String] = {
        dataPath.list().sorted
    }

    def addDefaultColumnsToBoard(board: Board) = {
        board.addObject(new BoardObject(id = 1, backlogId = 0, backlogName = "", storyId= "", title = "", name = "OnDeck", extraNotes = "", kind = "column"))
        board.addObject(new BoardObject(id = 2, backlogId = 0, backlogName = "", storyId= "", title = "", name = "InProgress", extraNotes = "", kind = "column"))
        board.addObject(new BoardObject(id = 3, backlogId = 0, backlogName = "", storyId= "", title = "", name = "DevDone", extraNotes = "", kind = "column"))
        board.addObject(new BoardObject(id = 4, backlogId = 0, backlogName = "", storyId= "", title = "", name = "QRing", extraNotes = "", kind = "column"))
        board.addObject(new BoardObject(id = 5, backlogId = 0, backlogName = "", storyId= "", title = "", name = "ReadyForDemo", extraNotes = "", kind = "column"))
        board.addObject(new BoardObject(id = 6, backlogId = 0, backlogName = "", storyId= "", title = "", name = "NeedsSoxing", extraNotes = "", kind = "column"))
        board.addObject(new BoardObject(id = 7, backlogId = 0, backlogName = "", storyId= "", title = "", name = "Soxing", extraNotes = "", kind = "column"))
        board.addObject(new BoardObject(id = 8, backlogId = 0, backlogName = "", storyId= "", title = "", name = "NeedsDeploy", extraNotes = "", kind = "column"))
        board.addObject(new BoardObject(id = 9, backlogId = 0, backlogName = "", storyId= "", title = "", name = "NeedsClosing", extraNotes = "", kind = "column"))
    }

}
