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
			dut.io.enable.poke(true.B)
			dut.io.message_word.poke(0.U)
			dut.io.message_len.poke(0.U)
			dut.io.key.poke(0.U)
			
			var cycles = 0
			while (!dut.io.finished.peek().litToBoolean && cycles < 1000) {
				dut.clock.step()
				cycles += 1
			}
			
			println(s"Completed in $cycles cycles")
			dut.io.finished.expect(true.B)
			
			val hash = (0 until 8).map(i => dut.io.hash_out(i).peek().litValue)
			println(s"Hash: ${hashToHex(hash)}")
		}
	}

	it should "hash a 1-byte message 'A'" in {
		test(new sha256(width = 1)) { dut =>
			val message = BigInt(0x41)
			
			dut.io.enable.poke(true.B)
			dut.io.message_word.poke(message.U)
			dut.io.message_len.poke(1.U)
			dut.io.key.poke(0.U)
			
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
			val message = BigInt("6162636465666768", 16)
			
			dut.io.enable.poke(true.B)
			dut.io.message_word.poke(message.U)
			dut.io.message_len.poke(8.U)
			dut.io.key.poke(0.U)
			
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
			val message = BigInt("68656c6c6f207468657265", 16)
			
			dut.io.enable.poke(true.B)
			dut.io.message_word.poke(message.U)
			dut.io.message_len.poke(11.U)
			dut.io.key.poke(0.U)
			
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
			val message = BigInt("616263", 16)
			
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

	it should "hash a 64-byte message (exactly one block)" in {
		test(new sha256(width = 64)) { dut =>
			val message = BigInt("0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f", 16)
			
			dut.io.enable.poke(true.B)
			dut.io.message_word.poke(message.U)
			dut.io.message_len.poke(64.U)
			dut.io.key.poke(0.U)
			
			var cycles = 0
			while (!dut.io.finished.peek().litToBoolean && cycles < 2000) {
				dut.clock.step()
				cycles += 1
			}
			
			println(s"64-byte message completed in $cycles cycles")
			dut.io.finished.expect(true.B)
			
			val hash = (0 until 8).map(i => dut.io.hash_out(i).peek().litValue)
			println(s"Hash: ${hashToHex(hash)}")
		}
	}

	it should "hash a 65-byte message (requires two blocks)" in {
		test(new sha256(width = 65)) { dut =>
			val messageBytes = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f40"
			val message = BigInt(messageBytes, 16)
			
			dut.io.enable.poke(true.B)
			dut.io.message_word.poke(message.U)
			dut.io.message_len.poke(65.U)
			dut.io.key.poke(0.U)
			
			var cycles = 0
			while (!dut.io.finished.peek().litToBoolean && cycles < 2000) {
				dut.clock.step()
				cycles += 1
			}
			
			println(s"65-byte message completed in $cycles cycles")
			dut.io.finished.expect(true.B)
			
			val hash = (0 until 8).map(i => dut.io.hash_out(i).peek().litValue)
			println(s"Hash: ${hashToHex(hash)}")
		}
	}

	it should "hash a 128-byte message (exactly two blocks)" in {
		test(new sha256(width = 128)) { dut =>
			val messageBytes = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f" +
												 "4041424344454647484a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f"
			val message = BigInt(messageBytes, 16)
			
			dut.io.enable.poke(true.B)
			dut.io.message_word.poke(message.U)
			dut.io.message_len.poke(128.U)
			dut.io.key.poke(0.U)
			
			var cycles = 0
			while (!dut.io.finished.peek().litToBoolean && cycles < 3000) {
				dut.clock.step()
				cycles += 1
			}
			
			println(s"128-byte message completed in $cycles cycles")
			dut.io.finished.expect(true.B)
			
			val hash = (0 until 8).map(i => dut.io.hash_out(i).peek().litValue)
			println(s"Hash: ${hashToHex(hash)}")
		}
	}

	it should "hash a 100-byte message (requires two blocks)" in {
		test(new sha256(width = 100)) { dut =>
			val message = BigInt("00" * 100, 16)
			
			dut.io.enable.poke(true.B)
			dut.io.message_word.poke(message.U)
			dut.io.message_len.poke(100.U)
			dut.io.key.poke(0.U)
			
			var cycles = 0
			while (!dut.io.finished.peek().litToBoolean && cycles < 3000) {
				dut.clock.step()
				cycles += 1
			}
			
			println(s"100-byte message completed in $cycles cycles")
			dut.io.finished.expect(true.B)
			
			val hash = (0 until 8).map(i => dut.io.hash_out(i).peek().litValue)
			println(s"Hash: ${hashToHex(hash)}")
		}
	}
}