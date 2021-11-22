package com.example.primenumber

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
  def calculate(max: Int): List[Int] =
    if (max < 2) {
      List()
    } else {
      calculateUsingSieveOfEratosthenes(max)
    }

  protected def calculateUsingSieveOfEratosthenes(max: Int): List[Int] = {
    val primes = new Array[Boolean](max + 1)
    for (i <- 2 to max) {
      primes(i) = true
    }
    for (i <- 2 to max) {
      if (primes(i)) {
        for (j <- i * i to max by i) {
          primes(j) = false
        }
      }
    }
    primes.zipWithIndex.filter(_._1).map(_._2).toList
  }
}
