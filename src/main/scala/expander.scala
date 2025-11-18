import chisel3._
import chisel3.util._

class Expander extends Module {
  val io = IO(new Bundle {
    val block = Input(UInt(512.W))
    val enable = Input(Bool())
    val w = Output(Vec(64, UInt(32.W)))
    val finished = Output(Bool())
  })

  def rotateRight(value: UInt, shift: UInt): UInt = {
    // value = 0111, shift = 3
    // 0000 | value << 4 - 3
    value >> shift | value << (32.U - shift)
  }

  val w = Wire(Vec(64, UInt(32.W)))

  for (i <- 0 until 16) {
    w(i) := io.block(511 - i * 32, 480 - i * 32)
  }
  for (i <- 16 until 64) {
    val w15 = w(i - 15)
    val w2 = w(i - 2)
    // xor: ^
    val s0 = rotateRight(w15, 7.U) ^ rotateRight(w15, 18.U) ^ (w15 >> 3.U)
    val s1 = rotateRight(w2, 17.U) ^ rotateRight(w2, 19.U) ^ (w2 >> 10.U)
    w(i) := s1 +% w(i - 7) +% s0 +% w(i - 16)
  }

  io.w := w
}
