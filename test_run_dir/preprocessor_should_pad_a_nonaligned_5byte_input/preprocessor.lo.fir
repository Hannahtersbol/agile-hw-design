FIRRTL version 1.2.0
circuit preprocessor :
  module preprocessor : @[src/main/scala/preprocessor.scala 4:7]
    input clock : Clock @[src/main/scala/preprocessor.scala 4:7]
    input reset : UInt<1> @[src/main/scala/preprocessor.scala 4:7]
    input io_password : UInt<40> @[src/main/scala/preprocessor.scala 5:14]
    output io_block : UInt<512> @[src/main/scala/preprocessor.scala 5:14]

    reg passwordReg : UInt<40>, clock with :
      reset => (UInt<1>("h0"), passwordReg) @[src/main/scala/preprocessor.scala 11:28]
    reg inputReceived : UInt<1>, clock with :
      reset => (UInt<1>("h0"), inputReceived) @[src/main/scala/preprocessor.scala 12:30]
    reg paddedMessage : UInt<512>, clock with :
      reset => (UInt<1>("h0"), paddedMessage) @[src/main/scala/preprocessor.scala 13:30]
    node _T = eq(inputReceived, UInt<1>("h0")) @[src/main/scala/preprocessor.scala 16:9]
    node _passwordReg_T = bits(io_password, 39, 0) @[src/main/scala/preprocessor.scala 17:31]
    node _GEN_0 = mux(_T, _passwordReg_T, passwordReg) @[src/main/scala/preprocessor.scala 16:25 17:17 11:28]
    node _GEN_1 = mux(_T, UInt<1>("h1"), inputReceived) @[src/main/scala/preprocessor.scala 16:25 18:19 12:30]
    node _K_T = sub(UInt<9>("h1bf"), UInt<6>("h28")) @[src/main/scala/preprocessor.scala 25:18]
    node _K_T_1 = tail(_K_T, 1) @[src/main/scala/preprocessor.scala 25:18]
    node K = rem(_K_T_1, UInt<10>("h200")) @[src/main/scala/preprocessor.scala 25:23]
    node _totalShift_T = add(K, UInt<7>("h40")) @[src/main/scala/preprocessor.scala 36:24]
    node totalShift = tail(_totalShift_T, 1) @[src/main/scala/preprocessor.scala 36:24]
    node messageWith1 = cat(passwordReg, UInt<1>("h1")) @[src/main/scala/preprocessor.scala 39:27]
    node messageShifted = dshl(messageWith1, totalShift) @[src/main/scala/preprocessor.scala 40:39]
    node _paddedMessage_T = or(messageShifted, UInt<6>("h28")) @[src/main/scala/preprocessor.scala 43:37]
    node _GEN_2 = mux(inputReceived, _paddedMessage_T, paddedMessage) @[src/main/scala/preprocessor.scala 29:24 43:19 13:30]
    io_block <= paddedMessage @[src/main/scala/preprocessor.scala 46:14]
    passwordReg <= mux(reset, UInt<40>("h0"), _GEN_0) @[src/main/scala/preprocessor.scala 11:{28,28}]
    inputReceived <= mux(reset, UInt<1>("h0"), _GEN_1) @[src/main/scala/preprocessor.scala 12:{30,30}]
    paddedMessage <= bits(mux(reset, UInt<512>("h0"), _GEN_2), 511, 0) @[src/main/scala/preprocessor.scala 13:{30,30}]
