package com.cj.nan.etherboard.pages

import org.httpobjects.jetty.HttpObjectsJettyHandler
import org.httpobjects.{Request, HttpObject}
import freemarker.template.DefaultObjectWrapper
import freemarker.cache.TemplateLoader
import java.io._
import org.httpobjects.freemarker.FreemarkerDSL._
import java.util.Locale
import java.util.regex.Pattern
import org.httpobjects.DSL._

class HomePage extends HttpObject("/") {
  
  private def buildFreemarker() = {
    import freemarker.template.Configuration
    val cfg = new Configuration();
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
  val freemarkerConfig = buildFreemarker();
  
  override def get(req: Request) = OK(FreemarkerTemplate("ui.html", null, freemarkerConfig))
  
  
  
}