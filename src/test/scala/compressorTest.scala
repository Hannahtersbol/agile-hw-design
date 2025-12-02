import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.security.MessageDigest
import TestHelper._

class compressorTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Compressor"

  it should "calculate the correct hash from 2 byte message" in {
    test(new compressor) { dut =>
      val message = Array[Byte](127, -1)
      val byteLength = message.length

      val msg = BigInt(1, message)
      val blockExp = expectedBlock(byteLength, msg)
      val wExp = expectedW(blockExp)

      // inject w[0..63] to compressor
      for (i <- 0 until 64) {
        dut.io.w(i).poke(wExp(i).U(32.W))
      }

      // start compression
      dut.io.enable.poke(true.B)
      dut.io.reset_hash.poke(false.B)

      // run clock until the finished bit is high
      while (!dut.io.finished.peek().litToBoolean) {
        dut.clock.step()
      }

      dut.io.enable.poke(false.B)
      dut.clock.step()

      // collect hash output
      val hw =
        hashToHex((0 until 8).map(i => dut.io.hash_out(i).peek().litValue))
      val sw = goldenSha256(message)
      assert(hw == sw, s"Hardware: $hw, Software: $sw")
    }
  }

  it should "calculate the correct block from random value" in {
    test(new compressor) { dut =>
      val message =
        "0ab0c83333333300000000000000000000000000000000000000000000000000000000000000000000000000000000000000aaaaaaa000000001"
      val blocksExp = expectedBlockCyclic(message)
      val byteLength = message.length
      dut.io.reset_hash.poke(true.B)
      dut.clock.step()
      dut.io.reset_hash.poke(false.B)
      for (i <- 0 until blocksExp.length) {
        val blockExp = blocksExp(i)
        println(blockExp.toString(16).reverse.padTo(128, '0').reverse)
        val wExp = expectedW(blockExp)

        // inject w[0..63] to compressor
        for (i <- 0 until 64) {
          dut.io.w(i).poke(wExp(i).U(32.W))
        }

        dut.io.enable.poke(true.B)
        // start compression
        dut.clock.step()

        // run clock until the finished bit is high
        while (!dut.io.finished.peek().litToBoolean) {
          dut.clock.step()
        }
        dut.io.enable.poke(false.B)
        dut.clock.step()
      }

      // collect hash output
      val hw =
        hashToHex((0 until 8).map(i => dut.io.hash_out(i).peek().litValue))
      // from hex string to byte array
      val swMessage = BigInt(message, 16).toByteArray
      val sw = goldenSha256(swMessage)
      assert(hw == sw, s"Hardware: $hw, Software: $sw")

    }
  }
}
