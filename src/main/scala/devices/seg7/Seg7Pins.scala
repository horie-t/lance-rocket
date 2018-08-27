// See LICENSE.HORIE_Tetusya for license details.
package sifive.blocks.devices.seg7

import chisel3._
import chisel3.util._
import chisel3.experimental.{withClockAndReset}
import freechips.rocketchip.util.{SynchronizerShiftReg}
import sifive.blocks.devices.pinctrl.{PinCtrl, Pin}

class Seg7LEDSignals[T <: Data](private val pingen: () => T) extends Bundle {
  val cathodes = Vec(7, pingen())
  val decimalPoint  = pingen()
  val anodes  = Vec(8,  pingen())
}

class Seg7LEDPins[T <: Pin] (pingen: ()=> T) extends Seg7LEDSignals(pingen)

object Seg7LEDPinsFromPort {
  
  def apply[T <: Pin](pins: Seg7LEDSignals[T], seg7led: Seg7LEDPortIO, clock: Clock, reset: Bool,
    syncStages: Int = 0, driveStrength: Bool = false.B) {

    withClockAndReset(clock, reset) {
      pins.cathodes.zipWithIndex.map { case (p, i) =>
        p.outputPin(seg7led.cathodes(i), ds = driveStrength)
      }

      pins.decimalPoint.outputPin(seg7led.decimalPoint, ds = driveStrength)

      pins.anodes.zipWithIndex.map { case (p, i) =>
        p.outputPin(seg7led.anodes(i), ds = driveStrength)
      }
    }
  }
}
