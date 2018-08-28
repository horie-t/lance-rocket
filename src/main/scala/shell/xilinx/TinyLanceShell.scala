// See LICENSE for license details.
// See LICENSE.HORIE_Tetsuya for license details.
package lancerocket.tinylance

import Chisel._
import chisel3.core.{Input, Output, attach}
import chisel3.experimental.{RawModule, Analog, withClockAndReset}

import freechips.rocketchip.config._
import freechips.rocketchip.devices.debug._

import sifive.blocks.devices.gpio._
import sifive.blocks.devices.pwm._
import sifive.blocks.devices.uart._
import sifive.blocks.devices.pinctrl.{BasePin}

import sifive.fpgashells.ip.xilinx.{IBUFG, IOBUF, PULLUP, mmcm4, reset_sys, PowerOnResetFPGAOnly}

//-------------------------------------------------------------------------
// TinyLanceShell
//-------------------------------------------------------------------------

abstract class TinyLanceShell(implicit val p: Parameters) extends RawModule {

  //-----------------------------------------------------------------------
  // Interface
  //-----------------------------------------------------------------------

  // Clock & Reset
  val CLK100MHZ    = IO(Input(Clock()))
  val ck_rst       = IO(Input(Bool()))

  // Green LEDs
  val led_0        = IO(Analog(1.W))
  val led_1        = IO(Analog(1.W))
  val led_2        = IO(Analog(1.W))
  val led_3        = IO(Analog(1.W))
  val led_4        = IO(Analog(1.W))
  val led_5        = IO(Analog(1.W))
  val led_6        = IO(Analog(1.W))
  val led_7        = IO(Analog(1.W))
  val led_8        = IO(Analog(1.W))
  val led_9        = IO(Analog(1.W))
  val led_10        = IO(Analog(1.W))
  val led_11        = IO(Analog(1.W))
  val led_12        = IO(Analog(1.W))
  val led_13        = IO(Analog(1.W))
  val led_14        = IO(Analog(1.W))
  val led_15        = IO(Analog(1.W))

  // 7Segment LEDs
  val seg7_ca        = IO(Analog(1.W))
  val seg7_cb        = IO(Analog(1.W))
  val seg7_cc        = IO(Analog(1.W))
  val seg7_cd        = IO(Analog(1.W))
  val seg7_ce        = IO(Analog(1.W))
  val seg7_cf        = IO(Analog(1.W))
  val seg7_cg        = IO(Analog(1.W))

  val seg7_dp        = IO(Analog(1.W))

  val seg7_an_0      = IO(Analog(1.W))
  val seg7_an_1      = IO(Analog(1.W))
  val seg7_an_2      = IO(Analog(1.W))
  val seg7_an_3      = IO(Analog(1.W))
  val seg7_an_4      = IO(Analog(1.W))
  val seg7_an_5      = IO(Analog(1.W))
  val seg7_an_6      = IO(Analog(1.W))
  val seg7_an_7      = IO(Analog(1.W))

  // Sliding switches
  val sw_0         = IO(Analog(1.W))
  val sw_1         = IO(Analog(1.W))
  val sw_2         = IO(Analog(1.W))
  val sw_3         = IO(Analog(1.W))

  // Buttons. First 3 used as GPIO, the last is used as wakeup
  val btn_0        = IO(Analog(1.W))
  val btn_1        = IO(Analog(1.W))
  val btn_2        = IO(Analog(1.W))
  val btn_3        = IO(Analog(1.W))

  // UART0
  val uart_rxd_out = IO(Analog(1.W))
  val uart_txd_in  = IO(Analog(1.W))
  val uart_cts     = IO(Analog(1.W))
  val uart_rts     = IO(Analog(1.W))

  //-----------------------------------------------------------------------
  // Wire declrations
  //-----------------------------------------------------------------------

  // Note: these frequencies are approximate.
  val clock_8MHz     = Wire(Clock())
  val clock_32MHz    = Wire(Clock())
  val clock_65MHz    = Wire(Clock())
  val clock_25MHz    = Wire(Clock())

  val mmcm_locked    = Wire(Bool())

  val reset_core     = Wire(Bool())
  val reset_bus      = Wire(Bool())
  val reset_periph   = Wire(Bool())
  val reset_intcon_n = Wire(Bool())
  val reset_periph_n = Wire(Bool())

  //-----------------------------------------------------------------------
  // Clock Generator
  //-----------------------------------------------------------------------
  // Mixed-mode clock generator

  val ip_mmcm = Module(new mmcm4())

  ip_mmcm.io.clk_in1 := CLK100MHZ
  clock_8MHz         := ip_mmcm.io.clk_out1  // 8.388 MHz = 32.768 kHz * 256
  clock_65MHz        := ip_mmcm.io.clk_out2  // 65 Mhz
  clock_32MHz        := ip_mmcm.io.clk_out3  // 65/2 Mhz
  clock_25MHz        := ip_mmcm.io.clk_out4  // 25MHz (VGA Px Clock)
  ip_mmcm.io.resetn  := ck_rst
  mmcm_locked        := ip_mmcm.io.locked

  //-----------------------------------------------------------------------
  // System Reset
  //-----------------------------------------------------------------------
  // processor system reset module

  val ip_reset_sys = Module(new reset_sys())

  ip_reset_sys.io.slowest_sync_clk := clock_8MHz
  ip_reset_sys.io.ext_reset_in     := ck_rst
  ip_reset_sys.io.aux_reset_in     := true.B
  ip_reset_sys.io.mb_debug_sys_rst := true.B
  ip_reset_sys.io.dcm_locked       := mmcm_locked

  reset_core                       := ip_reset_sys.io.mb_reset
  reset_bus                        := ip_reset_sys.io.bus_struct_reset
  reset_periph                     := ip_reset_sys.io.peripheral_reset
  reset_intcon_n                   := ip_reset_sys.io.interconnect_aresetn
  reset_periph_n                   := ip_reset_sys.io.peripheral_aresetn

  //---------------------------------------------------------------------
  // UART
  //---------------------------------------------------------------------

  def connectUART(dut: HasPeripheryUARTModuleImp): Unit = {
    val uartParams = p(PeripheryUARTKey)
    if (!uartParams.isEmpty) {
      IOBUF(uart_rxd_out, dut.uart(0).txd)
      dut.uart(0).rxd := IOBUF(uart_txd_in)
    }
  }

}
