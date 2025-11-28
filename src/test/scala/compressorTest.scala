import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class compressorTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Compressor"

  it should "Count the amount of cycles" in {
    test(new compressor) { dut =>
      dut.io.enable.poke(true.B)
      dut.io.reset_hash.poke(false.B)
      dut.io.w.poke(VecInit(Seq.fill(64)(0.U(32.W))))
      dut.clock.step()
      dut.io.finished.expect(false.B)
      dut.clock.step(63)
      dut.io.finished.expect(true.B)
    }
  }
}
