import chisel3._
import chisel3.util._

class sha256[T <: Data](gen: T) extends Module {
  val io = IO(new Bundle {
    val password = Input(gen)
    val hash = Output(gen)
  })

  //Code here
}