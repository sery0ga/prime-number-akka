package com.example.primenumber

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ProxyServiceSpec
  extends AnyWordSpec
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures {

  val conf = ConfigFactory.parseString("akka.http.server.preview.enable-http2 = on")
    .withFallback(ConfigFactory.defaultApplication())

  val testKit = ActorTestKit(conf)
  val system: ActorSystem[_] = testKit.system

  override def afterAll: Unit = {
    testKit.shutdownTestKit()
  }

  "The processing of correct number" should {
    class TestProxyService extends ProxyService(system, system.log) {
      override def processCorrectNumber(number: Int,
                                        client: PrimeNumberServiceClient,
                                        requestId: String)(implicit mat: Materializer,
                                                              ec: ExecutionContext) =
        super.processCorrectNumber(number, client, requestId)(mat, ec)
    }
    trait TestClientTrait extends PrimeNumberServiceClient {
      def close(): Future[Done] = ???

      def closed(): Future[Done] = ???
    }
    "return a success entity if the prime number calculation succeeds" in {
      class TestClient extends TestClientTrait {
        override def giveNumbers(in: PrimeNumberRequest): Source[PrimeNumberReply, NotUsed] =
          Source.empty[PrimeNumberReply]
      }
      val testProxyService = new TestProxyService
      val result = testProxyService.processCorrectNumber(2, new TestClient, "id")
      result.futureValue shouldBe HttpEntity(ContentTypes.`text/plain(UTF-8)`, "")
    }
  }

  "Input validation" should {
    class TestProxyService extends ProxyService(system, system.log) {
      override def validateInput(param: String): Either[String, Int] = super.validateInput(param)
    }
    val service = new TestProxyService
    "fail as '3214/edit' is not an` integer`" in {
      service.validateInput("3214/edit").isLeft shouldBe true
    }
    "fail as 'edit' is not an integer" in {
      service.validateInput("edit").isLeft shouldBe true
    }
    "fail as '3.123' is an integer" in {
      service.validateInput("3.123").isLeft shouldBe true
    }
    "fail as '-3' is a negative integer" in {
      service.validateInput("-3") shouldBe Left("-3 is a negative integer. Only positive integers allowed")
    }
    "succeed as '3' is an integer" in {
      service.validateInput("3") shouldBe Right(3)
    }
    "fail as 2147483647 (Int.MaxValue) is too big number to handle for the calculator" in {
      service.validateInput(s"${Int.MaxValue}").isLeft shouldBe true
    }
    "fail as 2147483648 (Int.MaxValue + 1) is out of int range" in {
      service.validateInput("2147483648") shouldBe Left("2147483648 is not a valid integer. Allowed numbers are between 0 and 2,147,483,646")
    }
  }

}
