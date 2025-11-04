// PreprocessorTest.scala
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class preprocessorTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "preprocessor"

  // Pure Scala helper for expected block (no hardware created).
  private def expectedBlock(widthBytes: Int, password: BigInt): BigInt = {
    val L  = BigInt(widthBytes * 8)                // message length in bits
    val K0 = (BigInt(447) - L) % BigInt(512)
    val K  = if (K0 < 0) K0 + 512 else K0          // normalize into [0, 511]
    val totalShiftInt = (K + 64).toInt             // <-- BigInt -> Int for shift distance

    val messageWith1   = (password << 1) | 1
    val messageShifted = messageWith1 << totalShiftInt
    val out            = messageShifted | L
    val mask512        = (BigInt(1) << 512) - 1
    out & mask512
  }

  it should "pad an 8-byte input correctly (two-cycle latency)" in {
    test(new preprocessor(width = 8)) { dut =>
      val pw = BigInt("6162636465666768", 16)
      dut.io.password.poke(pw.U)
      dut.clock.step(); dut.clock.step()
      val exp = expectedBlock(8, pw)
      dut.io.block.expect(exp.U(512.W))
    }
  }

  it should "pad a 1-byte input" in {
    test(new preprocessor(width = 1)) { dut =>
      val pw = BigInt(0x41) // 'A'
      dut.io.password.poke(pw.U)
      dut.clock.step(); dut.clock.step()
      val exp = expectedBlock(1, pw)
      dut.io.block.expect(exp.U(512.W))
    }
  }

  it should "pad zero input for multiple widths" in {
    Seq(1, 4, 8, 16).foreach { w =>
      test(new preprocessor(width = w)) { dut =>
        val pw = BigInt(0)
        dut.io.password.poke(pw.U)
        dut.clock.step(); dut.clock.step()
        val exp = expectedBlock(w, pw)
        dut.io.block.expect(exp.U(512.W))
      }
    }
  }

  it should "pad a non-aligned 5-byte input" in {
    test(new preprocessor(width = 5)) { dut =>
      val pw = BigInt("0102030405", 16)
      dut.io.password.poke(pw.U)
      dut.clock.step(); dut.clock.step()
      val exp = expectedBlock(5, pw)
      dut.io.block.expect(exp.U(512.W))
    }
  }

    it should "pad an 11-byte input \"hello there\"" in {
    test(new preprocessor(width = 11)) { dut =>
      val pw = BigInt("68656c6c6f207468657265", 16)
      dut.io.password.poke(pw.U)
      dut.clock.step(); dut.clock.step()
      val exp = expectedBlock(11, pw)
      dut.io.block.expect(exp.U(512.W))
    }
  }
}
