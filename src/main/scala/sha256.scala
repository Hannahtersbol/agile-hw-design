import chisel3._
import chisel3.util._


class sha256(val width: Int = 8) extends Module {
  val io = IO(new Bundle {
    val message_len = Input(UInt(32.W))
    val message = Input(UInt(32.W))
    val key = Input(UInt(32.W))
    val enable = Input(Bool())

    val hash_out = Output(Vec(8, UInt(32.W)))
    val finished = Output(Bool())
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