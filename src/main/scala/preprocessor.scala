import chisel3._
import chisel3.util._

class preprocessor(val width: Int = 8) extends Module {
  val io = IO(new Bundle {
    // inputs ----------------
    val enable = Input(Bool())
    val message_len = Input(UInt(32.W))
    val message = Input(UInt(32.W))
    val key = Input(UInt(32.W))
    // outputs ----------------
    val block = Output(UInt(512.W))
    val finished = Output(Bool())
    // comp logic ----------
    val allow_send = Input(Bool())
    val last_block = Output(Bool())
  })

  val passwordBits = width * 8  // Message length in bits
  val passwordReg = RegInit(0.U(passwordBits.W))
  val inputReceived = RegInit(false.B)
  val paddedMessage = RegInit(0.U(512.W))

  // Receive the password input (truncate to actual width)
  when (!inputReceived) {
    passwordReg := io.password(passwordBits - 1, 0)
    inputReceived := true.B
  }

  // Calculate the actual message length in bits
  val L = passwordBits.U  // Message length in bits

  // Calculate K = (447 - L) mod 512
  val K = (447.U - L) % 512.U

  // Structure: <password> 1 <K zeros> <L as 64-bit>
  when (inputReceived) {
    val totalShift = K + 64.U
    
    // Append '1' bit to password, then shift to correct position
    val messageWith1 = Cat(passwordReg, 1.U(1.W))
    val messageShifted = messageWith1 << totalShift
    
    // Combine: message with '1' bit (shifted) OR length in bottom 64 bits
    paddedMessage := messageShifted | L
  }

    io.block := paddedMessage

}