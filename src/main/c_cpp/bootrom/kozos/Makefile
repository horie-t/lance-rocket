# RISCV environment variable must be set

CC=$(RISCV)/bin/riscv64-unknown-elf-gcc
OBJCOPY=$(RISCV)/bin/riscv64-unknown-elf-objcopy
CFLAGS=-march=rv32imac -mabi=ilp32 -O2 -std=gnu11 -Wall -I. -nostartfiles -fno-common -fno-builtin -g 
LFLAGS=-static -nostdlib -T ld.scr

elf := $(BUILD_DIR)/bare.elf
$(elf): startup.s main.c serial.c lib.c xmodem.c
	$(CC) $(CFLAGS) $(LFLAGS) -o $@ $^

.PHONY: elf
elf: $(elf)

bin := $(BUILD_DIR)/bare.bin
$(bin): $(elf)
	$(OBJCOPY) -O binary $< $@

.PHONY: bin
bin: $(bin)

hex := $(BUILD_DIR)/bare.hex
$(hex): $(bin)
	od -t x4 -An -w4 -v $< > $@

.PHONY: hex
hex: $(hex)

romgen := $(BUILD_DIR)/rom.v
$(romgen): $(hex)
	$(rocketchip_dir)/scripts/vlsi_rom_gen $(ROMCONF) $< > $@

.PHONY: romgen
romgen: $(romgen)

.PHONY: clean
clean::
	rm -rf $(hex) $(elf) 
