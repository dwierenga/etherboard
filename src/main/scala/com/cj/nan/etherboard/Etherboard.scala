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
import org.apache.log4j.BasicConfigurator


object EtherboardMain {
    def main(args: Array[String]) {
      BasicConfigurator.configure();
    	Logger.getRootLogger.setLevel(Level.INFO)
    	Logger.getLogger("org.apache").setLevel(Level.ERROR)
      val configuration: Configuration = Configuration.read("configuration.json")
      
    	val dataPath = establishDataPathFromConfigurationOrPriorConvention(configuration)
    	println("Using data at " + dataPath)
    	
      val data = new BoardDaoImpl(dataPath)
      val server = new EtherboardServer(configuration, data)
    	
    	server.launch()
    }
    
    def establishDataPathFromConfigurationOrPriorConvention(configuration:Configuration):Path = {
      new Path(Option(configuration.dataDir).getOrElse("target/data"))
    }
}

