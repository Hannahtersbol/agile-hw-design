import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


object TestHelper {
  // Preprocessor
  // Calculating the expected padded block for a given input password
  def expectedBlock(widthBytes: Int, password: BigInt): BigInt = {
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

   val mask32 = (BigInt(1) << 32) - 1

  private def rotr32(x: BigInt, n: Int): BigInt = {
    val xr = x & mask32
    ((xr >> n) | (xr << (32 - n))) & mask32
  }

   def sigma0(x: BigInt): BigInt =
    rotr32(x, 7) ^ rotr32(x, 18) ^ (x >> 3)

   def sigma1(x: BigInt): BigInt =
    rotr32(x, 17) ^ rotr32(x, 19) ^ (x >> 10)

  // Expander
  // Calculating the expected W[0..63] for a given padded block
  def expectedW(block: BigInt): Seq[BigInt] = {
    val W = Array.fill[BigInt](64)(BigInt(0))

  // If block is a single large integer and this extraction is intentional:
  // Your current approach is fine, but consider:
  for (i <- 0 until 16) {
    W(i) = ((block >> (480 - i * 32)) & mask32).toInt  // explicit cast if needed
  }

    // w[16..63]
    for (i <- 16 until 64) {
      val s0 = sigma0(W(i - 15))
      val s1 = sigma1(W(i - 2))
      W(i) = (W(i - 16) + s0 + W(i - 7) + s1) & mask32
    }

    W.toSeq
  }

}