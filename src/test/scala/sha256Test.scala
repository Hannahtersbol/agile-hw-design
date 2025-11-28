import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class sha256Test extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "sha256"

  // Helper function to convert hash output to hex string
  def hashToHex(hash: Seq[BigInt]): String = {
    hash.map(h => f"${h}%08x").mkString("")
  }

  it should "hash an empty message" in {
    test(new sha256(width = 0)) { dut =>
      // Enable the module
      dut.io.enable.poke(true.B)
      dut.io.message_word.poke(0.U)
      dut.io.message_len.poke(0.U)
      dut.io.key.poke(0.U)
      
      // Wait for processing to complete
      var cycles = 0
      while (!dut.io.finished.peek().litToBoolean && cycles < 1000) {
        dut.clock.step()
        cycles += 1
      }
      
      println(s"Completed in $cycles cycles")
      dut.io.finished.expect(true.B)
      
      // Print the hash output
      val hash = (0 until 8).map(i => dut.io.hash_out(i).peek().litValue)
      println(s"Hash: ${hashToHex(hash)}")
    }
  }

  it should "hash a 1-byte message 'A'" in {
    test(new sha256(width = 1)) { dut =>
      val message = BigInt(0x41) // 'A'
      
      dut.io.enable.poke(true.B)
      dut.io.message_word.poke(message.U)
      dut.io.message_len.poke(1.U)
      dut.io.key.poke(0.U)
      
      // Wait for processing
      var cycles = 0
      while (!dut.io.finished.peek().litToBoolean && cycles < 1000) {
        dut.clock.step()
        cycles += 1
      }
      
      println(s"Completed in $cycles cycles")
      dut.io.finished.expect(true.B)
      
      val hash = (0 until 8).map(i => dut.io.hash_out(i).peek().litValue)
      println(s"Hash of 'A': ${hashToHex(hash)}")
    }
  }

  it should "hash an 8-byte message" in {
    test(new sha256(width = 8)) { dut =>
      val message = BigInt("6162636465666768", 16) // "abcdefgh"
      
      dut.io.enable.poke(true.B)
      dut.io.message_word.poke(message.U)
      dut.io.message_len.poke(8.U)
      dut.io.key.poke(0.U)
      
      // Wait for processing
      var cycles = 0
      while (!dut.io.finished.peek().litToBoolean && cycles < 1000) {
        dut.clock.step()
        cycles += 1
      }
      
      println(s"Completed in $cycles cycles")
      dut.io.finished.expect(true.B)
      
      val hash = (0 until 8).map(i => dut.io.hash_out(i).peek().litValue)
      println(s"Hash of 'abcdefgh': ${hashToHex(hash)}")
    }
  }

  it should "hash 'hello there' (11 bytes)" in {
    test(new sha256(width = 11)) { dut =>
      val message = BigInt("68656c6c6f207468657265", 16) // "hello there"
      
      dut.io.enable.poke(true.B)
      dut.io.message_word.poke(message.U)
      dut.io.message_len.poke(11.U)
      dut.io.key.poke(0.U)
      
      // Monitor state transitions
      var cycles = 0
      var prevReceived = false
      while (!dut.io.finished.peek().litToBoolean && cycles < 1000) {
        val received = dut.io.recieved.peek().litToBoolean
        if (received && !prevReceived) {
          println(s"  Preprocessing complete at cycle $cycles")
        }
        prevReceived = received
        
        dut.clock.step()
        cycles += 1
      }
      
      println(s"Completed in $cycles cycles")
      dut.io.finished.expect(true.B)
      
      val hash = (0 until 8).map(i => dut.io.hash_out(i).peek().litValue)
      println(s"Hash of 'hello there': ${hashToHex(hash)}")
    }
  }

  it should "hash 'abc' (3 bytes) - known test vector" in {
    test(new sha256(width = 3)) { dut =>
      // "abc" -> SHA-256: ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
      val message = BigInt("616263", 16) // "abc"
      
      dut.io.enable.poke(true.B)
      dut.io.message_word.poke(message.U)
      dut.io.message_len.poke(3.U)
      dut.io.key.poke(0.U)
      
      var cycles = 0
      while (!dut.io.finished.peek().litToBoolean && cycles < 1000) {
        dut.clock.step()
        cycles += 1
      }
      
      println(s"Completed in $cycles cycles")
      dut.io.finished.expect(true.B)
      
      val hash = (0 until 8).map(i => dut.io.hash_out(i).peek().litValue)
      val hashHex = hashToHex(hash)
      println(s"Hash of 'abc': $hashHex")
      println(s"Expected:      ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
    }
  }
}