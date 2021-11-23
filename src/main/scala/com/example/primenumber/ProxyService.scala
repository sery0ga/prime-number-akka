package com.example.primenumber

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import org.slf4j.Logger

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ProxyService {

  def main(args: Array[String]): Unit = {
    val conf = ConfigFactory.parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())
    val system = ActorSystem[Nothing](Behaviors.empty, "PrimeNumberProxyService", conf)
    new ProxyService(system, akka.event.slf4j.Logger.root).run()
  }
}

/**
 * Http server that proxies requests to the PrimeNumberService. It supports only one GET method
 * '/prime/{number}' that returns the prime numbers up to the given number.
 */
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

  /**
   * Route that handles the GET method '/prime/{number}'
   */
  protected def primePath(client: PrimeNumberServiceClient)(implicit mat: Materializer, ec: ExecutionContext) =
    path("prime" / Remaining) { parameter =>
      extractRequest { request =>
        val requestId = request.hashCode().toString
        get {
          logger.info(s"[request id=$requestId] request to calculate prime numbers for limit $parameter")
          validateInput(parameter) match {
            case Left(error) => complete(StatusCodes.BadRequest -> error)
            case Right(number) =>
              onComplete(processCorrectNumber(number, client, requestId)) {
                case Success(response) => complete(response)
                case Failure(ex) =>
                  logger.error(s"[request id=$requestId] error while calculating numbers for parameter $parameter", ex)
                  complete(StatusCodes.BadRequest -> ex.getMessage)
              }
          }
        }
      }
    }

  /**
   * Call the PrimeNumberService to calculate the prime numbers up to the given number
   */
  protected def processCorrectNumber(number: Int,
                                     client: PrimeNumberServiceClient,
                                     requestId: String)(implicit mat: Materializer, ec: ExecutionContext) = {
    val responseStream = client
      .giveNumbers(PrimeNumberRequest(number, requestId))
      .runFold(Seq[Int]())(_ :+ _.number)
      .map(_.mkString(","))
    responseStream.map(HttpEntity(ContentTypes.`text/plain(UTF-8)`, _))
  }

  /**
   * Validates the input parameter
   */
  protected def validateInput(param: String): Either[String, Int] =
    try {
      val number = param.toInt
      if (number == Int.MaxValue) {
        Left(s"$param is too big. Choose a smaller integer between 0 and 2,147,483,646")
      } else if (number < 0) {
        Left(s"$param is a negative integer. Only positive integers allowed")
      } else {
        Right(number)
      }
    } catch {
      case _: NumberFormatException => Left(s"$param is not a valid integer. Allowed numbers are between 0 and 2,147,483,646")
    }
}
