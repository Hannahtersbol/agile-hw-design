import chisel3._
import chisel3.util._

class Preprocessor(width: Int = 16) extends Module {
  val io = IO(new Bundle {
    val enable        = Input(Bool())
    val message_word  = Input(UInt((width * 8).W))
    val message_len   = Input(UInt(16.W)) // message length in bytes
    val allow_send    = Input(Bool())

    val block         = Output(UInt(512.W))
    val recieved      = Output(Bool())
    val last_block    = Output(Bool())
    val finished      = Output(Bool())
  })

  // === Registers ===
  val messageReg   = RegInit(0.U((width * 8).W))
  val lengthReg    = RegInit(0.U(16.W))
  val inputReceived = RegInit(false.B)

  val paddedAll    = RegInit(0.U(1024.W))  // up to 2 blocks
  val numBlocks    = RegInit(1.U(2.W))     // 1 or 2
  val blockIdx     = RegInit(0.U(2.W))
  val processing   = RegInit(false.B)

  // === Default outputs ===
  io.block      := 0.U
  io.recieved   := false.B
  io.last_block := false.B
  io.finished   := false.B

  // === Step 1: Capture input message ===
  when (io.enable && !inputReceived) {
    messageReg    := io.message_word
    lengthReg     := io.message_len
    inputReceived := true.B
    processing    := true.B
  }

  // === Step 2: Padding and block generation ===
  when (inputReceived && processing) {
    val L_bits = (lengthReg << 3).asUInt  // message length in bits
    val totalBitsNoPad = L_bits + 1.U + 64.U  // message + '1' + length field
    val twoBlocks = totalBitsNoPad > 512.U
    val totalLenBits = Mux(twoBlocks, 1024.U, 512.U)
    val k = totalLenBits - totalBitsNoPad  // number of zero bits to pad

    // Build the padded message (up to 1024 bits)
    val messageShifted = messageReg << (totalLenBits - L_bits)
    val oneBit = 1.U << (totalLenBits - L_bits - 1.U)
    val lengthField = L_bits.asUInt.pad(64) // 64-bit length at the end (zero-extended)
    val padded = messageShifted | oneBit | lengthField

    paddedAll := padded
    numBlocks := Mux(twoBlocks, 2.U, 1.U)
    blockIdx  := 0.U
    processing := false.B
  }

  // === Step 3: Output 512-bit blocks sequentially ===
  val block0 = paddedAll(1023, 512)
  val block1 = paddedAll(511, 0)

  // For a single 512-bit message, the padded data resides in the lower half.
  // For two blocks, the first block is the upper half, second is the lower half.
  val currentBlock = Mux(numBlocks === 1.U, block1, Mux(blockIdx === 0.U, block0, block1))

  when (inputReceived && !processing && io.allow_send) {
    io.block := currentBlock
    io.recieved := true.B
    io.last_block := (blockIdx === (numBlocks - 1.U))

    // Move to next block if needed
    when (blockIdx =/= (numBlocks - 1.U)) {
      blockIdx := blockIdx + 1.U
    } .otherwise {
      io.finished := true.B
      inputReceived := false.B
    }
  }
}
