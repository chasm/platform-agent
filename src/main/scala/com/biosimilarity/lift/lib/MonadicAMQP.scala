// -*- mode: Scala;-*- 
// Filename:    MonadicAMQP.scala 
// Authors:     lgm                                                    
// Creation:    Fri Jan 21 13:10:54 2011 
// Copyright:   Not supplied 
// Description: 
// ------------------------------------------------------------------------

package com.biosimilarity.lift.lib

import com.biosimilarity.lift.lib.moniker._

import net.liftweb.amqp._

import scala.util.continuations._

import scala.concurrent.{Channel => Chan, _}
import scala.concurrent.cpsops._

import _root_.com.rabbitmq.client.{ Channel => RabbitChan,
				   ConnectionParameters => RabbitCnxnParams, _}
import _root_.scala.actors.Actor

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver

import java.net.URI
import _root_.java.io.ObjectInputStream
import _root_.java.io.ByteArrayInputStream
import _root_.java.util.Timer
import _root_.java.util.TimerTask

trait MonadicAMQPDispatcher[T]
 extends MonadicDispatcher[T] {
   self : WireTap with Journalist =>

  case class AMQPDelivery(
    tag   : String,
    env   : Envelope,
    props : AMQP.BasicProperties,
    body  : Array[Byte]
  )

  type ConnectionParameters = RabbitCnxnParams
  type Channel = RabbitChan
  type Ticket = Int
  type Payload = AMQPDelivery

  abstract class SerializedConsumer[T](
    val channel : Channel
  ) extends DefaultConsumer( channel ) {
    override def handleDelivery(
      tag : String,
      env : Envelope,
      props : AMQP.BasicProperties,
      body : Array[Byte]
    )
  }  

  override def acceptConnections(
    params : ConnectionParameters,
    host : String,
    port : Int
  ) =
    Generator {
      k : ( Channel => Unit @suspendable ) => {
	//shift {
	  //innerk : (Unit => Unit @suspendable) => {
	    val factory = new ConnectionFactory( params )
	    val connection = factory.newConnection( host, port )
	    val channel = connection.createChannel()
	    k( channel );
	  //}
	//}      
      }
    }   

  override def beginService(
    params : ConnectionParameters,
    host : String,
    port : Int
  ) = {
    beginService( params, host, port, "mult" )
  }

   def beginService(
    params : ConnectionParameters,
    host : String,
    port : Int,
    exQNameRoot : String
  ) = Generator {
    k : ( T => Unit @suspendable ) =>
      //shift {	
	blog(
	  "The rabbit is running... (with apologies to John Updike)"
	)

	for( channel <- acceptConnections( params, host, port )	) {
	  spawn {
	    // Open bracket
	    blog( "Connected: " + channel )
	    val ticket = channel.accessRequest( "/data" ) 
	    val qname = (exQNameRoot + "_queue")
	    channel.exchangeDeclare( ticket, exQNameRoot, "direct" )
	    channel.queueDeclare( ticket, qname )
	    channel.queueBind( ticket, qname, exQNameRoot, "routeroute" )
	  
	    for ( t <- readT( channel, ticket, exQNameRoot ) ) { k( t ) }

	    // Close bracket
	  }
	}
      //}
  }

  override def callbacks( channel : Channel, ticket : Ticket) = {
    callbacks( channel, ticket, "mult" )
  }

  def callbacks(
    channel : Channel, ticket : Ticket, exQNameRoot : String
  ) =
    Generator {
      k : ( Payload => Unit @suspendable) =>

      blog("level 1 callbacks")

      shift {
	outerk : (Unit => Any) =>
	  
	  object TheRendezvous
	   extends SerializedConsumer[T]( channel ) {
    	     override def handleDelivery(
	       tag : String,
	       env : Envelope,
	       props : AMQP.BasicProperties,
	       body : Array[Byte]
	     ) {
    		 spawn { 
  		   blog("before continuation in callback")
  		
    		   k( AMQPDelivery( tag, env, props, body ) )
    		
    		   blog("after continuation in callback")
    		   
		   outerk()
    		 }
    	     }
	   }
  	
  	blog("before registering callback")
  	
	channel.basicConsume(
	  ticket,
	  exQNameRoot + "_queue",
	  false,
	  TheRendezvous
	)
  	
  	blog("after registering callback")
  	// stop
      }
    }

  def readT( channel : Channel, ticket : Ticket ) = {
    readT( channel, ticket, "mult" )
  }

   def readT( channel : Channel, ticket : Ticket, exQNameRoot : String ) =
    Generator {
      k: ( T => Unit @suspendable) =>
	shift {
	  outerk: (Unit => Unit) =>
	    reset {
	      
  	      for (
		amqpD <- callbacks( channel, ticket, exQNameRoot )
	      )	{
  		val routingKey = amqpD.env.getRoutingKey
		val contentType = amqpD.props.contentType
		val deliveryTag = amqpD.env.getDeliveryTag
		val in =
		  new ObjectInputStream(
		    new ByteArrayInputStream( amqpD.body )
		  )
		val t = in.readObject.asInstanceOf[T];
		k( t )
		channel.basicAck(deliveryTag, false);

		// Is this necessary?
		shift { k : ( Unit => Unit ) => k() }
  	      }
  	  
  	      blog( "readT returning" )
  	      outerk()
	    }
	}
    }

}

