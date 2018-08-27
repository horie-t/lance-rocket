// See LICENSE.HORIE_Tetsuya for license details.
package sifive.blocks.devices.vga

import chisel3._
import chisel3.util._

/** VRAM
  */
class Vram extends BlackBox {
  val io = IO(new Bundle {
    val clka = Input(Clock())
    val ena = Input(Bool())
    val wea = Input(UInt(4.W))
    val addra = Input(UInt(17.W))
    val dina  = Input(UInt(32.W))
    val douta = Output(UInt(32.W))

    val clkb = Input(Clock())
    val enb = Input(Bool())
    val web = Input(Bool())
    val addrb = Input(UInt(19.W))
    val dinb = Input(UInt(8.W))
    val doutb = Output(UInt(8.W))
  })
}
