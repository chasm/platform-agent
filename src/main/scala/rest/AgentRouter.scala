package org.munat.pagent.rest

import org.munat.pagent._
import org.munat.pagent.model._
import org.munat.pagent.Helpers._

import ru.circumflex._, core._, web._, freemarker._

import com.biosimilarity.lift.model.store._
import com.biosimilarity.lift.model.store.usage._
import com.biosimilarity.lift.model.store.xml._
import com.biosimilarity.lift.model.store.xml.datasets._
import com.biosimilarity.lift.lib._
import scala.util.continuations._ 
import java.net.URI
import java.util.UUID
import CCLDSL._
import PersistedMonadicTS._
import com.biosimilarity.lift.lib.SpecialKURIDefaults._
import mTT._

class AgentRouter extends RequestRouter("/rest/agents") {
  response.contentType("text/plain")
  
  get(pr("/:uuid/logs")) = {
    println("-----code: " + uri(1))
    println(session.get(uri(1)) match {
      case Some(r) => "FOUND SOMETHING: " + r.toString
      case None => "OH, NOES! FOUND NOTHING!"
    })
    Db.getLogs(session.getOrElse(uri(1), sendError(500)).asInstanceOf[AgentSession]) match {
      case Some(RBound(Some(Ground(json)),_)) => json
      case None => "None"
    }
  }
  
  get(pr("/:uuid/agents")) = {
    println("-----code: " + uri(1))
    Db.getAgents(session.getOrElse(uri(1), sendError(500)).asInstanceOf[AgentSession]) match {
      case Some(RBound(Some(Ground(json)),_)) => json
      case None => "None"
    }
  }
  
  get(pr("/:uuid/cnxns")) = {
    println("-----code: " + uri(1))
    Db.getCnxns(session.getOrElse(uri(1), sendError(500)).asInstanceOf[AgentSession]) match {
      case Some(RBound(Some(Ground(json)),_)) => json
      case None => "None"
    }
  }
  
}
