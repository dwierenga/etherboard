package com.cj.nan.etherboard

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.annotation.JsonTypeInfo.As

import scala.beans.BeanProperty
import scala.collection.JavaConversions._

import scala.util.Random 

class Position(@BeanProperty var left: Int = 0, @BeanProperty var top: Int = 0) {
    def this() = this(left = 0, top = 0)
}

class Sticky(@BeanProperty var name: String, @BeanProperty var extraNotes: String,
				
    /*###################################################################################
     *##  WARNING: All these fields are gargbage ... they should not be here ... ask Stu */
	/*##*/  @BeanProperty var id: Int = Random.nextInt(),
    /*##*/  @BeanProperty() var kind: String = "sticky",
    /*##*/  @BeanProperty var pos: Position = null,
    /*##*/  @BeanProperty var height: Int = 0,
    /*##*/  @BeanProperty var width: Int = 0,
    /*##*/  @BeanProperty var contents: java.util.List[Sticky] = new java.util.ArrayList[Sticky]()
    /*##################################################################*/

) {
    def this() = this(name = null, extraNotes = null)
    def this(_name: String) = this(name = _name, extraNotes = null)
}

class
BoardObject(@BeanProperty var id: Int,
                  @BeanProperty var backlogId: Int,
                  @BeanProperty var backlogName: String,
                  @BeanProperty var storyId: String,
                  @BeanProperty var title: String,
                  @BeanProperty var name: String,
                  @BeanProperty var extraNotes: String,
                  @BeanProperty() var kind: String,
                  @BeanProperty var pos: Position = new Position(),
                  @BeanProperty var height: Int = 150,
                  @BeanProperty var width: Int = 150,
                  @BeanProperty var contents: java.util.List[Sticky] = new java.util.ArrayList[Sticky]()) {

    if (kind == "stickie") {
        kind = "sticky"
    }

    def this() = this(id = Integer.MIN_VALUE, backlogId = Integer.MIN_VALUE, backlogName = null, storyId = null, title = null, name = null, extraNotes = null, kind = null, height = 150, width = 150)

    def this(_id: Int, other: BoardObject) = this(id = _id, backlogId = other.backlogId, backlogName = other.backlogName, storyId = other.storyId, title = other.title, name = other.name, extraNotes = other.extraNotes, kind = other.kind, pos = other.pos, height = other.height, width = other.width)

    def this(_id: Int) = this(id = _id, backlogId = Integer.MIN_VALUE, backlogName = null, storyId = null, title = null, name = null, extraNotes = null, kind = null, height = 150, width = 150)

    def updateFrom(other: BoardObject) {
        if (other != null) {
            backlogId = other.backlogId
            backlogName = other.backlogName
            storyId = other.storyId
            title = other.title
            pos = other.pos
            name = other.name
            extraNotes = other.extraNotes
            contents = other.contents
            height = other.height
            width = other.width
        }
    }

    def updateName(updatedName: String) {
        name = updatedName
    }

    override def equals(other: Any): Boolean = other match {
        case x: BoardObject =>
            this.id == x.id && this.backlogId == x.backlogId && this.backlogName == x.backlogName && storyId == x.storyId && title == x.title && this.name == x.name && this.extraNotes == x.extraNotes && this.kind == x.kind && pos.left == x.pos.left && pos.top == x.pos.top && height == x.height && width == x.width
        case _ => false
    }
}

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=As.PROPERTY, property="type", defaultImpl = classOf[Board])
@JsonSubTypes(Array(
        new JsonSubTypes.Type(value = classOf[Board], name="simple"),
        new JsonSubTypes.Type(value = classOf[PivotalTrackerBoard], name="pivotalTracker")
    ))
class Board(@BeanProperty var name: String, stuff: BoardObject*) {
    @BeanProperty var objects: java.util.List[BoardObject] = new java.util.ArrayList[BoardObject](java.util.Arrays.asList(stuff: _*))
    @BeanProperty var id_sequence: Int = objects.map(_.id).fold(0)((m, x) => m.max(x))
    @BeanProperty var boardUpdatesWebSocket = ""

    def this() = this(null)
    
    def onLoad() = {}	//no extra steps necessary on load

    def findObject(id: Int): Option[BoardObject] = {
    	objects.find(_.id == id)
    }

    def addObject(boardObject: BoardObject) = {
        objects.add(boardObject)
    }

    def removeObject(id: Int) = {
        objects = objects.filterNot(_.id == id)
    }

    def generateUniqueId(): Int = {
        id_sequence += 1
        id_sequence
    }

    def cloneObjectsFrom(otherBoard:Board) ={
      for (existingObject <- otherBoard.objects){
         this.addObject(new BoardObject(this.generateUniqueId(), existingObject))
      }
    }
}