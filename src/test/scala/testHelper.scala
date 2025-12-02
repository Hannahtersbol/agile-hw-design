import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.security.MessageDigest

object TestHelper {
  // Preprocessor
  // Calculating the expected padded block for a given input password
  def expectedBlock(widthBytes: Int, password: BigInt): BigInt = {
    // message length in bits
    val L = BigInt(widthBytes * 8)
    val K0 = (BigInt(447) - L) % BigInt(512)

    // normalize into [0, 511]
    val K = if (K0 < 0) K0 + 512 else K0

    // <-- BigInt -> Int for shift distance
    val totalShiftInt = (K + 64).toInt

    val messageWith1 = (password << 1) | 1
    val messageShifted = messageWith1 << totalShiftInt
    val out = messageShifted | L
    val mask512 = (BigInt(1) << 512) - 1
    out & mask512
  }

  def expectedBlockCyclic(hex: String): Array[BigInt] = {
    val mask512 = (BigInt(1) << 512) - 1
    val hexClean = if (hex.length % 2 != 0) {
      val (start, end) = hex.splitAt(hex.length - 1)
      start + "0" + end
    } else {
      hex
    }
    val fullWidthBytes = hexClean.length / 2

    val blockCount =
      fullWidthBytes / 64 + (if (fullWidthBytes % 64 <= 55) 1 else 2)
    // val fullHex = hexClean + "80" + "00" * (64-(fullWidthBytes % 64) -1) + f"${(fullWidthBytes * 8)}%016x"
    val fullHex =
      hexClean + "80" + "00" * ((blockCount * 64) - fullWidthBytes - 1 - 8) + f"${(fullWidthBytes * 8)}%016x"
    val fullHexWidthBytes = fullHex.length / 2
    (0 until blockCount).map { blockIdx =>
      val startByte = blockIdx * 64
      val endByte = Math.min(startByte + 64, fullHexWidthBytes)
      val blockHex = fullHex.slice(startByte * 2, endByte * 2)
      // if (blockIdx < blockCount - 1) {
      BigInt(blockHex, 16) & mask512
      // } else {
      // val paddedBlock = (blockHex +"80").padTo(56*2, '0')+ f"${(fullWidthBytes * 8)}%016x"
      // BigInt(paddedBlock, 16)
      // }
    }.toArray
  }

  val mask32 = (BigInt(1) << 32) - 1

  private def rotr32(x: BigInt, n: Int): BigInt = {
    val xr = x & mask32
    ((xr >> n) | (xr << (32 - n))) & mask32
  }

  def sigma0(x: BigInt): BigInt =
    rotr32(x, 7) ^ rotr32(x, 18) ^ (x >> 3)

  def sigma1(x: BigInt): BigInt =
    rotr32(x, 17) ^ rotr32(x, 19) ^ (x >> 10)

  // Expander
  // Calculating the expected W[0..63] for a given padded block
  def expectedW(block: BigInt): Seq[BigInt] = {
    val W = Array.fill[BigInt](64)(BigInt(0))

    // w[0..15]
    for (i <- 0 until 16) {
      W(i) = ((block >> (480 - i * 32)) & mask32)
    }

    // w[16..63]
    for (i <- 16 until 64) {
      val s0 = sigma0(W(i - 15))
      val s1 = sigma1(W(i - 2))
      W(i) = (W(i - 16) + s0 + W(i - 7) + s1) & mask32
    }

    W.toSeq
  }

  // Convert BigInt sequence output words to hex
  def hashToHex(hash: Seq[BigInt]): String =
    hash.map(h => f"$h%08x").mkString("")

  // Software SHA-256 reference
  def goldenSha256(bytes: Array[Byte]): String = {
    val md = MessageDigest.getInstance("SHA-256")
    md.digest(bytes).map(b => f"${b & 0xff}%02x").mkString
  }

  def runAndGetHashHexCyclic(dut: sha256Cyclic, msg: Array[Byte]): String = {
    // val msgBig = BigInt(1, msg)

    dut.io.message_len.poke(msg.length.U)
    dut.io.enable.poke(true.B)

    dut.clock.step()

    // Sends as 4 bytes at a time
    for (i <- 0 until msg.length by 4) {
      var chunk = msg.slice(i, Math.min(i + 4, msg.length))
      while (chunk.length < 4){
        chunk :+= 0.toByte
      }
      println(f"Chunk: ${chunk.map(b => f"${b & 0xff}%02x").mkString("")}")
      val chunkBig = BigInt(1, chunk)
      dut.io.message_word.poke(chunkBig.U)
      dut.clock.step()
    }
    
    // dut.io.message_word.poke(msgBig.U)

    var cycles = 0
    while (!dut.io.finished.peek().litToBoolean && cycles < 5000) {
      dut.clock.step()
      if(dut.debug_compressor){
        print("[")
        for (i<-1 until 7){
          print(dut.io.debug_port.get(i).peek().litValue)
          print(" : ")
        }
        print(dut.io.debug_port.get(7).peek().litValue)
        print("] \n")
      }
      cycles += 1
    }
    dut.io.finished.expect(true.B)

    val hw = (0 until 8).map(i => dut.io.hash_out(i).peek().litValue)
    hashToHex(hw)
  }


}
