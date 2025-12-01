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
