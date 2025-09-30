import chisel3._
import chisel3.util._

class preprocessor[T <: Data](gen: T) extends Module {
  val io = IO(new Bundle {
    val password = Input(gen)
    val block = Output(gen)
  })

  // SHA-256 initial hash values
  val h0 = RegInit(0x6a09e667.U(32.W))
  val h1 = RegInit(0xbb67ae85.U(32.W))
  val h2 = RegInit(0x3c6ef372.U(32.W))
  val h3 = RegInit(0xa54ff53a.U(32.W))
  val h4 = RegInit(0x510e527f.U(32.W))
  val h5 = RegInit(0x9b05688c.U(32.W))
  val h6 = RegInit(0x1f83d9ab.U(32.W))
  val h7 = RegInit(0x5be0cd19.U(32.W))


  // Initialize array of round constants (first 32 bits of the fractional parts of the cube roots of the first 64 primes 2..311)
  val k = VecInit(Seq(
    0x428a2f98.U(32.W), 0x71374491.U(32.W), 0xb5c0fbcf.U(32.W), 0xe9b5dba5.U(32.W), 
    0x3956c25b.U(32.W), 0x59f111f1.U(32.W), 0x923f82a4.U(32.W), 0xab1c5ed5.U(32.W),
    0xd807aa98.U(32.W), 0x12835b01.U(32.W), 0x243185be.U(32.W), 0x550c7dc3.U(32.W), 
    0x72be5d74.U(32.W), 0x80deb1fe.U(32.W), 0x9bdc06a7.U(32.W), 0xc19bf174.U(32.W),
    0xe49b69c1.U(32.W), 0xefbe4786.U(32.W), 0x0fc19dc6.U(32.W), 0x240ca1cc.U(32.W), 
    0x2de92c6f.U(32.W), 0x4a7484aa.U(32.W), 0x5cb0a9dc.U(32.W), 0x76f988da.U(32.W),
    0x983e5152.U(32.W), 0xa831c66d.U(32.W), 0xb00327c8.U(32.W), 0xbf597fc7.U(32.W), 
    0xc6e00bf3.U(32.W), 0xd5a79147.U(32.W), 0x06ca6351.U(32.W), 0x14292967.U(32.W),
    0x27b70a85.U(32.W), 0x2e1b2138.U(32.W), 0x4d2c6dfc.U(32.W), 0x53380d13.U(32.W), 
    0x650a7354.U(32.W), 0x766a0abb.U(32.W), 0x81c2c92e.U(32.W), 0x92722c85.U(32.W),
    0xa2bfe8a1.U(32.W), 0xa81a664b.U(32.W), 0xc24b8b70.U(32.W), 0xc76c51a3.U(32.W), 
    0xd192e819.U(32.W), 0xd6990624.U(32.W), 0xf40e3585.U(32.W), 0x106aa070.U(32.W),
    0x19a4c116.U(32.W), 0x1e376c08.U(32.W), 0x2748774c.U(32.W), 0x34b0bcb5.U(32.W), 
    0x391c0cb3.U(32.W), 0x4ed8aa4a.U(32.W), 0x5b9cca4f.U(32.W), 0x682e6ff3.U(32.W),
    0x748f82ee.U(32.W), 0x78a5636f.U(32.W), 0x84c87814.U(32.W), 0x8cc70208.U(32.W), 
    0x90befffa.U(32.W), 0xa4506ceb.U(32.W), 0xbef9a3f7.U(32.W), 0xc67178f2.U(32.W)
  ))
  //Code here
}