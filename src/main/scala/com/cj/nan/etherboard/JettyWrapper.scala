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

import org.httpobjects.jetty.HttpObjectsJettyHandler
import org.httpobjects.{Request, HttpObject}
import org.httpobjects.freemarker.FreemarkerDSL._
import org.httpobjects.proxy
import org.apache.log4j.BasicConfigurator
import freemarker.cache.TemplateLoader
import java.util.regex.Pattern
import java.util.Locale
import freemarker.template.{DefaultObjectWrapper}
import java.net.{InetSocketAddress, InetAddress}
import org.httpobjects.DSL._
import org.httpobjects.header.response.LocationField
import java.io._
import org.httpobjects.util.{RequestQueryUtil, ClasspathResourcesObject}
import com.fasterxml.jackson.databind.ObjectMapper
import org.httpobjects.util.MimeTypeTool
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.core.`type`.TypeReference
import scala.actors.OutputChannel
import org.jboss.netty.channel.Channel
import org.jboss.netty.bootstrap.ConnectionlessBootstrap
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory
import java.util.concurrent.Executors


object JettyWrapper {

    def launchServer(boardDao: BoardDao) {
        BasicConfigurator.configure();
        val freemarker = freemarkerConfig();
        val configuration: Configuration = Configuration.read("configuration.json")
        val websocketsEnabled = configuration.websocketsEnabled
        val websocketPort = configuration.websocketPort

        if (websocketsEnabled) {
          new BoardRealtimeUpdateServer(websocketPort).run()
        }
        val lock = new Object()

        val sourcePlugins:List[Plugin] = configurePlugins(configuration)

        HttpObjectsJettyHandler.launchServer(configuration.port,
            new HttpObject("/") {
                override def get(req: Request) = OK(FreemarkerTemplate("ui.html", null, freemarker))
            },
            new HttpObject("/api/external/sourceType/{sourceType}/id/{sourceId}") {
                override def post(req: Request) = lock.synchronized {
                    val sourceType = req.path().valueFor("sourceType")
                    val sourceId = req.path().valueFor("sourceId")
                    val mapper = new ObjectMapper()
                    mapper.registerModule(DefaultScalaModule)
                    val sourceItems = sourcePlugins.flatMap {
                        plugin =>
                            if (plugin.canHandle(sourceType)) {
                                println("Can Handle")
                                Some(plugin.listItemSuggestions(sourceId))
                            }
                            else None
                    }
                    syncBoardsWithSourceChanges(sourceType, sourceId, sourceItems(0))
                    OK(Text("OK"));
                }
            },
            new ClasspathResourcesObject("/{resource*}", EtherboardMain.getClass()),
            new HttpObject("/board/{boardId}/objects") {
                override def get(req: Request) = lock.synchronized {

                    val boardId = req.path().valueFor("boardId")
                    val jackson = new ObjectMapper()

                    val board = boardDao.getBoard(boardId)
                    board.boardUpdatesWebSocket = "ws://%s:%d/websocket?boardName=%s".format(InetAddress.getLocalHost.getHostName, websocketPort, board.name)
                    OK(Json(jackson.writeValueAsString(board)));
                }

                override def post(req: Request) = lock.synchronized {
                    val boardId = req.path().valueFor("boardId")
                    val jackson = new ObjectMapper()
                    val bytes = new ByteArrayOutputStream()
                    req.representation().write(bytes)
                    val o = jackson.readValue(new ByteArrayInputStream(bytes.toByteArray()), classOf[BoardObject]);

                    val board = boardDao.getBoard(boardId)
                    val id: Int = board.generateUniqueId();
                    val result: BoardObject = new BoardObject(id, o)
                    board.addObject(result)
                    boardDao.saveBoard(board)
                    OK(Json(jackson.writeValueAsString(result)));
                }
            },
            new HttpObject("/board/{boardId}/objects/{objectId}") {
                override def put(req: Request) = lock.synchronized {
                    val boardId = req.path().valueFor("boardId")
                    val jackson = new ObjectMapper()
                    val bytes = new ByteArrayOutputStream()
                    req.representation().write(bytes)
                    val o = jackson.readValue(new ByteArrayInputStream(bytes.toByteArray()), classOf[BoardObject]);

                    val id = Integer.parseInt(req.path().valueFor("objectId"), 10)

                    val board = boardDao.getBoard(boardId)
                    val existing = board.findObject(id)

                    existing match {
                        case Some(existingBoardObject) =>
                            existingBoardObject.updateFrom(o);
                            boardDao.saveBoard(board)
                            OK(Json(jackson.writeValueAsString(existingBoardObject)))
                        case None =>
                            NOT_FOUND()
                    }
                }

                override def delete(req: Request) = lock.synchronized {
                    val boardId = req.path().valueFor("boardId")
                    val id = Integer.parseInt(req.path().valueFor("objectId"), 10)
                    val board = boardDao.getBoard(boardId)
                    board.removeObject(id);
                    boardDao.saveBoard(board);
                    NO_CONTENT();
                }
            },
            new HttpObject("/board") {
                override def get(req: Request) = {
                    val jackson = new ObjectMapper()
                    OK(Json(jackson.writeValueAsString(boardDao.listBoards())));
                }

                override def post(req: Request) = {
                    val baos = new ByteArrayOutputStream()
                    req.representation().write(baos)
                    val body = baos.toString
                    val parsedBody = parseHttpForm(body)

                    val newBoard = parsedBody("syncType") match {
                        case "pivotalTracker" => new PivotalTrackerBoard(parsedBody("name"), parsedBody("pivotalProjectId"), parsedBody("pivotalDevKey"))
                        case _ => new Board(parsedBody("name"))
                    }

                    boardDao.saveBoard(newBoard)

                    SEE_OTHER(new LocationField("/?board=" + newBoard.name))
                }
            },
            new HttpObject("/{resource*}") {
                override def get(req: Request) = {
                    val resource = req.path().valueFor("resource");
                    val data = this.getClass().getClassLoader().getResourceAsStream(resource);

                    if (data != null) {
                        OK(Bytes(new MimeTypeTool().guessMimeTypeFromName(resource), data))
                    } else {
                        null
                    }
                }
            },
            new HttpObject("/api/external/sources") {
                override def get(req: Request) = {
                    val sources = sourcePlugins.flatMap(_.listSources)
                    val mapper = new ObjectMapper()
                    mapper.registerModule(DefaultScalaModule)

                    OK(Bytes("application/json", mapper.writeValueAsBytes(sources)))
                }
            },
            new HttpObject("/api/external/sources/{id}/items/suggestions") {
                override def get(req: Request) = {
                    val sourceId = req.path().valueFor("id")
                    val suggestions = sourcePlugins.flatMap(_.listItemSuggestions(sourceId))
                    val mapper = new ObjectMapper()
                    mapper.registerModule(DefaultScalaModule)

                    OK(Bytes("application/json", mapper.writeValueAsBytes(suggestions)))
                }
            }
        )
    }

