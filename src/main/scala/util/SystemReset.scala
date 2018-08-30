// See LICENSE.HORIE_Tetsuya

package sifive.blocks.util

import chisel3._
import chisel3.core._

class SystemReset(clockNum: Int) extends Module {
  val io = new Bundle {
    val aReset = Input(Bool())
    val clocks = Input(Vec(clockNum, Clock()))
    val resets = Output(Vec(clockNum, Clock()))
  }

  withClock {
    val holdClock = Module(new ResetHold)
    holdClock.io.aReset := aReset
    holdClock.io.clock := clocks(0)
    resets(0) := holdClock.io.reset
  }

  val synchronizers = Seq.fill(clockNum - 1){Module(new ResetSync)}
  for (i <- 0 until clockNum - 1) {
    val synchronizer =  synchronizers(i)
    synchronizer.io.aReset := resets(i)
    synchronizer.io.clock := clocks(i + 1)
    resets(i + 1) := synchronizer.io.reset
  }
}

/** クロックがロックされ、電源が安定するまで、リセット維持します。
  * 
  */
class ResetSync(clocksForSync: Int = 4) extends BlackBox {
  val io = new Bundle {
    val aReset = Input(Bool())
    val clock = Input(Clock())
    val reset = Output(Bool())
  }

  setInline("ResetSync.v",
    s"""
     |module ResetSync(
     |   input wire aReset,
     |   input wire clock,
     |   output wire reset);
     |
     |   reg [${clocksForSync} - 1:0] gen_reset = {`${clocksForSync}{1'b1}};
     |   always @(posedge clock, posedge aReset) begin
     |      if (aReset) begin
     |	   gen_reset <= {`${clocksForSync}{1'b1}};
     |      end else begin
     |	   gen_reset <= {1'b0, gen_reset[`${clocksForSync} - 1:1]};
     |      end
     |   end
     |
     |   assign reset = gen_reset[0];
     |endmodule // ResetSync
    """.stripMargin)
}

/** Resetを保持し続けます。
  * 
  */
class ResetHold(clocksForSync: Int = 4, debounceBits: Int = 8) extends Module {
  val io = new Bundle {
    aReset = Input(Bool())
    reset = Output(Bool())
  }

  val rawReset = Wire(Bool())
  val capture = Module(new ResetSync)
  capture.io.aReset := aReset
  capture.io.clock := clock
  rawReset := capture.io.reset

  val syncReset = RegNext(Cat(rawReset, syncReset(clocksForSync - 1, 1)), ((1 << clocksForSync) - 1).asUInt)
  val debounceReset = RegInit(((1 << debounceBits + 1) - 1).asUInt)

  val outReset = debounceReset(debounceBits)

  when (syncReset(0) === 1.U) {
    debounceReset := ((1 << debounceBits + 1) - 1).asUInt
  } .elsewise {
    debounceReset := debounceReset - outReset
  }

  reset := outReset
}
