import chisel3._

object Main extends App {
  emitVerilog(new sha256())
}