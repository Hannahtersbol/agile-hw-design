import chisel3._
    import chiseltest._
    import org.scalatest.flatspec.AnyFlatSpec

    class compressorTest extends AnyFlatSpec with ChiselScalatestTester {
      behavior of "Sha chip"

      it should "hash the empty message" in {
        // For the empty message the preprocessor produces a single 512-bit block:
        // W[0] = 0x80000000, W[1..14] = 0x00000000, W[15] = 0x00000000
        // Instantiate with width = 0 so the module treats the message as empty.
        test(new sha256(width = 8)) { c =>
          // Allow modules some cycles to propagate the preprocessed block into the expander
          c.clock.step(5)

          // Check the first 16 words (the initial message block words)
          c.io.w_out(0).expect(BigInt("80000000", 16))
          for (i <- 1 until 16) {
            c.io.w_out(i).expect(0)
          }

          // Optionally print expanded words for inspection
          println("\nExpanded words for empty message (W[0..15]):")
          for (i <- 0 until 16) {
            println(f"W($i) = 0x${c.io.w_out(i).peek().litValue}%08x")
          }
        }
      }
    }
