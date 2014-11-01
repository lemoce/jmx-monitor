package org.cerencio.jmx

import java.io.PrintStream
import javax.management.remote.{JMXConnector, JMXConnectorFactory, JMXServiceURL}
import javax.management.{MBeanServerConnection, ObjectName}

import akka.actor.{Actor, Props}
import com.sun.tools.attach.VirtualMachine

import scala.concurrent.duration._

case class Variavel (name: ObjectName, atributo: String)
case object TICK

object Indicador {

  def curry[A,B,C] (f:(A,B) => C): A => (B => C) = a => b => f(a,b)

  def pegaIndicador(server:MBeanServerConnection, variavel: Variavel) = variavel match {
    case Variavel(b,c) => server.getAttribute(b,c).toString
  }

}

class Scheduler(conn: MBeanServerConnection,
                writer: PrintStream, vars:Array[Variavel]) extends Actor {
  import org.cerencio.jmx.Indicador._

  override def receive: Receive = {
    case TICK =>
      writer.println(
        vars.map (
          curry(pegaIndicador)(conn)
        ).mkString(",")
      )
  }

}

/**
 * Created by 27190057897 on 30/10/14.
 */
object Main {
  //Catalina:type=Datasource,path=/alfresco,host=localhost,class=javax.sql.Datasource,name="jdbc/dataSource
  //Catalina:type=ThreadPool,name=http-8080
  //Catalina:type=ThreadPool,name=jk-8009

  val system = akka.actor.ActorSystem("system")
  import org.cerencio.jmx.Main.system.dispatcher

  def conectar(pid: String): (JMXConnector, MBeanServerConnection) = {

    //Montado a conexao com o processo
    val vm = VirtualMachine.attach(pid)
    var connectorAddr = Option(vm.getAgentProperties.getProperty("com.sun.management.jmxremote.localConnectorAddress"))

    if (connectorAddr.isEmpty) {
      val agent = vm.getSystemProperties.getProperty("java.home") + System.getProperty("file.separator") + "lib" +
        System.getProperty("file.separator") + "management-agent.jar"
      vm.loadAgent(agent)
      connectorAddr = Option(vm.getAgentProperties.getProperty("com.sun.management.jmxremote.localConnectorAddress"))
    }

    val url = new JMXServiceURL(connectorAddr.get)

    val conn = JMXConnectorFactory.connect(url, null)

    (conn, conn.getMBeanServerConnection)
    //Conexao com o servidor MBean aceita
  }

  def pegaVariaveis(args: Array[String]): Array[Variavel] = {
    if (args.isEmpty) Array.empty[Variavel]
    else {
      val name = new ObjectName(args(0))
      new Variavel(name, args(1)) +: pegaVariaveis(args.drop(2))
    }
  }

  def main (args: Array[String]) = {

    val (jmxConn, conn) = conectar(args(0))
    val newArgs = args.tail
    val vars = pegaVariaveis(newArgs)
    val myWriter = System.out

    val actor = system.actorOf(Props(classOf[Scheduler], conn, myWriter, vars))

    val cancelavel = system.scheduler.schedule(1 second, 1 second, actor, TICK)

    sys addShutdownHook {
      cancelavel.cancel
      jmxConn.close
    }

    while(true){}
  }

}
