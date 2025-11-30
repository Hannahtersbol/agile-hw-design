import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.security.MessageDigest

class sha256Test extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "sha256"

  // Convert DUT output words to hex
  def hashToHex(hash: Seq[BigInt]): String =
    hash.map(h => f"$h%08x").mkString("")

  // Software SHA-256 reference
  def goldenSha256(bytes: Array[Byte]): String = {
    val md = MessageDigest.getInstance("SHA-256")
    md.digest(bytes).map(b => f"${b & 0xff}%02x").mkString
  }

  // Helper: run DUT until finished and return hash hex
  def runAndGetHashHex(dut: sha256, msg: Array[Byte]): String = {
    val msgBig = BigInt(1, msg)

    dut.io.enable.poke(true.B)
    dut.io.message_word.poke(msgBig.U)
    dut.io.message_len.poke(msg.length.U)

    var cycles = 0
    while (!dut.io.finished.peek().litToBoolean && cycles < 5000) {
      dut.clock.step()
      cycles += 1
    }
    dut.io.finished.expect(true.B)

    val hw = (0 until 8).map(i => dut.io.hash_out(i).peek().litValue)
    hashToHex(hw)
  }

  it should "hash empty message correctly" in {
    test(new sha256(width = 0)) { dut =>
      val msg = Array.emptyByteArray
      val hw = runAndGetHashHex(dut, msg)
      val sw = goldenSha256(msg)
      assert(hw == sw, s"Hardware: $hw, Software: $sw")
    }
  }

  it should "hash 'A' correctly" in {
    test(new sha256(width = 1)) { dut =>
      val msg = Array('A'.toByte)
      val hw = runAndGetHashHex(dut, msg)
      val sw = goldenSha256(msg)
      assert(hw == sw)
    }
  }

  it should "hash 'abc' correctly" in {
    test(new sha256(width = 3)) { dut =>
      val msg = "abc".getBytes("UTF-8")
      val hw = runAndGetHashHex(dut, msg)
      val sw = goldenSha256(msg)
      assert(hw == sw)
    }
  }

  it should "hash 8-byte message correctly" in {
    test(new sha256(width = 8)) { dut =>
      val msg = "abcdefgh".getBytes("UTF-8")
      val hw = runAndGetHashHex(dut, msg)
      val sw = goldenSha256(msg)
      assert(hw == sw)
    }
  }

  it should "hash 'hello there' correctly" in {
    test(new sha256(width = 11)) { dut =>
      val msg = "hello there".getBytes("UTF-8")
      val hw = runAndGetHashHex(dut, msg)
      val sw = goldenSha256(msg)
      assert(hw == sw)
    }
  }

  it should "hash 64-byte message correctly" in {
    test(new sha256(width = 64)) { dut =>
      val msg = (0 until 64).map(_.toByte).toArray
      val hw = runAndGetHashHex(dut, msg)
      val sw = goldenSha256(msg)
      assert(hw == sw)
    }
  }

  it should "hash 65-byte message correctly" in {
    test(new sha256(width = 65)) { dut =>
      val msg = (0 until 65).map(_.toByte).toArray
      val hw = runAndGetHashHex(dut, msg)
      val sw = goldenSha256(msg)
      assert(hw == sw)
    }
  }

  it should "hash 100-byte message correctly" in {
    test(new sha256(width = 100)) { dut =>
      val msg = (0 until 100).map(_.toByte).toArray
      val hw = runAndGetHashHex(dut, msg)
      val sw = goldenSha256(msg)
      assert(hw == sw)
    }
  }
}