trait MonadicJSONAMQPDispatcher[T]
{
  self : MonadicWireToTrgtConversion with WireTap with Journalist =>
  type Wire = String
  type Trgt = T
  
  override def wire2Trgt( wire : Wire ) : Trgt = {
    val xstrm = new XStream( new JettisonMappedXmlDriver )
    xstrm.fromXML( wire ).asInstanceOf[Trgt]
  }
}

trait AMQPUtilities {
  def stdCnxnParams : RabbitCnxnParams = {
    val params = new RabbitCnxnParams //new ConnectionParameters
    params.setUsername( "guest" )
    params.setPassword( "guest" )
    params.setVirtualHost( "/" )
    params.setRequestedHeartbeat( 0 )
    params
  }
}

object AMQPDefaults extends AMQPUtilities {
  implicit val defaultConnectionFactory : ConnectionFactory =
    new ConnectionFactory( defaultConnectionParameters )
  implicit val defaultConnectionParameters : RabbitCnxnParams =
    stdCnxnParams
  implicit val defaultHost : String = "localhost"
  implicit val defaultPort : Int = 5672
  implicit val defaultDispatching : Boolean = true
}

trait DefaultMonadicAMQPDispatcher[T]
extends MonadicAMQPDispatcher[T] {
  self : WireTap with Journalist =>
  //import AMQPDefaults._
  
  def host : String
  def port : Int

  override def tap [A] ( fact : A ) : Unit = {
    reportage( fact )
  }

  def acceptConnections()( implicit params : RabbitCnxnParams )
  : Generator[Channel,Unit,Unit] =
    acceptConnections( params, host, port )
  def beginService()( implicit params : RabbitCnxnParams )
  : Generator[T,Unit,Unit] =
    beginService( params, host, port )
  def beginService( exQNameStr : String )( implicit params : RabbitCnxnParams )
  : Generator[T,Unit,Unit] =
    beginService( params, host, port, exQNameStr )
}

class StdMonadicAMQPDispatcher[T](
  override val host : String,
  override val port : Int
) extends DefaultMonadicAMQPDispatcher[T](
) with WireTap
  with Journalist
  with ConfiggyReporting
  with ConfiggyJournal {
}

class StdMonadicJSONAMQPDispatcher[T](
  override val host : String,
  override val port : Int
) extends StdMonadicAMQPDispatcher[String]( host, port )
with MonadicJSONAMQPDispatcher[T]
with MonadicWireToTrgtConversion {
}

object StdMonadicAMQPDispatcher {
  def apply[T] (
    host : String, port : Int
  ) : StdMonadicAMQPDispatcher[T] = {
    new StdMonadicAMQPDispatcher(
      host, port
    )
  }
  def unapply[T](
    smAMQPD : StdMonadicAMQPDispatcher[T]
  ) : Option[(String,Int)] = {
    Some( ( smAMQPD.host, smAMQPD.port ) )
  }    
}

trait SemiMonadicJSONAMQPTwistedPair[T]
{  
  self : WireTap with Journalist =>

  import AMQPDefaults._
  
  //def srcURI : URI
  def srcURI : Moniker
  //def trgtURI : URI
  def trgtURI : Moniker

  var _jsonDispatcher : Option[StdMonadicJSONAMQPDispatcher[T]] = None
  def jsonDispatcher( handle : T => Unit )(
    implicit dispatchOnCreate : Boolean, port : Int
  ) : StdMonadicJSONAMQPDispatcher[T] = {
    jsonDispatcher( "mult", handle )
  }

