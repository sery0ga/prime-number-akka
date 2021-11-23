package com.example.primenumber

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, LogCapturing}
import akka.actor.typed.ActorSystem
import akka.event.slf4j.Logger
import akka.stream.scaladsl.Sink
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

class PrimeNumberServiceImplSpec
  extends AnyWordSpec
  with BeforeAndAfterAll
  with Matchers
  with ScalaFutures
  with LogCapturing {

  val testKit = ActorTestKit()

  implicit val patience: PatienceConfig = PatienceConfig(scaled(5.seconds), scaled(100.millis))

  implicit val system: ActorSystem[_] = testKit.system

  override def afterAll(): Unit = {
    testKit.shutdownTestKit()
  }

  "PriceNumberServiceImpl" should {
    "reply to single request" in {
      val reply = new PrimeNumberServiceImpl(Logger.root).giveNumbers(PrimeNumberRequest(1314)).take(5).runWith(Sink.seq)
      reply.futureValue.map(_.number) shouldBe Vector(2, 3, 5, 7, 11)
    }
  }
}
