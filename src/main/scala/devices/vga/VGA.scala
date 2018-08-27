// See LICENSE.HORIE_Tetsuya for license details.
package sifive.blocks.devices.vga

import chisel3._
import chisel3.util._
import chisel3.core.withClockAndReset
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._

/** Size, location and contents of the VGA. */
case class VGAParams(
  address: BigInt = 0x10080000,
  size: Int = 0x80000)

/** VGAポート */
class VGAPortIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Bool())
  val red   = Output(UInt(4.W))
  val green = Output(UInt(4.W))
  val blue  = Output(UInt(4.W))
  val hSync = Output(Bool())
  val vSync = Output(Bool())
}

object VGA {
  val fps         = 60  // 1秒間に60回画面全体を描画

  val hMax        = 800 // 水平方向のピクセル数(非表示期間も含む)
  val hSyncPeriod = 96  // 水平同期の期間
  val hBackPorch  = 48  // 水平バックポーチ
  val hFrontPorch = 16  // 水平フロントポーチ

  val vMax        = 521 // 垂直方向のライン数(非表示期間も含む)
  val vSyncPeriod = 2   // 垂直同期の期間
  val vBackPorch  = 33  // 垂直バックポーチ
  val vFrontPorch = 10  // 垂直フロントポーチ
  // 仕様上は上記が正しいが、下記にしないとずれるモニタもある。
  // val vMax        = 512 // 垂直方向のライン数(非表示期間も含む)
  // val vSyncPeriod = 2   // 垂直同期の期間
  // val vBackPorch  = 10  // 垂直バックポーチ
  // val vFrontPorch = 20  // 垂直フロントポーチ

  val hDispMax = hMax - (hSyncPeriod + hBackPorch + hFrontPorch)
  val vDispMax = vMax - (vSyncPeriod + vBackPorch + vFrontPorch)

  val pxClock = (100000000.0 / fps / vMax / hMax).round.toInt // 1ピクセル分のクロック数
  val pxMax = hDispMax * vDispMax
}

import VGA._

class TLVGA(val base: BigInt, val size: Int, executable: Boolean = false, beatBytes: Int = 4,
  resources: Seq[Resource] = new SimpleDevice("vga", Seq("horie,vga")).reg("mem"))(implicit p: Parameters) extends LazyModule
{
  val node = TLManagerNode(Seq(TLManagerPortParameters(
    Seq(TLManagerParameters(
      address     = List(AddressSet(base, size-1)),
      resources   = resources,
      regionType  = RegionType.UNCACHEABLE,
      executable  = executable,
      supportsGet        = TransferSizes(1, beatBytes),
      supportsPutPartial = TransferSizes(1, beatBytes),
      supportsPutFull    = TransferSizes(1, beatBytes),
      fifoId      = Some(0))),
    beatBytes = beatBytes)))

  lazy val module = new LazyModuleImp(this) {
    val io = IO(new Bundle{
      val vga = new VGAPortIO
    })

    /*
     * CPU - VRAM(ブロックメモリ)読み書き
     */
    val width = 8 * beatBytes

    val (in, edge) = node.in(0)
    val addrBits = edge.addr_hi(in.a.bits.address - base.asUInt())(log2Ceil(size)-1, 0)
    val vram = Module(new Vram)

    // D stage registers from A
    val d_full      = RegInit(false.B)
    val d_ram_valid = RegInit(false.B) // true if we just read-out from SRAM
    val d_size      = Reg(UInt())
    val d_source    = Reg(UInt())
    val d_read      = Reg(Bool())
    val d_address   = Reg(UInt(addrBits.getWidth.W))
    val d_rmw_mask  = Reg(UInt(beatBytes.W))
    val d_rmw_data  = Reg(UInt(width.W))
    val d_poison    = Reg(Bool())

    // BRAM output
    val d_raw_data      = Wire(Bits(width.W))

    val d_wb = d_rmw_mask.orR
    val d_held_data = RegEnable(d_raw_data, d_ram_valid)

    in.d.bits.opcode  := Mux(d_read, TLMessages.AccessAckData, TLMessages.AccessAck)
    in.d.bits.param   := 0.U
    in.d.bits.size    := d_size
    in.d.bits.source  := d_source
    in.d.bits.sink    := 0.U
    in.d.bits.denied  := false.B
    in.d.bits.data    := Mux(d_ram_valid, d_raw_data, d_held_data)
    in.d.bits.corrupt := false.B

    // Formulate a response only when SRAM output is unused or correct
    in.d.valid := d_full
    in.a.ready := !d_full || (in.d.ready && !d_wb)

    val a_address = addrBits
    val a_read = in.a.bits.opcode === TLMessages.Get
    val a_data = in.a.bits.data
    val a_mask = in.a.bits.mask

    val a_ren = a_read

    when (in.d.fire()) { d_full := false.B }
    d_ram_valid := false.B
    d_rmw_mask  := 0.U
    when (in.a.fire()) {
      d_full      := true.B
      d_ram_valid := a_ren
      d_size      := in.a.bits.size
      d_source    := in.a.bits.source
      d_read      := a_read
      d_address   := a_address
      d_rmw_mask  := 0.U
      d_poison    := in.a.bits.corrupt
      when (!a_read) {
        d_rmw_mask := in.a.bits.mask
        d_rmw_data := in.a.bits.data
      }
    }

    // BRAM arbitration
    val a_fire = in.a.fire()
    val wen =  d_wb || (a_fire && !a_ren)
    val ren = !wen && a_fire

    val addr   = Mux(d_wb, d_address, a_address)
    val dat    = Mux(d_wb, d_rmw_data, a_data)
    val mask   = Mux(d_wb, d_rmw_mask, a_mask)

    vram.io.clka  := clock
    vram.io.ena   := wen | ren
    vram.io.wea   := Mux(wen, mask, 0.U)
    vram.io.addra := addr
    vram.io.dina  := dat
    d_raw_data   := vram.io.douta

    // Tie off unused channels
    in.b.valid := false.B
    in.c.ready := true.B
    in.e.ready := true.B

    /*
     * VRAM -> VGA出力
     */
    withClockAndReset(io.vga.clock, io.vga.reset) {
      val (hCount, hEn)   = Counter(true.B, hMax)
      val (vCount, vEn)   = Counter(hEn, vMax)

      // 表示ピクセルかどうか
      val pxEnable = (hSyncPeriod + hBackPorch).U <= hCount && hCount < (hMax - hFrontPorch).U &&
        (vSyncPeriod + vBackPorch).U <= vCount && vCount < (vMax - vFrontPorch).U

      val (vramAddr, wrap) = Counter(pxEnable, pxMax)
      when (hCount === 0.U && vCount === 0.U) {
        vramAddr := 0.U
      }

      // Bポートから読み出して、VGAポートに出力
      vram.io.clkb := io.vga.clock
      vram.io.enb := pxEnable
      vram.io.web := false.B
      vram.io.addrb := vramAddr
      vram.io.dinb := 0.U
      // VRAMからの出力は1Clock遅れるので、pxEnableをRegNextで受けている
      val pxData = Mux(RegNext(pxEnable, false.B), vram.io.doutb, 0.U)

      io.vga.red   := Cat(pxData(7, 5), pxData(5))
      io.vga.green := Cat(pxData(4, 2), pxData(2))
      io.vga.blue  := Cat(pxData(1, 0), pxData(0), pxData(0))

      // VRAMからの出力の遅れに合わせる
      io.vga.hSync := RegNext(!(hCount < hSyncPeriod.U), true.B)
      io.vga.vSync := RegNext(!(vCount < vSyncPeriod.U), true.B)
    }
  }
}
