import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import TestHelper._

class cyclicSha256Test extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "cyclicSha256"

  it should "handle 'abc' message" in {
    test(new sha256Cyclic()) { dut =>
      val msg = Array[Byte]('a'.toByte, 'b'.toByte, 'c'.toByte)
      val hw = runAndGetHashHexCyclic(dut, msg)
      val sw = goldenSha256(msg)
      assert(hw == sw, s"Hardware: $hw, Software: $sw")
      println(s"Hardware: $hw, Software: $sw")
    }
  }
}
