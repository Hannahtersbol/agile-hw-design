# SHA-256 Hasher in Chisel
## Group Members:
- Hannah Christophersen Tersbøl (s224775)
- Christoffer Dam-Hansen (s224789)
- Frederik Hvarregaard (s224801)

## Introduction 
In this project, we built a SHA‑256 hasher in Chisel. Our goal was to follow the official SHA‑256 standard and make a clear, modular design that is easy to understand and test. The hasher takes a message, pads it according to the rules, splits it into 512‑bit blocks, and runs 64 rounds per block to produce a 256‑bit hash. 

We verified the design with unit tests for each module and compared the final hash against a software golden model (Java’s MessageDigest) to check correctness.

## Structure

- **Preprocessor**: Pads the input message and turns it into one or more 512‑bit blocks.
- **Expander**: Builds the 64 words (w[0..63]) needed for each block.
- **Compressor**: Runs the 64 SHA‑256 rounds and keeps the running hash across blocks.
- **SHA‑256 top**: Connects all parts, latching blocks from the Preprocessor, driving the Expander/Compressor, and resetting the hash state only at the start of a new message

## Build and Test

**Requirements** 
- Java(JDK): version 17
- sbt: version 1.9.x (project uses 1.9.6)
- Scala: 2.13
- Chisel: 6.7.0

**Testing** 
To run all tests use `sbt test`.

For testing a single component:
- Preprocessor: `sbt "testOnly preprocessorTest`
- Expander: `sbt "testOnly expanderTest`
- Compressor: `sbt "testOnly compressorTest`
- Sha256: `sbt "testOnly sha256Test`

## Components 
The initial design of the SHA256 algorithm was based on the pseudo code from the Wikipedia site on [SHA-2](https://en.wikipedia.org/wiki/SHA-2). This was a good starting point to get the basic structure and values for the project.

### Preprocessor 
### Expander 
### Compressor
### SHA-256 Module


## Tests
### Preprocessor 
### Compressor
### Expander 
### SHA-256 Module
### Helper Functions

## Minimal Viable Product
Our minimal viable product is the sha256 hasher with an input size of 512 bits. 

## Optimizations
- [x] Preprocessor taking multiple block messages
- [x] Compressor generator (more calculations per cycle)
- [] Parrallel states in sha-file



stateDiagram-v2
    [*] --> Idle

    Idle --> Preprocessing: io.enable == 1\n(en_pre := 1,\nreset_hash_pulse := 1)

    Preprocessing --> Expanding: preprocessor.io.recieved\n(blockReg := block,\nlastBlockReg := last_block,\nen_pre := 0,\nen_exp := 1)

    Expanding --> Compressing: expander.io.finished\n(en_exp := 0,\nen_comp := 1)

    Compressing --> Finished: compressor.io.finished && lastBlockReg\n(en_comp := 0,\nlastBlockReg := 0)

    Compressing --> Preprocessing: compressor.io.finished && !lastBlockReg\n(en_comp := 0,\nen_pre := 1,\nreset_hash_pulse := 0)

    Finished --> Idle: io.enable == 0

    state Idle: 
      - en_pre = 0\n- en_exp = 0\n- en_comp = 0

    state Preprocessing:
      - preprocessor.io.allow_send = 1\n- reset_hash_pulse = 0

    state Expanding:
      - en_exp = 1

    state Compressing:
      - en_comp = 1

    state Finished:
      - io.finished = 1
