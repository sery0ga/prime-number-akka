package com.example.primenumber

import akka.NotUsed
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PrimeNumberCalculatorSpec extends AnyWordSpec with Matchers
  with ScalaFutures {

  val testKit = ActorTestKit()

  implicit val system: ActorSystem[_] = testKit.system

  "For the number Int.MaxValue should return an error" in {
    PrimeNumberCalculator.calculate(Int.MaxValue) shouldBe Left("The selected limit of 2147483647 is too high. Choose a smaller number")
  }
  "For the number -4 should return an empty list []" in {
    toValue(PrimeNumberCalculator.calculate(-4)) shouldBe List()
  }
  "For the number 3 should return the list of prime numbers [2, 3]" in {
    toValue(PrimeNumberCalculator.calculate(3)) shouldBe List(2, 3)
  }
  "For the number 10 should return the list of prime numbers [2, 3, 5, 7]" in {
    toValue(PrimeNumberCalculator.calculate(10)) shouldBe List(2, 3, 5, 7)
  }
  "For the number 17 should return the list of prime numbers [2, 3, 5, 7, 11, 13, 17]" in {
    toValue(PrimeNumberCalculator.calculate(17)) shouldBe List(2, 3, 5, 7, 11, 13, 17)
  }
  "For the number 31231312 the first 10 numbers should be [2, 3, 5, 7, 11, 13, 17]" in {
    val value = PrimeNumberCalculator.calculate(31231312)
    toValue(value.map(_.take(10))) shouldBe List(2, 3, 5, 7, 11, 13, 17, 19, 23, 29)
  }

  private def toValue(source: Either[String, Source[Int, NotUsed]]): Seq[Int] =
    source match {
      case Right(source) => source.runWith(Sink.seq).futureValue
      case Left(_) => Seq()
    }
}
