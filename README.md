#SHA-256 Hasher in Chisel
## Group Members:
- Hannah Christophersen Tersbøl (s224775)
- Christoffer Dam-Hansen (s224789)
- Frederik Hvarregaard (s224801)

## Introduction 
In this project, we built a SHA‑256 hasher in Chisel. Our goal was to follow the official SHA‑256 standard and make a clear, modular design that is easy to understand and test. The hasher takes a message, pads it according to the rules, splits it into 512‑bit blocks, and runs 64 rounds per block to produce a 256‑bit hash. 

We verified the design with unit tests for each module and compared the final hash against a software golden model (Java’s MessageDigest) to check correctness.

## Structure

- Preprocessor: Pads the input message and turns it into one or more 512‑bit blocks.
- Expander: Builds the 64 words (w[0..63]) needed for each block.
- Compressor: Runs the 64 SHA‑256 rounds and keeps the running hash across blocks.
- SHA‑256 top: Connects all parts, latching blocks from the Preprocessor, driving the Expander/Compressor, and resetting the hash state only at the start of a new message

## Build and Test

## Components 

### Preprocessor 
### Compressor
### Expander 
### SHA-256 Module


## Tests
### Preprocessor 
### Compressor
### Expander 
### SHA-256 Module
### Helper Functions

## Minimal Viable Product
## Optimizations