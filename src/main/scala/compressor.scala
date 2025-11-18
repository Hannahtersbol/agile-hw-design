import chisel3._
import chisel3.util._

class compressor(val width: Int = 8) extends Module {
val io = IO(new Bundle {
	val enable = Input(Bool())
	val block  = Input(Vec(64, UInt(32.W)))
	val finished = Output(Bool())
	val hash_out = Output(Vec(8, UInt(32.W)))
})

// register to hold final hash; assign H to this register in the Finished state
private val hash_out_reg = RegInit(VecInit(Seq.fill(8)(0.U(32.W))))
io.hash_out := hash_out_reg

  private val H_values = Seq(
    0x6a09e667L, 0xbb67ae85L, 0x3c6ef372L, 0xa54ff53aL,
    0x510e527fL, 0x9b05688cL, 0x1f83d9abL, 0x5be0cd19L
  ).map(_.U(32.W))

  private val K = VecInit(Seq(
    0x428a2f98L, 0x71374491L, 0xb5c0fbcfL, 0xe9b5dba5L, 0x3956c25bL, 0x59f111f1L, 0x923f82a4L, 0xab1c5ed5L,
    0xd807aa98L, 0x12835b01L, 0x243185beL, 0x550c7dc3L, 0x72be5d74L, 0x80deb1feL, 0x9bdc06a7L, 0xc19bf174L,
    0xe49b69c1L, 0xefbe4786L, 0x0fc19dc6L, 0x240ca1ccL, 0x2de92c6fL, 0x4a7484aaL, 0x5cb0a9dcL, 0x76f988daL,
    0x983e5152L, 0xa831c66dL, 0xb00327c8L, 0xbf597fc7L, 0xc6e00bf3L, 0xd5a79147L, 0x06ca6351L, 0x14292967L,
    0x27b70a85L, 0x2e1b2138L, 0x4d2c6dfcL, 0x53380d13L, 0x650a7354L, 0x766a0abbL, 0x81c2c92eL, 0x92722c85L,
    0xa2bfe8a1L, 0xa81a664bL, 0xc24b8b70L, 0xc76c51a3L, 0xd192e819L, 0xd6990624L, 0xf40e3585L, 0x106aa070L,
    0x19a4c116L, 0x1e376c08L, 0x2748774cL, 0x34b0bcb5L, 0x391c0cb3L, 0x4ed8aa4aL, 0x5b9cca4fL, 0x682e6ff3L,
    0x748f82eeL, 0x78a5636fL, 0x84c87814L, 0x8cc70208L, 0x90befffaL, 0xa4506cebL, 0xbef9a3f7L, 0xc67178f2L
  ).map(_.U(32.W)))

  // Helper functions for SHA-256 operations
  private def rotateRight(x: UInt, n: Int): UInt = (x >> n) | (x << (32 - n))
  private def chF(x: UInt, y: UInt, z: UInt): UInt = (x & y) ^ (~x & z)
  private def majF(x: UInt, y: UInt, z: UInt): UInt = (x & y) ^ (x & z) ^ (y & z)
  private def bigSigma0(x: UInt): UInt = rotateRight(x, 2) ^ rotateRight(x, 13) ^ rotateRight(x, 22)
  private def bigSigma1(x: UInt): UInt = rotateRight(x, 6) ^ rotateRight(x, 11) ^ rotateRight(x, 25)
  private def smallSigma0(x: UInt): UInt = rotateRight(x, 7) ^ rotateRight(x, 18) ^ (x >> 3)
  private def smallSigma1(x: UInt): UInt = rotateRight(x, 17) ^ rotateRight(x, 19) ^ (x >> 10)

  // Working registers (a..h)
  val a = RegInit(0.U(32.W))
  val b = RegInit(0.U(32.W))
  val c = RegInit(0.U(32.W))
  val d = RegInit(0.U(32.W))
  val e = RegInit(0.U(32.W))
  val f = RegInit(0.U(32.W))
  val g = RegInit(0.U(32.W))
  val h = RegInit(0.U(32.W))

  // Hash state H[0..7]
  val H = RegInit(VecInit(H_values))

  // loop counter 0..63
  val loop_counter = RegInit(0.U(log2Ceil(64).W))

  private object State extends ChiselEnum {
    val Idle, Working, Finished = Value
  }
  private val state = RegInit(State.Idle)

  // finished output reflects Finished state
  io.finished := (state === State.Finished)

  // Main state machine
  switch(state) {
    is(State.Idle) {
      when(io.enable) {
        // load working registers from H and start rounds
        a := H(0)
        b := H(1)
        c := H(2)
        d := H(3)
        e := H(4)
        f := H(5)
        g := H(6)
        h := H(7)

        loop_counter := 0.U
        state := State.Working
      }
    }

    is(State.Working) {
      val S1 = bigSigma1(e)
      val ch = chF(e, f, g)
	// store input block into registers on the first round, then read from those regs
	val blockRegs = RegInit(VecInit(Seq.fill(64)(0.U(32.W))))
	when(loop_counter === 0.U) {
	  for (i <- 0 until 64) {
		blockRegs(i) := io.block(i)
	  }
	}
  
	val w_i = blockRegs(loop_counter)
      val temp1 = h + S1 + ch + K(loop_counter) + w_i
      val S0 = bigSigma0(a)
      val maj = majF(a, b, c)
      val temp2 = S0 + maj

      // update working registers
      h := g
      g := f
      f := e
      e := d + temp1
      d := c
      c := b
      b := a
      a := temp1 + temp2

      when(loop_counter === 63.U) {
        // finish compression and update H
        H(0) := H(0) + a
        H(1) := H(1) + b
        H(2) := H(2) + c
        H(3) := H(3) + d
        H(4) := H(4) + e
        H(5) := H(5) + f
        H(6) := H(6) + g
        H(7) := H(7) + h

        state := State.Finished
      }.otherwise {
        loop_counter := loop_counter + 1.U
      }
    }

    is(State.Finished) {
      when(!io.enable) {
        state := State.Idle
      }
    }
  }
  when(state === State.Finished) {
    hash_out_reg := H
  }
}