import chisel3._
import chisel3.util._

class preprocessor(val width: Int = 8) extends Module {
  val io = IO(new Bundle {
  val password = Input(UInt((width * 8).W))
  val block    = Output(UInt(512.W))
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
  // Build the padded message when password is received
  when (inputReceived) {
    // Build from right to left (LSB to MSB):
    // Bits [63:0] = Length (L)
    // Bits [63+K:64] = zeros (implicitly 0)
    // Bit [64+K] = 1
    // Bits [511:65+K] = password
    
    val totalShift = K + 64.U
    
    // Append '1' bit to password, then shift to correct position
    val messageWith1 = Cat(passwordReg, 1.U(1.W))
    val messageShifted = messageWith1 << totalShift
    
    // Combine: message with '1' bit (shifted) OR length in bottom 64 bits
    paddedMessage := messageShifted | L
  }

    io.block := paddedMessage

}