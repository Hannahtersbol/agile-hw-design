import chisel3._
import chisel3.util._

class sha256(val width: Int = 8) extends Module {
  val io = IO(new Bundle {
    val password = Input(UInt(32.W))
    val collected = Output(UInt(512.W))
    val colLength = Output(UInt(32.W))
  })

    //importing modules
    val preprocessor = Module(new preprocessor(width))
    // val compressor = Module(new compressor())
    // val expander = Module(new expander())


    // Connect the preprocessor module
    preprocessor.io.password := io.password
    io.collected := preprocessor.io.block
    io.colLength := 0.U  // Placeholder
}