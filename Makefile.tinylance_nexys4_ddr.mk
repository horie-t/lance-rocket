# See LICENSE for license details.
# See LICENSE.HORIE_Tetsuya for license details.

base_dir := $(patsubst %/,%,$(dir $(abspath $(lastword $(MAKEFILE_LIST)))))
BUILD_DIR := $(base_dir)/builds/tinylance
FPGA_DIR := $(base_dir)/src/main/fpga-shells/xilinx
MODEL := TinyLance
PROJECT := io.github.horie_t.lancerocket.tinylance
export CONFIG_PROJECT := io.github.horie_t.lancerocket.tinylance
export CONFIG := TinyLanceConfig
export BOARD := nexys4_ddr
#export BOOTROM_DIR := $(base_dir)/bootrom/xip

rocketchip_dir := $(base_dir)/rocket-chip
#sifiveblocks_dir := $(base_dir)/sifive-blocks
#VSRCS := \
#	$(rocketchip_dir)/src/main/resources/vsrc/AsyncResetReg.v \
#	$(rocketchip_dir)/src/main/resources/vsrc/plusarg_reader.v \
#	$(sifiveblocks_dir)/vsrc/SRLatch.v \
#	$(FPGA_DIR)/common/vsrc/PowerOnResetFPGAOnly.v \
#	$(BUILD_DIR)/$(CONFIG_PROJECT).$(CONFIG).rom.v \
#	$(BUILD_DIR)/$(CONFIG_PROJECT).$(CONFIG).v
VSRCS := \
	$(BUILD_DIR)/$(CONFIG_PROJECT).$(CONFIG).v

include common.mk
