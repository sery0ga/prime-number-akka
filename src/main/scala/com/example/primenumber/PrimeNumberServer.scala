package com.example.primenumber

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import com.typesafe.config.ConfigFactory
import org.slf4j.Logger

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object PrimeNumberServer {

  def main(args: Array[String]): Unit = {
    // important to enable HTTP/2 in ActorSystem's config
    val conf = ConfigFactory.parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())
    val system = ActorSystem[Nothing](Behaviors.empty, "PrimeNumberServer", conf)
    new PrimeNumberServer(system, akka.event.slf4j.Logger.root).run()
  }
}

/**
 * A prime number server serves one purpose: stream a list of prime numbers up to the given limit. The list is empty
 * if the limit is less than 2. Otherwise it's not :)
 */
class PrimeNumberServer(system: ActorSystem[_], logger: Logger) extends WithHttps {

  def run(): Future[Http.ServerBinding] = {
    implicit val sys = system
    implicit val ec: ExecutionContext = system.executionContext

    val service = PrimeNumberServiceHandler(new PrimeNumberServiceImpl(logger))

    val (host, port) = (
      system.settings.config.getString("server.host"),
      system.settings.config.getInt("server.port")
    )

    val bound = Http(system)
      .newServerAt(interface = host, port = port)
      .enableHttps(serverHttpContext(system.settings.config,
        "server.tls.cert-chain",
        "server.tls.private-key"))
      .bind(service)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))

    bound.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        logger.info(s"gPRC server bound to ${address.getHostString}:${address.getPort}")
      case Failure(ex) =>
        logger.error(s"gRPC server could not bind to $host:$port", ex)
        system.terminate()
    }

    bound
  }
}
