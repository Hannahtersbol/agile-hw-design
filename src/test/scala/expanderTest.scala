import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import TestHelper._

class ExpanderTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Expander"

  private def testHelper(message: String, double: Boolean): TestResult = {
    test(new Expander(double)) { dut =>
      // "hello there" in binary (88 bits = 11 bytes)
      val pwBinary = message

      val bytes = pwBinary.length / 8
      val msg = BigInt(pwBinary)
      val blockExp = expectedBlock(bytes, msg)
      val Wexp = expectedW(blockExp)

      dut.io.block.poke(blockExp.U)
      dut.io.enable.poke(true.B)
      var cycle = 0
      while (!dut.io.finished.peek().litToBoolean) {
        // run clock until finished
        dut.clock.step()
        cycle += 1
      }
      // println(f"Input block: $block")

      for (i <- 0 until 64) {
        dut.io
          .w(i)
          .expect(
            Wexp(i).U,
            s"w($i) mismatch, expected ${Wexp(i)} got ${dut.io.w(i).peek().litValue}"
          )

        // Printing of
        // val wVal = dut.io.w(i).peek().litValue
        // println(s"Padded block (bin): w($i) = ${wVal.toString(2).reverse.padTo(32, '0').reverse}")
      }
      if (!double) {
        // One cycle for w[0..15] + 48 cycles for the calculation of w[16..63]
        assert(cycle == 49);
      } else {
        // One cycle for w[0..15] + (48/2)=24 cycles for the calculation of w[16..63]
        assert(cycle == 25)
      }
      // println(s"Number of Cycles: $cycle")
    }
  }

  it should "calculate the right w[0..63] from the message 'Hello there' in binary" in {
    testHelper(
      "0110100001100101011011000110110001101111001000000111010001101000011001010111001001100101",
      false
    )
  }

  it should "calculate w[0..63] from the message 'Hello there', in 25 cycles" in {
    testHelper(
      "0110100001100101011011000110110001101111001000000111010001101000011001010111001001100101",
      true
    )
  }

  it should "calculate w[0..63] from short message '001'" in {
    Seq(true, false).foreach { w =>
      testHelper(
        "001",
        w
      )
    }
  }

  it should "calculate w[0..63] from empty message" in {
    Seq(true, false).foreach { w =>
      testHelper(
        "0",
        w
      )
    }
  }

  it should "calculate w[0..63] from message of zero'es" in {
    Seq(true, false).foreach { w =>
      testHelper(
        "000000",
        w
      )
    }
  }

  it should "calculate w[0..63] from message of ones" in {
    Seq(true, false).foreach { w =>
      testHelper(
        "11111",
        w
      )
    }
  }
}
