// See LICENSE.SiFive for license details.
// Execute in place
	
  .section .text.init
  .option norvc
  .globl _start
_start:
1:
	j 1b 	// 無限ループ

  .section .rodata
dtb:
  .incbin DEVICE_TREE
