package com.example.primenumber

//#import

import scala.concurrent.Future

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.BroadcastHub
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.MergeHub
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source

//#import

//#service-request-reply
//#service-stream
class PrimeNumberServiceImpl(system: ActorSystem[_]) extends PrimeNumberService {
  private implicit val sys: ActorSystem[_] = system

  //#service-request-reply
  val (inboundHub: Sink[PrimeNumberRequest, NotUsed], outboundHub: Source[PrimeNumberReply, NotUsed]) =
    MergeHub.source[PrimeNumberRequest]
      .map(request => PrimeNumberReply(1))
      .toMat(BroadcastHub.sink[PrimeNumberReply])(Keep.both)
      .run()
  //#service-request-reply

  override def giveNumber(request: PrimeNumberRequest): Future[PrimeNumberReply] = {
    Future.successful(PrimeNumberReply(1))
  }

  //#service-request-reply
  override def sayHelloToAll(in: Source[PrimeNumberRequest, NotUsed]): Source[PrimeNumberReply, NotUsed] = {
    in.runWith(inboundHub)
    outboundHub
  }
  //#service-request-reply
}
//#service-stream
//#service-request-reply
