package com.example.primenumber

import akka.NotUsed
import akka.stream.scaladsl.Source
import org.slf4j.Logger

/**
 * The implementation of PrimeNumberService to provide the prime numbers up to the given limit
 *
 * After some considerations, I've decided to use a simple logic of returning an empty list if the limit is less than 2,
 * without additional error handling of negative numbers as I see no need for it.
 */
class PrimeNumberServiceImpl(logger: Logger) extends PrimeNumberService {

  /**
   * Returns the prime numbers up to the given limit
   * @param request The request containing the limit
   */
  override def giveNumbers(request: PrimeNumberRequest): Source[PrimeNumberReply, NotUsed] = {
    logger.info(s"[request id=${request.requestId}] producing the list of prime numbers up to ${request.number}")
    Source(PrimeNumberCalculator.calculate(request.number).map(PrimeNumberReply(_)))
  }
}
