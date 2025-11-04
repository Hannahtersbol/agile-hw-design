import chisel3._
import chisel3.util._

class sha256(val width: Int = 8) extends Module {
  val io = IO(new Bundle {
    val password = Input(UInt(32.W))
    val collected = Output(UInt(512.W))
    val colLength = Output(UInt(32.W))
  })

  // SHA-256 initial hash values
  val h0 = RegInit("h6a09e667".U(32.W))
  val h1 = RegInit("hbb67ae85".U(32.W))
  val h2 = RegInit("h3c6ef372".U(32.W))
  val h3 = RegInit("ha54ff53a".U(32.W))
  val h4 = RegInit("h510e527f".U(32.W))
  val h5 = RegInit("h9b05688c".U(32.W))
  val h6 = RegInit("h1f83d9ab".U(32.W))
  val h7 = RegInit("h5be0cd19".U(32.W))


  // Initialize array of round constants (first 32 bits of the fractional parts of the cube roots of the first 64 primes 2..311)
  val k = VecInit(Seq(
    "h428a2f98".U(32.W), "h71374491".U(32.W), "hb5c0fbcf".U(32.W), "he9b5dba5".U(32.W), 
    "h3956c25b".U(32.W), "h59f111f1".U(32.W), "h923f82a4".U(32.W), "hab1c5ed5".U(32.W),
    "hd807aa98".U(32.W), "h12835b01".U(32.W), "h243185be".U(32.W), "h550c7dc3".U(32.W), 
    "h72be5d74".U(32.W), "h80deb1fe".U(32.W), "h9bdc06a7".U(32.W), "hc19bf174".U(32.W),
    "he49b69c1".U(32.W), "hefbe4786".U(32.W), "h0fc19dc6".U(32.W), "h240ca1cc".U(32.W), 
    "h2de92c6f".U(32.W), "h4a7484aa".U(32.W), "h5cb0a9dc".U(32.W), "h76f988da".U(32.W),
    "h983e5152".U(32.W), "ha831c66d".U(32.W), "hb00327c8".U(32.W), "hbf597fc7".U(32.W), 
    "hc6e00bf3".U(32.W), "hd5a79147".U(32.W), "h06ca6351".U(32.W), "h14292967".U(32.W),
    "h27b70a85".U(32.W), "h2e1b2138".U(32.W), "h4d2c6dfc".U(32.W), "h53380d13".U(32.W), 
    "h650a7354".U(32.W), "h766a0abb".U(32.W), "h81c2c92e".U(32.W), "h92722c85".U(32.W),
    "ha2bfe8a1".U(32.W), "ha81a664b".U(32.W), "hc24b8b70".U(32.W), "hc76c51a3".U(32.W), 
    "hd192e819".U(32.W), "hd6990624".U(32.W), "hf40e3585".U(32.W), "h106aa070".U(32.W),
    "h19a4c116".U(32.W), "h1e376c08".U(32.W), "h2748774c".U(32.W), "h34b0bcb5".U(32.W), 
    "h391c0cb3".U(32.W), "h4ed8aa4a".U(32.W), "h5b9cca4f".U(32.W), "h682e6ff3".U(32.W),
    "h748f82ee".U(32.W), "h78a5636f".U(32.W), "h84c87814".U(32.W), "h8cc70208".U(32.W), 
    "h90befffa".U(32.W), "ha4506ceb".U(32.W), "hbef9a3f7".U(32.W), "hc67178f2".U(32.W)
  ))

    //importing modules
    val preprocessor = Module(new preprocessor(width))
    // val compressor = Module(new compressor())
    // val expander = Module(new expander())


    // Connect the preprocessor module
    preprocessor.io.password := io.password
    io.collected := preprocessor.io.block
    io.colLength := 0.U  // Placeholder
}