import chisel3._
import chisel3.util._

class compressor(val sequencing: Int = 1) extends Module {
  val io = IO(new Bundle {
    // inputs ----------------
  	val enable = Input(Bool())
    val w  = Input(Vec(64, UInt(32.W)))
    val reset_hash = Input(Bool())
    // outputs ----------------
  	val finished = Output(Bool())
  	val hash_out = Output(Vec(8, UInt(32.W)))
  })
  private object State extends ChiselEnum {
    val Idle, Working, Finished = Value
  }

  private val state = RegInit(State.Idle)
  // values needed for SHA-256
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

  // loop counter 0..63
  val loop_counter = RegInit(0.U(32.W))
  val blockRegs = RegInit(VecInit(Seq.fill(64)(0.U(32.W))))
  // Hash state H[0..7]
  val H = RegInit(VecInit(H_values))
  // Working registers (a..h)
  val a = RegInit(0.U(32.W))
  val aWire = Wire(Vec(sequencing + 1, UInt(32.W)))
  aWire(0) := a
  val b = RegInit(0.U(32.W))
  val bWire = Wire(Vec(sequencing + 1, UInt(32.W)))
  bWire(0) := b
  val c = RegInit(0.U(32.W))
  val cWire = Wire(Vec(sequencing + 1, UInt(32.W)))
  cWire(0) := c
  val d = RegInit(0.U(32.W))
  val dWire = Wire(Vec(sequencing + 1, UInt(32.W)))
  dWire(0) := d
  val e = RegInit(0.U(32.W))
  val eWire = Wire(Vec(sequencing + 1, UInt(32.W)))
  eWire(0) := e
  val f = RegInit(0.U(32.W))
  val fWire = Wire(Vec(sequencing + 1, UInt(32.W)))
  fWire(0) := f
  val g = RegInit(0.U(32.W))
  val gWire = Wire(Vec(sequencing + 1, UInt(32.W)))
  gWire(0) := g
  val h = RegInit(0.U(32.W))
  val hWire = Wire(Vec(sequencing + 1, UInt(32.W)))
  hWire(0) := h

  val S1 = Wire(Vec(sequencing, UInt(32.W)))
  // Combinational hash round calculation
  for(i <- 0 until sequencing){
    S1(i) := bigSigma1(eWire(i))
    val ch = chF(eWire(i), fWire(i), gWire(i))
    val w_i = blockRegs(loop_counter+i.asUInt)
    val temp1 = (hWire(i) + S1(i) + ch + K(loop_counter+i.asUInt) + w_i)(31, 0)
    val S0 = bigSigma0(aWire(i))
    val maj = majF(aWire(i), bWire(i), cWire(i))
    val temp2 = (S0 + maj)(31, 0)

    aWire(i+1) := (temp1 + temp2)(31, i)
    bWire(i+1) := aWire(i)
    cWire(i+1) := bWire(i)
    dWire(i+1) := cWire(i)
    eWire(i+1) := (d + temp1)(31, 0)
    fWire(i+1) := eWire(i)
    gWire(i+1) := fWire(i)
    hWire(i+1) := gWire(i)
    when(loop_counter + i.asUInt === 63.U){
      H(0) := H(0) + aWire(i+1)
      H(1) := H(1) + bWire(i+1)
      H(2) := H(2) + cWire(i+1)
      H(3) := H(3) + dWire(i+1)
      H(4) := H(4) + eWire(i+1)
      H(5) := H(5) + fWire(i+1)
      H(6) := H(6) + gWire(i+1)
      H(7) := H(7) + hWire(i+1)
      state := State.Finished
    }
    loop_counter := loop_counter + sequencing.asUInt
  }

  // finished output reflects Finished state
  io.finished := (state === State.Finished)
  // Always output the current hash value
  io.hash_out := H

  // Main state machine
  switch(state) {
    is(State.Idle) {
      when(io.enable) {
        // Only reset H when explicitly requested and entering Working state
        when(io.reset_hash) {
          H := VecInit(H_values)
        }
        // load working registers from H and start rounds
        a := H(0)
        b := H(1)
        c := H(2)
        d := H(3)
        e := H(4)
        f := H(5)
        g := H(6)
        h := H(7)
        // load input w into registers
        for (i <- 0 until 64) {
		      blockRegs(i) := io.w(i)
	      }

        loop_counter := 0.U
        state := State.Working
      }
    }

    is(State.Working) {
      // Update working registers
      a := aWire(sequencing)
      b := bWire(sequencing)
      c := cWire(sequencing)
      d := dWire(sequencing)
      e := eWire(sequencing)
      f := fWire(sequencing)
      g := gWire(sequencing)
      h := hWire(sequencing)
    }

    is(State.Finished) {
      when(!io.enable) {
        state := State.Idle
      }
    }
  }
}