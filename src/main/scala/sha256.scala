import chisel3._
import chisel3.util._


class sha256(val width: Int = 8) extends Module {
  val io = IO(new Bundle {
    val message_len = Input(UInt(32.W))
    val message_word = Input(UInt(32.W))
    val key = Input(UInt(32.W))
    val enable = Input(Bool())

    val hash_out = Output(Vec(8, UInt(32.W)))
    val finished = Output(Bool())

    // comp logic ----------
    val recieved = Output(Bool())
  })
  private object State extends ChiselEnum {
    val Idle, Preprocessing, Expanding, Compressing, Finished = Value
  }
  private val state = RegInit(State.Idle)

  // Instantiate modules
  val preprocessor = Module(new preprocessor(width))
  val expander     = Module(new Expander())
  val compressor   = Module(new compressor())

  // Control signals
  val en_pre = RegInit(false.B)
  val en_exp = RegInit(false.B)
  val en_comp = RegInit(false.B)
  val last_block = RegInit(false.B)

  // Connecting components
  preprocessor.io.enable := en_pre
  preprocessor.io.message_word := io.message_word
  preprocessor.io.message_len := io.message_len
  preprocessor.io.key := io.key
  io.recieved := preprocessor.io.recieved
  expander.io.enable := en_exp
  expander.io.block := preprocessor.io.block
  compressor.io.enable := en_comp
  compressor.io.w := expander.io.w
  io.hash_out := compressor.io.hash_out
  io.finished := (state === State.Finished)

  switch(state) {
    is(State.Idle) {
      when(io.enable) {
        state := State.Preprocessing
        en_pre := true.B
      }
    }
    is(State.Preprocessing) {
      when(preprocessor.io.finished) {
        en_pre := false.B
        en_exp := true.B
        last_block := preprocessor.io.last_block
        state := State.Expanding
      }
    }
    is(State.Expanding) {
      when(expander.io.finished) {
        en_exp := false.B
        en_comp := true.B
        state := State.Compressing
      }
    }
    is(State.Compressing) {
      when(compressor.io.finished) {
        en_comp := false.B
        when(last_block) {
          last_block := false.B
          state := State.Finished
        } .otherwise {
          // start preprocessing next block
          en_pre := true.B
          state := State.Preprocessing
        }
      }
    }
    is(State.Finished) {
      when(!io.enable) {
        state := State.Idle
      }
    }
  }
}