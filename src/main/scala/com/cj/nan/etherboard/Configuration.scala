package com.cj.nan.etherboard

import java.io.{FileWriter, File}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper


object Configuration {
    def read(fileName:String):Configuration = {
        val mapper = new ObjectMapper()
        mapper.registerModule(DefaultScalaModule)
        val configFile = new File(fileName)

        val config = if (configFile.exists()){
            mapper.readValue(configFile, classOf[Configuration])
        } else {
            val newConf = Configuration()

            configFile.createNewFile()
            mapper.writeValue(configFile, newConf)

            newConf
        }

        config

    }

    def main(args:Array[String]) {
        val configuration = Configuration()
        println("Port:" + configuration.port + "  websocketport:" + configuration.websocketPort)

    }
}

case class Configuration (
    port:Int = 40180, 
    websocketsEnabled:Boolean = true, 
    websocketPort:Int = 40181, 
    pluginClasses:List[String]=List(), 
    dataDir:String="etherboard-data")
