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

import org.jboss.netty.channel.Channel


import websockets.SimpleWebsocketServer
import org.jboss.netty.handler.codec.http.QueryStringDecoder
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import actors.Actor

class BoardRealtimeUpdateServer(port:Int)  {
    val websocketServer = new SimpleWebsocketServer(port, connectionHandler, messageHandler)

    def run() {
        BoardUpdatesActor.start()
        websocketServer.run
    }

    def connectionHandler(uri:String, channel:Channel) {
        val decoder: QueryStringDecoder = new QueryStringDecoder(uri)
        val boardName = decoder.getParameters.get("boardName").get(0)

        BoardUpdatesActor ! NewConnection(boardName, channel)
    }

    def messageHandler(message:String, channel:Channel) {
        BoardUpdatesActor ! MessageFromBoard(message, channel)
    }
}

case class NewConnection(board:String, channel:Channel)

case class MessageFromBoard(msg:String, channel:Channel)

case class MessageFromSource(board:String, msg:String)

//case class BoardUpdatesState(state:Map[String, List[Channel]]) {
//    def newConnection(board:String, channel:Channel):BoardUpdatesState = {
//
//    }
//}

object BoardUpdatesActor extends Actor {

    private var connectionsByBoard = Map[String, List[Channel]]()

    def act() {
        loop {
            react {
                case NewConnection(board:String, channel:Channel) => {
                    val connections:List[Channel] = connectionsByBoard.get(board).getOrElse(List())
                    connectionsByBoard += (board -> (channel :: connections))
                }
                case MessageFromBoard(message:String, meChannel:Channel) => {
                    def boardWeWantToSendTo(entry:(String, List[Channel])):Boolean = {
                        val (boardName, channels) = entry
                        val result =  channels.contains(meChannel)

                        result
                    }
                    val maybeBoardWeWantToSendTo:Option[(String, List[Channel])] = connectionsByBoard.find(boardWeWantToSendTo)
                    maybeBoardWeWantToSendTo match {
                        case Some((boardName, channels)) =>
                            def channelIsOpen(element:Channel):Boolean = element.isOpen
                            def channelIsNotMe(element:Channel):Boolean = element != meChannel
                            val openNotMeChannels:List[Channel] = channels.filter(channelIsOpen).filter(channelIsNotMe)
                            def forwardMessageToConnection(connection:Channel) {
                                connection.write(new TextWebSocketFrame(message))
                            }
                            openNotMeChannels.foreach(forwardMessageToConnection)
                        case None =>
                            println("could not find what board this connection belongs to")
                    }
                }
                case MessageFromSource(name:String, message:String) => {
                    def forwardMessageToConnection(connection:Channel) {
                        connection.write(new TextWebSocketFrame(message))
                    }
                    val maybeConnections:Option[List[Channel]] = connectionsByBoard.get(name)
                    maybeConnections match {
                        case Some(connections) =>
                            def channelIsOpen(element: Channel):Boolean = element.isOpen
                            connections.foreach( element => {
                                if (channelIsOpen(element)) {
                                    forwardMessageToConnection(element)
                                }
                            })
                        case None =>
                            println("Message from source to unknown board")
                    }
                }
                case _ => println("unknown message sent to BoardUpdateActor")
            }
        }
    }
}