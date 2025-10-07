import chisel3._
import chisel3.util._

class sha256 extends Module {
  val io = IO(new Bundle {
    val password = Input(UInt(32.W))
    val width = Input(UInt(32.W))
    val collected = Output(UInt(512.W))
    val colLength = Output(UInt(32.W))
  })

    //importing modules
    val preprocessor = Module(new preprocessor())
    val compressor = Module(new compressor())
    val expander = Module(new expander())


    // Connect the preprocessor module
    preprocessor.io.password := io.password.asTypeOf(UInt(32.W))
    preprocessor.io.width := io.width.asTypeOf(UInt(32.W))
    preprocessor.io.block := 0.U.asTypeOf(UInt(512.W))
}