// See LICENSE for license details.
package sifive.fpgashells.devices.xilinx.xilinxnexys4ddrmig

import Chisel._
import freechips.rocketchip.config._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp, AddressRange}

case object MemoryXilinxDDRKey extends Field[XilinxNexys4DDRMIGParams]

trait HasMemoryXilinxNexys4DDRMIG { this: BaseSubsystem =>
  val module: HasMemoryXilinxNexys4DDRMIGModuleImp

  val xilinxnexys4ddrmig = LazyModule(new XilinxNexys4DDRMIG(p(MemoryXilinxDDRKey)))

  require(nMemoryChannels == 1, "Core complex must have 1 master memory port")
  xilinxnexys4ddrmig.node := memBuses.head.toDRAMController(Some("xilinxnexys4ddrmig"))()
}

trait HasMemoryXilinxNexys4DDRMIGBundle {
  val xilinxnexys4ddrmig: XilinxNexys4DDRMIGIO
  def connectXilinxNexys4DDRMIGToPads(pads: XilinxNexys4DDRMIGPads) {
    pads <> xilinxnexys4ddrmig
  }
}

trait HasMemoryXilinxNexys4DDRMIGModuleImp extends LazyModuleImp
    with HasMemoryXilinxNexys4DDRMIGBundle {
  val outer: HasMemoryXilinxNexys4DDRMIG
  val ranges = AddressRange.fromSets(p(MemoryXilinxDDRKey).address)
  require (ranges.size == 1, "DDR range must be contiguous")
  val depth = ranges.head.size
  val xilinxnexys4ddrmig = IO(new XilinxNexys4DDRMIGIO(depth))

  xilinxnexys4ddrmig <> outer.xilinxnexys4ddrmig.module.io.port
}
