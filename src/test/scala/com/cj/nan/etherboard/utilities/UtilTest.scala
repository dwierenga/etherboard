package com.cj.nan.etherboard.utilities

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{GivenWhenThen, Matchers, FunSuite}



@RunWith(classOf[JUnitRunner])
class UtilTest extends FunSuite  with Matchers with GivenWhenThen {


  val util = new Util

  test("parseHttpForm should not fail for empty string"){

    // given
    //when
    val result = util.parseHttpForm("")


    //then
    result.size should equal(0)

  }

  test("parseHttpForm should work for valid string"){

    // given
    //when
    val result = util.parseHttpForm("foo=1&bar=2")


    //then
    result.size should equal(2)

  }

}