  def jsonDispatcher( exQNameStr : String, handle : T => Unit )(
    implicit dispatchOnCreate : Boolean, port : Int
  ) : StdMonadicJSONAMQPDispatcher[T] = {
    _jsonDispatcher match {
      case Some( jd ) => jd
      case None => {
	val jd =
	  new StdMonadicJSONAMQPDispatcher[T]( srcURI.getHost, port )

	if ( dispatchOnCreate ) {
	  reset {
	    for(
	      msg <- jd.xformAndDispatch(
		jd.beginService( exQNameStr )
	      )
	    ) {
	      handle( msg )
	    }
	  }	
	}

	_jsonDispatcher = Some( jd )
	jd
      }
    }
  }

  var _jsonSender : Option[JSONAMQPSender] = None 
  
  def jsonSender()( implicit params : RabbitCnxnParams, port : Int ) : JSONAMQPSender = {
    jsonSender( "mult" )( params, port )
  }

  def jsonSender( exchNameStr : String )( implicit params : RabbitCnxnParams, port : Int ) : JSONAMQPSender = {
    _jsonSender match {
      case Some( js ) => js
      case None => {
	val js = new JSONAMQPSender(
	  new ConnectionFactory( params ),
	  trgtURI.getHost,
	  port,
	  exchNameStr,
	  "routeroute"
	)       

	_jsonSender = Some( js )

	js.start
	js
      }
    }
  }

  def send( contents : T ) : Unit = {
    for( amqp <- _jsonSender ) {
      val body =
	new XStream(
	  new JettisonMappedXmlDriver()
	).toXML( contents )	

      // tweet(
// 	(
// 	  this 
// 	  + " is sending "
// 	  + contents
// 	  + " encoded as "
// 	  + body
// 	  + " along "
// 	  + amqp
// 	)
//       )
      amqp ! AMQPMessage( body )
    }
  }  
}

class SMJATwistedPair[T](
  //override val srcURI : URI,
  override val srcURI : Moniker,
  //override val trgtURI : URI
  override val trgtURI : Moniker
) extends SemiMonadicJSONAMQPTwistedPair[T]
  with WireTap
  with Journalist
  with ConfiggyReporting
  with ConfiggyJournal {
    override def tap [A] ( fact : A ) : Unit = {
      reportage( fact )
    }
}

object SMJATwistedPair {
  def apply[T] (
    srcIPStr : String, trgtIPStr : String
  ) : SMJATwistedPair[T] = {
    new SMJATwistedPair[T](
      //new URI( "agent", srcIPStr, "/invitation", "" ),
      MURI( new URI( "agent", srcIPStr, "/invitation", "" ) ),
      //new URI( "agent", trgtIPStr, "/invitation", "" )
      MURI( new URI( "agent", trgtIPStr, "/invitation", "" ) )
    )
  }
  def unapply[T](
    smjatp : SMJATwistedPair[T]
  ) : //Option[(URI,URI)] = {
  Option[(Moniker,Moniker)] = {
    Some( ( smjatp.srcURI, smjatp.trgtURI ) )
  }    
}

package usage {
/* ------------------------------------------------------------------
 * Mostly self-contained object to support unit testing
 * ------------------------------------------------------------------ */ 

object MonadicAMQPUnitTest {
  import AMQPDefaults._
  case class Msg(
    s : String, i : Int, b : Boolean, r : Option[Msg]
  )
  
  lazy val msgStrm : Stream[Msg] =
    List( Msg( ("msg" + 0), 0, true, None ) ).toStream append (
      msgStrm.map(
	{
	  ( msg ) => {
	    msg match {
	      case Msg( s, i, b, r ) => {
		val j = i + 1
		Msg(
		  ("msg" + j) , j, ((j % 2) == 0), Some( msg )
		)
	      }
	    }
	  }
	}
      )
    )

  val srcIPStr = "10.0.1.5"
  val trgtIPStr = "10.0.1.9"
  
  trait Destination
  case class Src() extends Destination
  case class Trgt() extends Destination
  def smjatp( d : Destination ) = {
    val _smjatp =
      d match {
	case Src() => {
	  SMJATwistedPair[Msg]( srcIPStr, trgtIPStr )
	}
	case Trgt() => {
	  SMJATwistedPair[Msg]( trgtIPStr, srcIPStr )
	}
      }
    _smjatp.jsonDispatcher(
      ( msg ) => { println( "received : " + msg ) }
    )
    val msgs = msgStrm.take( 100 ).toList
    _smjatp.jsonSender
    // for( i <- 1 to 100 ) {
//       _smjatp.send( msgs( i - 1 ) )
//     }
    _smjatp
  }
}

}
