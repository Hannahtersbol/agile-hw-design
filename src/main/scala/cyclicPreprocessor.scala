import chisel3._
import chisel3.util._

class CyclicPreprocessor() extends Module {
  val io = IO(new Bundle {
    val enable        = Input(Bool())
    val message_word  = Input(UInt(32.W))
    val message_len   = Input(UInt(64.W)) // message length in bytes
    val allow_send    = Input(Bool())

    val block         = Output(UInt(512.W))
    val recieved      = Output(Bool())
    val last_block    = Output(Bool())
    val finished      = Output(Bool())
  })

  private object State extends ChiselEnum {
  	val Idle, Waiting, Reading, Fill, AddLength, Finished = Value
  }
  private val state = RegInit(State.Idle)

  val blockMem = RegInit(0.U(512.W))
  val m_len = RegInit(0.U(64.W))
  val total_mes_len = RegInit(0.U(64.W))
  val byte_count = RegInit(0.U(64.W))
  val done = RegInit(false.B)
  val last = RegInit(false.B)
  val test = RegInit(0.U(32.W))

  io.block := blockMem
  io.finished := (state === State.Finished)
  io.recieved := (state === State.Reading)
  io.last_block := last

  switch(state){
	  is(State.Idle){
	  	when(io.enable){
	  		state := State.Reading
        m_len := io.message_len
        total_mes_len := io.message_len
	  	}
	  }
    is(State.Waiting){
      when(io.enable){
	  		state := State.Reading
	  	}
    }
	  is(State.Reading){
      when(byte_count < m_len && byte_count < 64.U){
        blockMem := (blockMem << 32) + io.message_word
        byte_count := byte_count + 4.U
      }.elsewhen(byte_count === 64.U && byte_count <= m_len){
        blockMem := blockMem
        state := State.Finished
        m_len := m_len - 64.U
      }.elsewhen(byte_count === 64.U && byte_count > m_len){
        val shift_amt = ((byte_count - m_len) << 3)(8, 0) - 1.U
        blockMem := blockMem + (1.U(512.W) << shift_amt)
        state := State.Finished
        m_len := m_len - 64.U
      }.elsewhen(byte_count =/= 64.U && byte_count === m_len){
        blockMem := (blockMem << 32) + (1.U(512.W) << 31)
        byte_count := byte_count + 4.U
        state := State.Fill
      }.elsewhen(byte_count =/= 64.U && byte_count > m_len){
        val offset_bytes = (byte_count - m_len)(5, 0)
        val shift_amt = (offset_bytes << 3) + 31.U
        blockMem := (blockMem << 32) + (1.U(512.W) << shift_amt)
        byte_count := byte_count + 4.U
        state := State.Fill
      }
    }
    is(State.Fill){
      when(byte_count < 64.U){
        blockMem := blockMem << 32.U
        byte_count := byte_count + 4.U
      }.otherwise(
        when(m_len <= 56.U){
          state := State.AddLength
          done := true.B
        }.otherwise{
          state := State.Finished
          m_len := 0.U
        }
      )
    }
    is(State.AddLength){
      blockMem := blockMem + (total_mes_len << 3.U)
      last := true.B
      state := State.Finished
    }
	  is(State.Finished){
      byte_count := 0.U
      when(!io.enable){
        when(done){
          state := State.Idle
        }.otherwise(
          state := State.Waiting
        )
      }
	  }
  }
}

//80000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000018
//61626380000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000018