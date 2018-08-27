// See LICENSE.HORIE_Tetusya for license details.
package sifive.blocks.devices.vga

import chisel3._
import chisel3.util._
import chisel3.experimental.{withClockAndReset}
import freechips.rocketchip.util.{SynchronizerShiftReg}
import sifive.blocks.devices.pinctrl.{PinCtrl, Pin}

class VGASignals[T <: Data](private val pingen: () => T) extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Bool())
  val red   = Vec(4, pingen())
  val green = Vec(4, pingen())
  val blue  = Vec(4, pingen())
  val hSync = pingen()
  val vSync = pingen()
}

class VGAPins[T <: Pin] (pingen: ()=> T) extends VGASignals(pingen)

object VGAPinsFromPort {
  
  def apply[T <: Pin](pins: VGASignals[T], vga: VGAPortIO, clock: Clock, reset: Bool,
    syncStages: Int = 0, driveStrength: Bool = false.B) {

    vga.clock := pins.clock
    vga.reset := pins.reset

    withClockAndReset(clock, reset) {
      pins.red.zipWithIndex.map { case (p, i) =>
        p.outputPin(vga.red(i), ds = driveStrength)
      }

      pins.green.zipWithIndex.map { case (p, i) =>
        p.outputPin(vga.green(i), ds = driveStrength)
      }

      pins.blue.zipWithIndex.map { case (p, i) =>
        p.outputPin(vga.blue(i), ds = driveStrength)
      }

      pins.hSync.outputPin(vga.hSync, ds = driveStrength)
      pins.vSync.outputPin(vga.vSync, ds = driveStrength)
    }
  }
}
