import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ExpanderTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Expander"

  // Same expectedBlock you showed:
  private def expectedBlock(widthBytes: Int, password: BigInt): BigInt = {
    val L  = BigInt(widthBytes * 8)
    val K0 = (BigInt(447) - L) % BigInt(512)
    val K  = if (K0 < 0) K0 + 512 else K0
    val totalShiftInt = (K + 64).toInt

    val messageWith1   = (password << 1) | 1
    val messageShifted = messageWith1 << totalShiftInt
    val out            = messageShifted | L
    val mask512        = (BigInt(1) << 512) - 1
    out & mask512
  }

  private val mask32 = (BigInt(1) << 32) - 1

  private def rotr32(x: BigInt, n: Int): BigInt = {
    val xr = x & mask32
    ((xr >> n) | (xr << (32 - n))) & mask32
  }

  private def sigma0(x: BigInt): BigInt =
    rotr32(x, 7) ^ rotr32(x, 18) ^ (x >> 3)

  private def sigma1(x: BigInt): BigInt =
    rotr32(x, 17) ^ rotr32(x, 19) ^ (x >> 10)

  private def schedule(block: BigInt): Seq[BigInt] = {
    val W = Array.fill[BigInt](64)(BigInt(0))

    // w[0..15] according to your Expander's indexing:
    for (i <- 0 until 16) {
      val hi = 511 - 32*i
      val lo = hi - 31
      W(i) = (block >> lo) & mask32
    }

    // w[16..63]
    for (i <- 16 until 64) {
      val s0 = sigma0(W(i-15))
      val s1 = sigma1(W(i-2))
      W(i) = (W(i-16) + s0 + W(i-7) + s1) & mask32
    }

    W.toSeq
  }

  it should "match the golden message schedule for a padded block" in {
    test(new Expander) { dut =>
      // Example message: 3 bytes: 0x616263 ("abc")
      val bytes = 3
      val msg   = BigInt("616263", 16) // BigInt of your raw message
      val block = expectedBlock(bytes, msg)
      val Wexp  = schedule(block)

      dut.io.block.poke(block.U)
      dut.clock.step()

      for (i <- 0 until 64) {
        dut.io.w(i).expect(Wexp(i).U, s"w($i) mismatch")
      }
    }
  }
}
