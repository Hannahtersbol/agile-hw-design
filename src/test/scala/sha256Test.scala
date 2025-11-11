import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class sha256Test extends AnyFlatSpec with ChiselScalatestTester {
  "sha256" should "connect preprocessor and expander correctly" in {
    test(new sha256(width = 8)) { c =>
  val inputValue = BigInt("41", 16)
  c.io.password.poke(inputValue.U)

  // Wait a few cycles for preprocessor to update
  c.clock.step(5)

  println("\nExpanded words (W[0..63]):")
  for (i <- 0 until 64) {
    println(f"W($i) = 0x${c.io.w_out(i).peek().litValue}%08x")
  }
}
}
}