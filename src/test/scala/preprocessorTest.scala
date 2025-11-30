// PreprocessorTest.scala
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import TestHelper._

class preprocessorTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "preprocessor"

  /** Helper: step the clock until io.recieved is true or we hit a timeout */
  private def waitForRecieved(dut: Preprocessor, maxCycles: Int = 50): Unit = {
    var cycles = 0
    while (!dut.io.recieved.peek().litToBoolean && cycles < maxCycles) {
      dut.clock.step()
      cycles += 1
    }
    assert(
      dut.io.recieved.peek().litToBoolean,
      s"Timeout waiting for io.recieved after $cycles cycles"
    )
  }

  it should "pad an 8-byte input correctly" in {
    test(new Preprocessor(width = 8)) { dut =>
      val pw = BigInt("6162636465666768", 16) // 8 bytes

      dut.io.enable.poke(true.B)
      dut.io.message_len.poke(8.U)
      dut.io.message_word.poke(pw.U)
      dut.io.allow_send.poke(true.B)

      // wait until block is ready
      waitForRecieved(dut)

      val exp = expectedBlock(8, pw)
      dut.io.block.expect(exp.U(512.W))
    }
  }

  it should "pad a 1-byte input" in {
    test(new Preprocessor(width = 1)) { dut =>
      val pw = BigInt(0x41) // 'A'

      dut.io.enable.poke(true.B)
      dut.io.message_len.poke(1.U)
      dut.io.message_word.poke(pw.U)
      dut.io.allow_send.poke(true.B)

      waitForRecieved(dut)

      val exp = expectedBlock(1, pw)
      dut.io.block.expect(exp.U(512.W))
    }
  }

  it should "pad abc correctly" in {
    test(new Preprocessor(width = 3)) { dut =>
      val pw = BigInt("616263", 16)  // "abc"

      dut.io.enable.poke(true.B)
      dut.io.message_len.poke(3.U)
      dut.io.message_word.poke(pw.U)
      dut.io.allow_send.poke(true.B)

      waitForRecieved(dut)

      val exp = expectedBlock(3, pw)
      val output = dut.io.block.peek().litValue
      println(s"block of 'abc' (bits):\t${output.toString(2).reverse.padTo(512, '0').reverse}")
      println(s"should be (bits):\t${exp.toString(2).reverse.padTo(512, '0').reverse}")
      dut.io.block.expect(exp.U(512.W))
    }
  }

  it should "pad zero input for multiple widths" in {
    Seq(1, 4, 8, 16).foreach { w =>
      test(new Preprocessor(width = w)) { dut =>
        val pw = BigInt(0)

        dut.io.enable.poke(true.B)
        dut.io.message_len.poke(w.U)   // w bytes of 0s
        dut.io.message_word.poke(pw.U)
        dut.io.allow_send.poke(true.B)

        waitForRecieved(dut)

        val exp = expectedBlock(w, pw)
        dut.io.block.expect(exp.U(512.W))
      }
    }
  }

  it should "pad a non-aligned 5-byte input" in {
    test(new Preprocessor(width = 5)) { dut =>
      val pw = BigInt("0102030405", 16)

      dut.io.enable.poke(true.B)
      dut.io.message_len.poke(5.U)
      dut.io.message_word.poke(pw.U)
      dut.io.allow_send.poke(true.B)

      waitForRecieved(dut)

      val exp = expectedBlock(5, pw)
      dut.io.block.expect(exp.U(512.W))
    }
  }

  it should "pad an 11-byte input \"hello there\"" in {
    test(new Preprocessor(width = 11)) { dut =>
      val pw = BigInt("68656c6c6f207468657265", 16)  // "hello there"

      dut.io.enable.poke(true.B)
      dut.io.message_len.poke(11.U)
      dut.io.message_word.poke(pw.U)
      dut.io.allow_send.poke(true.B)

      waitForRecieved(dut)

      val exp = expectedBlock(11, pw)
      dut.io.block.expect(exp.U(512.W))
      println(f"Padded block: 0x${dut.io.block.peek().litValue}%0128x")
    }
  }

  it should "pad an 11-byte input \"hello there\" using binary input" in {
    test(new Preprocessor(width = 11)) { dut =>
      // "hello there" in binary (88 bits = 11 bytes)
      val pwBinary =
        "0110100001100101011011000110110001101111001000000111010001101000011001010111001001100101"
      val pw = BigInt(pwBinary, 2)  // Parse as binary (base 2)

      dut.io.enable.poke(true.B)
      dut.io.message_len.poke(11.U)
      dut.io.message_word.poke(pw.U)
      dut.io.allow_send.poke(true.B)

      waitForRecieved(dut)

      val exp = expectedBlock(11, pw)
      dut.io.block.expect(exp.U(512.W))
      
      val blockValue = dut.io.block.peek().litValue
      println(f"Padded block (hex): 0x${blockValue}%0128x")
      println(s"Padded block (bin): ${blockValue.toString(2).reverse.padTo(512, '0').reverse}")
      println(s"Input password (bin): ${pw.toString(2).reverse.padTo(88, '0').reverse}")
    }
  }
}
