package com.example.primenumber

import akka.NotUsed
import akka.stream.scaladsl.Source
import org.slf4j.Logger

/**
 * The implementation of PrimeNumberService to provide the prime numbers up to the given limit
 */
class PrimeNumberServiceImpl(logger: Logger) extends PrimeNumberService {

  /**
   * Returns the prime numbers up to the given limit
   * @param request The request containing the limit
   */
  override def giveNumbers(request: PrimeNumberRequest): Source[PrimeNumberReply, NotUsed] = {
    logger.info(s"[request id=${request.requestId}] producing the list of prime numbers up to ${request.number}")
    val numbers = PrimeNumberCalculator.calculate(request.number)
    numbers match {
      case Left(msg) =>
        logger.error(s"[request id=${request.requestId}] caused the error `$msg`")
        Source.failed(new RuntimeException(msg))
      case Right(numbers) => numbers.map(PrimeNumberReply(_))
    }
  }
}
