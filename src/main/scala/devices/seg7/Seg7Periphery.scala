// See LICENSE.HORIE_Tetsuya for license details.
package sifive.blocks.devices.seg7

import chisel3._
import chisel3.util._
import chisel3.experimental.{withClockAndReset}
import freechips.rocketchip.config.Field
import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp}
import freechips.rocketchip.subsystem.{BaseSubsystem, PeripheryBusKey}

case object PeripherySeg7LEDKey extends Field[Seq[Seg7LEDParams]]

trait HasPeripherySeg7LED { this: BaseSubsystem =>
  val seg7LedParams = p(PeripherySeg7LEDKey)
  val seg7Leds = seg7LedParams.zipWithIndex.map { case(params, i) =>
    val name = Some(s"seg7led_$i")
    val seg7Led = LazyModule(new TLSeg7LED(pbus.beatBytes, params)).suggestName(name)
    pbus.toVariableWidthSlave(name) { seg7Led.node }
    seg7Led
  }
}

trait HasPeripherySeg7LEDBundle {
  val seg7Led: Vec[Seg7LEDPortIO]
}

trait HasPeripherySeg7LEDModuleImp extends LazyModuleImp with HasPeripherySeg7LEDBundle {
  val outer: HasPeripherySeg7LED
  val seg7Led = IO(Vec(outer.seg7LedParams.size, new Seg7LEDPortIO))

  (seg7Led zip outer.seg7Leds).foreach { case (io, device) =>
    io <> device.module.io.port
  }
}
