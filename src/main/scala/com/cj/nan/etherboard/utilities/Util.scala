package com.cj.nan.etherboard.utilities

import java.io.ByteArrayOutputStream

import org.httpobjects.{Request, HttpObject, Response}


class Util {



  def parseHttpForm(input: String): Map[String, String] = {
    val parts = input.split("&").filter(!_.isEmpty)
    parts.map(s => {
      val pair = s.split("=")
      (pair(0) -> pair(1))
    }).toMap
  }

  def getRequestRepresentationString(request: Request): String ={
    val baos = new ByteArrayOutputStream ()
    request.representation ().write (baos)
    baos.toString
  }

  def getResponseDataString(response: Response): String ={
    val baos = new ByteArrayOutputStream ()
    response.representation ().write (baos)
    baos.toString
  }

}
