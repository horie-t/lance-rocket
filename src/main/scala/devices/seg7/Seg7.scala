// See LICENSE.HORIE_Tetsuya for license details.
package sifive.blocks.devices.seg7

import chisel3._
import chisel3.util._
import chisel3.experimental.MultiIOModule
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._

import sifive.blocks.util.{NonBlockingEnqueue, NonBlockingDequeue}

case class Seg7LEDParams(
  address: BigInt)

trait HasSeg7LEDParameters {
  def c: Seg7LEDParams
}

abstract class Seg7LEDModule(val c: Seg7LEDParams)(implicit val p: Parameters)
    extends Module with HasSeg7LEDParameters

class Seg7LEDPortIO extends Bundle {
  /** 各セグメントの点灯用。0〜7をCAからCGに対応させる事。0の時に点灯、1の時に消灯します。 */
  val cathodes     = Output(UInt(7.W))
  /** 小数点用。0の時に点灯、1の時に消灯。 */
  val decimalPoint = Output(Bool())
  /** 桁の選択用。0の桁が点灯、１の桁が消灯。 */
  val anodes       = Output(UInt(8.W))
}

trait HasSeg7LEDTopBundleContents extends Bundle {
  val port = new Seg7LEDPortIO
}

class Seg7LED(c: Seg7LEDParams)(implicit p: Parameters) extends Seg7LEDModule(c)(p) {
  val io = IO(new Bundle {
    val digits = Input(Vec(8, UInt(4.W))) // 8桁分の4ビットの数値をVecで確保する
    val blink = Input(Bool())             // 点滅表示するかどうか
    val seg7led = new Seg7LEDPortIO
  })

  val clk_hz = 32500000

  /* 各桁を切り替える時間のカウンタ
   * Counterは、引数にカウントアップする条件(cond)、カウントする数(n, 0〜n-1までカウントする)をとり、
   * 現在のカウント数の値の信号、n-1にカウントアップした時ににtrue.Bになる信号のタプルを返します。 
   * カウントアップ条件にtrue.Bを渡すと、毎クロックカウントアップします。 */
  val (digitChangeCount, digitChange) = Counter(true.B, clk_hz / 1000)

  val (digitIndex, digitWrap) = Counter(digitChange, 8) // 何桁目を表示するか
  val digitNum = io.digits(digitIndex)        // 表示桁の数値

  io.seg7led.cathodes := MuxCase("b111_1111".U,
    Array(                   // gfe_dcba の順序にcathodeが並ぶ
      (digitNum === "h0".U) -> "b100_0000".U,
      (digitNum === "h1".U) -> "b111_1001".U,
      (digitNum === "h2".U) -> "b010_0100".U,
      (digitNum === "h3".U) -> "b011_0000".U,
      (digitNum === "h4".U) -> "b001_1001".U,
      (digitNum === "h5".U) -> "b001_0010".U,
      (digitNum === "h6".U) -> "b000_0010".U,
      (digitNum === "h7".U) -> "b101_1000".U,
      (digitNum === "h8".U) -> "b000_0000".U,
      (digitNum === "h9".U) -> "b001_0000".U,
      (digitNum === "ha".U) -> "b000_1000".U,
      (digitNum === "hb".U) -> "b000_0011".U,
      (digitNum === "hc".U) -> "b100_0110".U,
      (digitNum === "hd".U) -> "b010_0001".U,
      (digitNum === "he".U) -> "b000_0110".U,
      (digitNum === "hf".U) -> "b000_1110".U))

  val anodes = RegInit("b1111_1110".U(8.W))
  when (digitChange) {
    // 表示桁の切り替えタイミングで、ローテートシフト
    anodes := Cat(anodes(6, 0), anodes(7))
  }

  val (blinkCount, blinkToggle) = Counter(io.blink, clk_hz)
  val blinkLight = RegInit(true.B) // 点滅表示時に点灯するかどうか
  when (blinkToggle) {
    blinkLight := !blinkLight
  }
  io.seg7led.anodes := Mux(!io.blink || blinkLight, anodes, "hff".U)

  io.seg7led.decimalPoint := 1.U         // 小数点は点灯させない。
}

trait HasSeg7LEDTopModuleContents extends MultiIOModule with HasSeg7LEDParameters with HasRegMap {
  val io: HasSeg7LEDTopBundleContents
  implicit val p: Parameters
  def params: Seg7LEDParams
  def c = params

  val seg7led = Module(new Seg7LED(params))

  val disp_val = RegInit(0.U(32.W))
  val blink = RegInit(false.B)
  seg7led.io.digits := VecInit(Seq(disp_val(3, 0), disp_val(7, 4), disp_val(11, 8), disp_val(15, 12),
    disp_val(19, 16), disp_val(23, 20), disp_val(27, 24), disp_val(31, 28)))
  seg7led.io.blink := blink
  io.port := seg7led.io.seg7led

  regmap(
    Seg7LEDCtrlRegs.value -> Seq(RegField(32, disp_val,
               RegFieldDesc("value","value for display", reset=Some(0)))),
    Seg7LEDCtrlRegs.blink -> Seq(RegField(1, blink,
               RegFieldDesc("blink","blink led", reset=Some(0)))))
}

// Magic TL2 Incantation to create a TL2 Seg7LED
class TLSeg7LED(w: Int, c: Seg7LEDParams)(implicit p: Parameters)
  extends TLRegisterRouter(c.address, "seg7led", Seq("horie,seg7led"), interrupts = 0, beatBytes = w)(
  new TLRegBundle(c, _)    with HasSeg7LEDTopBundleContents)(
  new TLRegModule(c, _, _) with HasSeg7LEDTopModuleContents)
