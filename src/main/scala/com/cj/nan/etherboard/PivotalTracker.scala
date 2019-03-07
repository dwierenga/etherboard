package com.cj.nan.etherboard

import java.io.{File => Path}
import scala.collection.JavaConversions._
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.annotation.JsonTypeInfo.As
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import java.io.ByteArrayOutputStream
import org.jboss.netty.logging.Log4JLoggerFactory
import org.apache.commons.logging.LogFactory
import org.apache.log4j.Logger
import org.apache.log4j.Level
import scala.xml.XML
import scala.util.Random
import scala.beans.BeanProperty

class PivotalTrackerBoard(@BeanProperty name: String,  @BeanProperty var toolSyncId: String, @BeanProperty var toolSyncKey: String, stuff: BoardObject*) extends Board(name, stuff:_*) {
	def this() = this(null, null, null)
    
    override def onLoad() = syncData
     
    def syncData() = {
      val httpClient = new DefaultHttpClient()
      val req = new HttpGet(s"http://www.pivotaltracker.com/services/v3/projects/${toolSyncId}/stories?filter=state%3Astarted,finished,delivered,accepted,rejected%20includedone%3Afalse")
      req.setHeader("X-TrackerToken", toolSyncKey)
      
      val response = httpClient.execute(req)
      val storiesToStreamOut = new ByteArrayOutputStream()
      response.getEntity().writeTo(storiesToStreamOut)
      
      val stories = pivotalTrackerStoriesFromXml(storiesToStreamOut.toString())
      
      stories.foreach(story => {
    	findObject(story.id) match {
          case Some(existingStory) =>
            existingStory.name = story.name
            existingStory.extraNotes = story.description
          case None =>
            addObject(new BoardObject(story.id, Integer.MIN_VALUE, "","","", story.name, story.description, "sticky"))
      	}
      })
    }
    
    def pivotalTrackerStoriesFromXml(xmlString:String):Seq[PivotalTrackerStory] = {
    	XML.loadString(xmlString) \ "story" map(s => {
			val id = s\"id"
			val url = s\"url"
			val name = s\"name"
			val desc = s\"description"
			
			new PivotalTrackerStory(Integer.parseInt(id.text), url.text, name.text, desc.text)
	    })
	}
}

case class PivotalTrackerStory(id:Int, url:String, name:String, description:String)