    def getSourceName(sourceType:String, externalSourceId:String):String = {
        val configuration: Configuration = Configuration.read("configuration.json")
        val sourcePlugins:List[Plugin] = configurePlugins(configuration)
        var sourceName = "Unknown Source"

        sourcePlugins.foreach(plugin => {
            if (plugin.canHandle(sourceType)) {
                sourceName = plugin.getSourceName(externalSourceId)
            }
        })
        sourceName

    }

    def syncBoardsWithSourceChanges(sourceType:String, externalSourceId:String, sourceItems:List[ExternalItemSuggestion]) {
        val sourceName = getSourceName(sourceType, externalSourceId)
        val boardIds = BoardDaoImpl.listBoards().toList

        for (name <- boardIds) {
            val board = BoardDaoImpl.getBoard(name)
            val boardObjects:java.util.List[BoardObject] = board.objects

            val iterator = boardObjects.iterator()
            while (iterator.hasNext) {
                val boardObject = iterator.next()
                if (boardObject.kind.equalsIgnoreCase("sticky")) {
                    for (sourceItem <- sourceItems ) {
                        if (boardObject.storyId == sourceItem.externalId) {
                            val message = createMessage(boardObject, sourceName, sourceItem.name)
                            BoardUpdatesActor ! MessageFromSource(name, message)
                        }
                    }
                }
            }
        }
    }

    def createMessage(boardObject:BoardObject, sourceName:String, msgContent:String) = {
        def quotesAround(field:String):String = {
            val quote = """""""
            quote + field + quote
        }
        def quoteField(elementType:String, element:String):String = {
            quotesAround(elementType) + ":" + quotesAround(element)
        }

        val backlogId = boardObject.backlogId
        val firstLine = msgContent
        val truncatedName = if(firstLine.length>100){
            firstLine.substring(0, 100) + "..."
        }else {
            firstLine
        }

        val stickyContent = s"""<a href="http://cjtools101.wl.cj.com:43180/backlog/$backlogId" style="background: #418F3A; color: white; display: block;"> $sourceName </a><div style="white-space:pre-line;"> $truncatedName </div>""".replaceAllLiterally(""""""", """\"""")

        val message =  s"""{${quoteField("type", "stickyContentChanged")},${quoteField("widgetId", "widget" + boardObject.id.toString)},${quoteField("content", stickyContent)},${quoteField("extraNotes", "")}}"""

        message
    }

    def freemarkerConfig() = {
        val cfg = new freemarker.template.Configuration();
        cfg.setTemplateLoader(new TemplateLoader() {

            override def getReader(source: Object, encoding: String): Reader = {
                new InputStreamReader(getClass().getClassLoader().getResourceAsStream(source.toString()), encoding);
            }

            override def getLastModified(arg0: Object) = {
                System.currentTimeMillis();
            }

            override def findTemplateSource(name: String) = {
                name.replaceAll(Pattern.quote("_en_US"), "");
            }

            override def closeTemplateSource(arg0: Object) {}
        });
        cfg.setEncoding(Locale.US, "UTF8");
        cfg.setObjectWrapper(new DefaultObjectWrapper());
        cfg
    }

    def parseHttpForm(input: String): Map[String, String] = {
        input.split("&").map(s => {
            val pair = s.split("=")
            (pair(0) -> pair(1))
        }).toMap
    }

    def configurePlugins(conf: Configuration): List[Plugin] = {
        println("Loading " + conf.pluginClasses)
        conf.pluginClasses.map(Class.forName(_).newInstance().asInstanceOf[Plugin])
    }

}
