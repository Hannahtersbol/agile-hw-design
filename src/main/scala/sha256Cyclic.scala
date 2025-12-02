import chisel3._
import chisel3.util._

class sha256Cyclic(val width: Int = 8, val compressor_sequencing: Int = 1, val debug_compressor: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val message_len  = Input(UInt(32.W))
    val message_word = Input(UInt((width * 8).W))
    val key          = Input(UInt(32.W))
    val enable       = Input(Bool())

    val hash_out = Output(Vec(8, UInt(32.W)))
    val finished = Output(Bool())

    val recieved = Output(Bool())

    val debug_port = if (debug_compressor) Some(Output(Vec(8, UInt(32.W)))) else None
  })

  private object State extends ChiselEnum {
    val Idle, Preprocessing, Expanding, Compressing, Finished = Value
  }
  private val state = RegInit(State.Idle)

  // Instantiate modules
  // val preprocessor = Module(new Preprocessor(width))
  val preprocessor = Module(new CyclicPreprocessor())
  val expander     = Module(new Expander(false))
  val compressor   = Module(new compressor(sequencing = compressor_sequencing, debug = debug_compressor))

  // Control signals
  val en_pre           = RegInit(false.B)
  val en_exp           = RegInit(false.B)
  val en_comp          = RegInit(false.B)
  val last_block       = RegInit(false.B)
  val reset_hash_pulse = RegInit(false.B)

  // Connections
  preprocessor.io.enable       := en_pre
  preprocessor.io.message_word := io.message_word
  preprocessor.io.message_len  := io.message_len(15, 0)  // truncate to 16 bits
  // Only allow send while we are in pre-processing and ready to capture a block
  preprocessor.io.allow_send   := (state === State.Preprocessing)
  io.recieved                  := preprocessor.io.recieved

  // Latch the current 512-bit block when preprocessor asserts 'recieved'
  val blockReg      = RegInit(0.U(512.W))
  val lastBlockReg  = RegInit(false.B)

  expander.io.enable := en_exp
  expander.io.block  := blockReg

  compressor.io.enable      := en_comp
  compressor.io.w           := expander.io.w
  compressor.io.reset_hash  := reset_hash_pulse

  io.hash_out := compressor.io.hash_out
  io.finished := (state === State.Finished)
  
  switch(state) {
    is(State.Idle) {
      when(io.enable) {
        state := State.Preprocessing
        en_pre := true.B
        reset_hash_pulse := true.B
      }
    }

    is(State.Preprocessing) {
      // Only reset hash at the start of a new message
      reset_hash_pulse := false.B
      // Capture each block as it becomes available
      when(preprocessor.io.finished) {
        blockReg := preprocessor.io.block
        lastBlockReg := preprocessor.io.last_block
        en_pre := false.B
        en_exp := true.B
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
        when(lastBlockReg) {
          lastBlockReg := false.B
          state := State.Finished
        } .otherwise {
          // Request next block from preprocessor without resetting hash
          en_pre := true.B
          reset_hash_pulse := false.B
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

  if(debug_compressor){
    io.debug_port.get(0) := compressor.io.debug_port.get(0)
    io.debug_port.get(1) := compressor.io.debug_port.get(1)
    io.debug_port.get(2) := compressor.io.debug_port.get(2)
    io.debug_port.get(3) := compressor.io.debug_port.get(3)
    io.debug_port.get(4) := compressor.io.debug_port.get(4)
    io.debug_port.get(5) := compressor.io.debug_port.get(5)
    io.debug_port.get(6) := compressor.io.debug_port.get(6)
    io.debug_port.get(7) := compressor.io.debug_port.get(7)
  }
}
