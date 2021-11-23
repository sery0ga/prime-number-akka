package com.example.primenumber

//#import

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

//#import

//#client-request-reply
object PrimeNumberClient {

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem[_] = ActorSystem(Behaviors.empty, "PrimeNumberClient")
    implicit val ec: ExecutionContext = sys.executionContext

    val client = PrimeNumberServiceClient(GrpcClientSettings.fromConfig("primenumber.PrimeNumberService"))

    val names = if (args.isEmpty) List("Alice", "Bob") else args.toList

    names.foreach(singleRequestReply)

    def singleRequestReply(name: String): Unit = {
      val responseStream: Source[PrimeNumberReply, NotUsed] = client.giveNumbers(PrimeNumberRequest(13))
      val done: Future[Done] = {
        responseStream.runForeach(reply => println(s"$name got streaming reply: ${reply.number}"))
      }

      done.onComplete {
        case Success(_) =>
          println("streamingBroadcast done")
        case Failure(e) =>
          println(s"Error streamingBroadcast: $e")
      }
    }

  }

}
//#client-request-reply
