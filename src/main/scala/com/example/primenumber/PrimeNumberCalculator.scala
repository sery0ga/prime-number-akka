package com.example.primenumber

import akka.NotUsed
import akka.stream.scaladsl.Source

/**
 * Sieve of Eratosthenes algorithm to calculate a prime number list up to the given number.
 *
 * We select the easiest algo possible as the purpose of the test task is not to show how to code
 * well-known algorithms (I hope) but to show how to create server/client gRPC services. The algorithm is a black bock
 * which could be easily replaced by a better one.
 *
 * @author Sergey Kotlov
 */
object PrimeNumberCalculator {
  def calculate(max: Int): Either[String, Source[Int, NotUsed]] = {
    if (max == Int.MaxValue) {
      Left(s"The selected limit of $max is too high. Choose a smaller number")
    } else {
      if (max < 2) {
        Right(Source.empty[Int])
      } else {
        Right(calculateUsingSieveOfEratosthenes(max))
      }
    }
  }

  protected def calculateUsingSieveOfEratosthenes(max: Int): Source[Int, NotUsed] = {
    val primes = new Array[Boolean](max + 1)
    for (i <- 2 to max) {
      primes(i) = true
    }
    for (i <- 2 to max) {
      // we need to make sure that we won't get out of bound by multiplication of our index
      if (primes(i) && i < Int.MaxValue / i) {
        for (j <- i * i to max by i) {
          primes(j) = false
        }
      }
    }
    // Though the calculation of primes in a boolean array is cheap from memory point of view, calculation of indexes
    // via easy methods like zipWithIndex is not on big numbers, and could easily lead to OutOfMemory error. To remove
    // this problem, we use a source.
    Source.unfold(2) { index =>
      if (index > max) {
        None
      } else {
        val nextIndex = index + 1
        if (primes(index)) {
          Some((nextIndex, index))
        } else {
          Some((nextIndex, -1))
        }
      }
    }.filter(_ > 0)
  }
}
