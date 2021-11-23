package com.example.primenumber

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import org.slf4j.Logger

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ProxyService {

  def main(args: Array[String]): Unit = {
    // important to enable HTTP/2 in ActorSystem's config
    val conf = ConfigFactory.parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())
    val system = ActorSystem[Nothing](Behaviors.empty, "PrimeNumberProxyService", conf)
    new ProxyService(system, akka.event.slf4j.Logger.root).run()
  }
}

class ProxyService(system: ActorSystem[_], logger: Logger) extends WithHttps {

  def run(): Future[Http.ServerBinding] = {
    implicit val sys = system
    implicit val ec: ExecutionContext = system.executionContext

    val client = PrimeNumberServiceClient(GrpcClientSettings.fromConfig("primenumber.PrimeNumberService"))

    val (host, port) = (
      system.settings.config.getString("proxyService.host"),
      system.settings.config.getInt("proxyService.port")
    )

    val route = primePath(client)

    val bound = Http(system)
      .newServerAt(interface = host, port = port)
      .enableHttps(serverHttpContext(system.settings.config,
        "proxyService.tls.cert-chain",
        "proxyService.tls.private-key"))
      .bind(route)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))

    bound.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        logger.info(s"proxy server bound to ${address.getHostString}:${address.getPort}")
      case Failure(ex) =>
        logger.error(s"proxy server could not bind to $host:$port", ex)
        system.terminate()
    }

    bound
  }

  protected def primePath(client: PrimeNumberServiceClient)(implicit mat: Materializer, ec: ExecutionContext) =
    path("prime" / Remaining) { parameter =>
      get {
        complete {
          val responseStream = client
            .giveNumbers(PrimeNumberRequest(parameter.toInt, "some id"))
            .runFold(Seq[Int]())(_ :+ _.number)
            .map(_.mkString(","))
          responseStream.map(response => HttpEntity(ContentTypes.`text/plain(UTF-8)`, response))
        }
      }
    }
}
