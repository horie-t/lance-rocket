// See LICENSE.SiFive for license details.
// See LICENSE.HORIE_Tetsuya for license details.
package lancerocket.tinylance

import Chisel._

import freechips.rocketchip.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util.ResetCatchAndSync
import freechips.rocketchip.system._

import sifive.blocks.devices.mockaon._
import sifive.blocks.devices.gpio._
import sifive.blocks.devices.jtag._
import sifive.blocks.devices.pwm._
import sifive.blocks.devices.uart._
import sifive.blocks.devices.i2c._
import sifive.blocks.devices.seg7._
import sifive.blocks.devices.pinctrl._

//-------------------------------------------------------------------------
// PinGen
//-------------------------------------------------------------------------

object PinGen {
  def apply(): BasePin =  {
    val pin = new BasePin()
    pin
  }
}

//-------------------------------------------------------------------------
// TinyLancePlatformIO
//-------------------------------------------------------------------------

class TinyLancePlatformIO(implicit val p: Parameters) extends Bundle {
  val pins = new Bundle {
    val jtag = new JTAGPins(() => PinGen(), false)
    val gpio = new GPIOPins(() => PinGen(), p(PeripheryGPIOKey)(0))
    val seg7 = new Seg7LEDPins(() => PinGen())
    val aon  = new MockAONWrapperPins()
  }
  val jtag_reset = Bool(INPUT)
  val ndreset    = Bool(OUTPUT)
}

//-------------------------------------------------------------------------
// TinyLancePlatform
//-------------------------------------------------------------------------

class TinyLancePlatform(implicit val p: Parameters) extends Module {
  val sys = Module(LazyModule(new TinyLanceSystem).module)
  val io = new TinyLancePlatformIO

  // This needs to be de-asserted synchronously to the coreClk.
  val async_corerst = sys.aon.rsts.corerst
  // Add in debug-controlled reset.
  sys.reset := ResetCatchAndSync(clock, async_corerst, 20)

  //-----------------------------------------------------------------------
  // Check for unsupported rocket-chip connections
  //-----------------------------------------------------------------------

  require (p(NExtTopInterrupts) == 0, "No Top-level interrupts supported");

  //-----------------------------------------------------------------------
  // Build GPIO Pin Mux
  //-----------------------------------------------------------------------
  // Pin Mux for UART, SPI, PWM
  // First convert the System outputs into "IOF" using the respective *GPIOPort
  // converters.

  val sys_uart = sys.uart
  val sys_pwm  = sys.pwm
  val sys_i2c  = sys.i2c

  val uart_pins = sys.outer.uartParams.map { c => Wire(new UARTPins(() => PinGen()))}
  val pwm_pins  = sys.outer.pwmParams.map  { c => Wire(new PWMPins(() => PinGen(), c))}
  val i2c_pins  = sys.outer.i2cParams.map  { c => Wire(new I2CPins(() => PinGen()))}

  (uart_pins zip  sys_uart) map {case (p, r) => UARTPinsFromPort(p, r, clock = clock, reset = reset, syncStages = 0)}
  (pwm_pins  zip  sys_pwm)  map {case (p, r) => PWMPinsFromPort(p, r) }
  (i2c_pins  zip  sys_i2c)  map {case (p, r) => I2CPinsFromPort(p, r, clock = clock, reset = reset, syncStages = 0)}

  //-----------------------------------------------------------------------
  // Default Pin connections before attaching pinmux

  for (iof_0 <- sys.gpio(0).iof_0.get) {
    iof_0.default()
  }

  for (iof_1 <- sys.gpio(0).iof_1.get) {
    iof_1.default()
  }

  //-----------------------------------------------------------------------
  
  val iof_0 = sys.gpio(0).iof_0.get
  val iof_1 = sys.gpio(0).iof_1.get

  // I2C
  if (sys.outer.i2cParams.length == 1) {
    BasePinToIOF(i2c_pins(0).sda, iof_0(12))
    BasePinToIOF(i2c_pins(0).scl, iof_0(13))
  }

  // UART0
  BasePinToIOF(uart_pins(0).rxd, iof_0(16))
  BasePinToIOF(uart_pins(0).txd, iof_0(17))

  // UART1
  BasePinToIOF(uart_pins(1).rxd, iof_0(24))
  BasePinToIOF(uart_pins(1).txd, iof_0(25))

  //PWM
  BasePinToIOF(pwm_pins(0).pwm(0), iof_1(0) )
  BasePinToIOF(pwm_pins(0).pwm(1), iof_1(1) )
  BasePinToIOF(pwm_pins(0).pwm(2), iof_1(2) )
  BasePinToIOF(pwm_pins(0).pwm(3), iof_1(3) )

  BasePinToIOF(pwm_pins(1).pwm(1), iof_1(19))
  BasePinToIOF(pwm_pins(1).pwm(0), iof_1(20))
  BasePinToIOF(pwm_pins(1).pwm(2), iof_1(21))
  BasePinToIOF(pwm_pins(1).pwm(3), iof_1(22))

  BasePinToIOF(pwm_pins(2).pwm(0), iof_1(10))
  BasePinToIOF(pwm_pins(2).pwm(1), iof_1(11))
  BasePinToIOF(pwm_pins(2).pwm(2), iof_1(12))
  BasePinToIOF(pwm_pins(2).pwm(3), iof_1(13))

  //-----------------------------------------------------------------------
  // Drive actual Pads
  //-----------------------------------------------------------------------

  // Result of Pin Mux
  GPIOPinsFromPort(io.pins.gpio, sys.gpio(0))

  // Dedicated Seg 7 LED Pads
  Seg7LEDPinsFromPort(io.pins.seg7, sys.seg7Led(0), clock = sys.clock, reset = sys.reset, syncStages = 0)

  // JTAG Debug Interface
  val sjtag = sys.debug.systemjtag.get
  JTAGPinsFromPort(io.pins.jtag, sjtag.jtag)
  sjtag.reset := io.jtag_reset
  sjtag.mfr_id := p(JtagDTMKey).idcodeManufId.U(11.W)

  io.ndreset := sys.debug.ndreset

  // AON Pads -- direct connection is OK because
  // EnhancedPin is hard-coded in MockAONPads
  // and thus there is no .fromPort method.
  io.pins.aon <> sys.aon.pins
}
