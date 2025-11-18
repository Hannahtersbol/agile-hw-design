import chisel3._
import chisel3.util._


class sha256(val width: Int = 8) extends Module {
  val io = IO(new Bundle {
    val password = Input(UInt((width * 8).W))
    val w_out    = Output(Vec(64, UInt(32.W))) // Final output: expanded message words
  })

  // Instantiate modules
  val preprocessor = Module(new preprocessor(width))
  val expander     = Module(new Expander())
  val compressor   = Module(new compressor())

  // Connect preprocessor
  preprocessor.io.password := io.password

  // Connect expander
  expander.io.block := preprocessor.io.block

  // connect compressor
  compressor.io.block := expander.io.w

  // Output from expander becomes the sha256 output
  io.w_out := compressor.io.hash_out
}