# See LICENSE.HORIE_Tetsuya for license details.
	
	.section .text.init
	.option norvc
	.globl _start
_start:
	li sp, 0x80004000
	call main
1:
	j 1b 	# 無限ループ

