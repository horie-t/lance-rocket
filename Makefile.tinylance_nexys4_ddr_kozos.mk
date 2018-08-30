# See LICENSE for license details.
# See LICENSE.HORIE_Tetsuya for license details.

base_dir := $(patsubst %/,%,$(dir $(abspath $(lastword $(MAKEFILE_LIST)))))
BUILD_DIR := $(base_dir)/builds/tinylance
FPGA_DIR := $(base_dir)/src/main/fpga-shells/xilinx
MODEL := TinyLanceChip
PROJECT := lancerocket.tinylance
export CONFIG_PROJECT := lancerocket.tinylance
export CONFIG := TinyLanceConfig
export BOARD := nexys4_ddr
export BOOTROM_DIR := $(base_dir)/src/main/c_cpp/bootrom/kozos

rocketchip_dir := $(base_dir)/rocket-chip
VSRCS := \
	$(rocketchip_dir)/src/main/resources/vsrc/AsyncResetReg.v \
	$(rocketchip_dir)/src/main/resources/vsrc/plusarg_reader.v \
	$(base_dir)/src/main/verilog/SRLatch.v \
	$(base_dir)/src/main/verilog/xilinx/PowerOnResetFPGAOnly.v \
	$(BUILD_DIR)/$(CONFIG_PROJECT).$(CONFIG).rom.v \
	$(BUILD_DIR)/$(CONFIG_PROJECT).$(CONFIG).v

include common.mk
