package com.cj.nan.etherboard

import org.httpobjects.header.request.RequestHeader
import org.httpobjects.path.Path
import org.httpobjects.test.MockRequest
import org.httpobjects.{Representation, Query, Request, HttpObject}
import org.httpobjects.HttpObject._
import org.httpobjects.DSL.Bytes
import org.junit.runner.RunWith
import org.scalatest.{GivenWhenThen, Matchers, FunSuite}
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class RestAPITest extends FunSuite with Matchers with GivenWhenThen {

  test("Create board returns new board resource") {

    // given
    val testHttpObject = new HttpObject("/board")
    val request = new Request {override def immutableCopy(): Request = ???

      override def header(): RequestHeader = ???

      override def hasRepresentation: Boolean = ???

      override def representation(): Representation = ???

      override def path(): Path = ???

      override def query(): Query = ???
    }

    //val request = new MockRequest(testHttpObject, "/board", HttpObject.Text("name=Test0&syncType=simple"))

    //val request:Request = new MockRequest(subject, "/echo", utf8Bytes("hi"));
    // when

    val response = testHttpObject.post(request)

    // then
    response should not be null
    //println(response.code())
    //val headers = response.header().length
    //println(headers)

  }

}
