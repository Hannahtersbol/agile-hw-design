import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class CyclicPreprocessorTest extends AnyFlatSpec with ChiselScalatestTester {
  
  behavior of "CyclicPreprocessor"
  
  it should "correctly pad and process 'abc' message" in {
    test(new CyclicPreprocessor()) { dut =>
    
      dut.io.enable.poke(true.B)
      dut.io.message_len.poke(3.U)
      dut.io.allow_send.poke(false.B)
      
      dut.io.message_word.poke("h61626300".U)
      dut.clock.step(1)
      
      dut.io.recieved.expect(true.B)
      
	  while(!dut.io.finished.peek().litToBoolean){
		dut.clock.step(1)
	  }
      
      // "abc" + 0x80 + zeros + length(24 bits)
      val expectedBlock = BigInt("6162638000000000000000000000000000000000000000000000000000000000" +
                                  "0000000000000000000000000000000000000000000000000000000000000018", 16)
      dut.io.block.expect(expectedBlock.U)
    }
  }
  
  it should "handle a single word message correctly" in {
    test(new CyclicPreprocessor()) { dut =>
      dut.io.enable.poke(true.B)
      dut.io.message_len.poke(4.U) 
      dut.io.allow_send.poke(false.B)
      
      dut.clock.step(1)

      dut.io.message_word.poke("hDEADBEEF".U)
      dut.clock.step(1)
      
      dut.io.recieved.expect(true.B)
      
      // Send padding and fill
      dut.io.message_word.poke("h80000000".U)
      dut.clock.step(1)
      
	  while(!dut.io.finished.peek().litToBoolean){
		dut.clock.step(1)
	  }
      
      dut.io.finished.expect(true.B)
      dut.io.last_block.expect(true.B)
    }
  }
  
  it should "handle multi-block messages" in {
    test(new CyclicPreprocessor()) { dut =>
      dut.io.enable.poke(true.B)
      dut.io.message_len.poke(64.U)
      dut.io.allow_send.poke(false.B)
      dut.clock.step(1)
      
      for (i <- 0 until 16) {
        dut.io.message_word.poke(i.U)
        dut.clock.step(1)
        if (i < 15) {
          dut.io.recieved.expect(true.B)
        }
      }
      dut.io.finished.expect(false.B)
      dut.clock.step(1)
      // First block should be finished
      dut.io.finished.expect(true.B)
      dut.io.last_block.expect(false.B) // Not the last block yet
      
      dut.io.enable.poke(false.B)
      dut.clock.step(1)
      
      dut.io.enable.poke(true.B)
      dut.clock.step(1)
      dut.io.finished.expect(false.B)
      
	  while(!dut.io.finished.peek().litToBoolean){
		dut.clock.step(1)
	  }
      dut.io.finished.expect(true.B)
      dut.io.last_block.expect(true.B)
	  val expectedBlock = BigInt("8000000000000000000000000000000000000000000000000000000000000000" +
                                  "0000000000000000000000000000000000000000000000000000000000000200", 16)
      dut.io.block.expect(expectedBlock.U)
    }
  }
  
  it should "handle empty message" in {
    test(new CyclicPreprocessor()) { dut =>
      dut.io.enable.poke(true.B)
      dut.io.message_len.poke(0.U)
      dut.io.allow_send.poke(false.B)
      
      dut.clock.step(1)
      
      while(!dut.io.finished.peek().litToBoolean){
		dut.clock.step(1)
	  }
      
      val expectedBlock = BigInt("8000000000000000000000000000000000000000000000000000000000000000" +
                                  "0000000000000000000000000000000000000000000000000000000000000000", 16)
      dut.io.block.expect(expectedBlock.U)
    }
  }
  
  it should "reset properly after processing a message" in {
    test(new CyclicPreprocessor()) { dut =>
      // First message
      dut.io.enable.poke(true.B)
      dut.io.message_len.poke(3.U)
      dut.io.allow_send.poke(false.B)
      
      dut.clock.step(1)
      dut.io.message_word.poke("h61626300".U)
	  while(!dut.io.finished.peek().litToBoolean){
		dut.clock.step(1)
	  }
      
      dut.io.finished.expect(true.B)
      
      dut.io.enable.poke(false.B)
      dut.clock.step(2)
      
      dut.io.enable.poke(true.B)
      dut.io.message_len.poke(4.U)
      
      dut.io.message_word.poke("h01020304".U)
      dut.clock.step(1)
      
      dut.io.recieved.expect(true.B)
    }
  }
}