# See LICENSE.HORIE_Tetsuya for license details.
	
	.section .text.init
	.option norvc
	.global _start
_start:
	la sp, stack
	call main
1:
	j 1b 	# 無限ループ
