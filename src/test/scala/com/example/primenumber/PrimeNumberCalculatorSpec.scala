package com.example.primenumber

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PrimeNumberCalculatorSpec extends AnyWordSpec with Matchers {

  "For the number -4 should return an empty list []" in {
    PrimeNumberCalculator.calculate(-4) shouldBe List()
  }
  "For the number 3 should return the list of prime numbers [2, 3]" in {
    PrimeNumberCalculator.calculate(3) shouldBe List(2, 3)
  }
  "For the number 10 should return the list of prime numbers [2, 3, 5, 7]" in {
    PrimeNumberCalculator.calculate(10) shouldBe List(2, 3, 5, 7)
  }
  "For the number 17 should return the list of prime numbers [2, 3, 5, 7, 11, 13, 17]" in {
    PrimeNumberCalculator.calculate(10) shouldBe List(2, 3, 5, 7, 11, 13, 17)
  }
}
