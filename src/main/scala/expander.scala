import chisel3._
import chisel3.util._

class Expander(double: Boolean) extends Module {
  val io = IO(new Bundle {
    // Inputs ---------
    val block = Input(UInt(512.W))
    val enable = Input(Bool())
    // Outputs ----------
    val w = Output(Vec(64, UInt(32.W)))
    val finished = Output(Bool())
  })

  private object State extends ChiselEnum {
    val Idle, working, Finished = Value
  }
  private val state = RegInit(State.Idle)

  val w = RegInit(VecInit(Seq.fill(64)(0.U(32.W))))

  io.w := w
  io.finished := (state === State.Finished)

  // index for w[16..63]
  val i = RegInit(16.U(6.W)) 

  def rotateRight(value: UInt, shift: UInt): UInt = {
    // value = 0111, shift = 3
    // 0000 | value << 4 - 3
    value >> shift | value << (32.U - shift)
  }

  switch(state) {
    is(State.Idle) {
      when(io.enable) {
        // TODO assign correctly
        // w := io.block.asTypeOf(Vec(16, UInt(32.W))) // first 16 words from input block
        for (i <- 0 until 16) {
          w(i) := io.block(511 - i * 32, 480 - i * 32)
        }
        state := State.working
      }
    }
    is(State.working) {
      // Compute w[16..63]
      // if double is true, compute two words per cycle
      var stopValue = 63.U
      if (double) {
        val w15 = w(i - 15.U)
        val w2 = w(i - 2.U)

        val s0 = rotateRight(w15, 7.U) ^ rotateRight(w15, 18.U) ^ (w15 >> 3.U)
        val s1 = rotateRight(w2, 17.U) ^ rotateRight(w2, 19.U) ^ (w2 >> 10.U)
        w(i) := s1 +% w(i - 7.U) +% s0 +% w(i - 16.U)

        val w15_2 = w(i + 1.U - 15.U)
        val w2_2 = w(i + 1.U - 2.U)

        val s0_2 = rotateRight(w15_2, 7.U) ^ rotateRight(w15_2, 18.U) ^ (w15_2 >> 3.U)
        val s1_2 = rotateRight(w2_2, 17.U) ^ rotateRight(w2_2, 19.U) ^ (w2_2 >> 10.U)
        w(i + 1.U) := s1_2 +% w(i + 1.U - 7.U) +% s0_2 +% w15
        stopValue = 62.U
      } else {
        val w15 = w(i - 15.U)
        val w2 = w(i - 2.U)

        val s0 = rotateRight(w15, 7.U) ^ rotateRight(w15, 18.U) ^ (w15 >> 3.U)
        val s1 = rotateRight(w2, 17.U) ^ rotateRight(w2, 19.U) ^ (w2 >> 10.U)
        w(i) := s1 +% w(i - 7.U) +% s0 +% w(i - 16.U)
      }

      when(i === stopValue) {
        state := State.Finished
        // reset for next time
        i := 16.U 
      } .otherwise {
        i := i + 1.U
      }

    }
    is(State.Finished) {
      when(!io.enable) {
        state := State.Idle
      }
    }
  }
}
