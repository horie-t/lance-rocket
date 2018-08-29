// See LICENSE for license details.
// Execute in place
// Jump directly to XIP_TARGET_ADDR
#define GPIO_CTRL_ADDR  0x10012000
#define UART0_CTRL_ADDR 0x10013000

#define GPIO_IOF_EN     (0x38)
#define GPIO_IOF_SEL    (0x3C)
	
#define UART_REG_TXFIFO         0x00
#define UART_REG_TXCTRL         0x08
#define UART_REG_DIV            0x18
	
#define UART_TXEN               0x1
	
#define IOF0_UART0_MASK         0x00030000

	
  .section .text.init
  .option norvc
  .globl _start
_start:

_init_uart:
	// *(GPIO_CTRL_ADDR + GPIO_IOF_SEL) &= ~IOF0_UART0_MASK
	li t0, GPIO_CTRL_ADDR
	lw t1, GPIO_IOF_SEL(t0)
	li t2, IOF0_UART0_MASK
	not t3, t2
	and t1, t1, t3
	sw t1, GPIO_IOF_SEL(t0)
	// *(GPIO_CTRL_ADDR + GPIO_IOF_EN) |= IOF0_UART0_MASK
	lw t1, GPIO_IOF_EN(t0)
	or t1, t1, t2
	sw t1, GPIO_IOF_EN(t0)
	// *(UART0_CTRL_ADDR + UART_REG_DIV) = 32500000 / 115200 - 1 = 281
	li t0, UART0_CTRL_ADDR
	li t1, 281
	sw t1, UART_REG_DIV(t0)
	// *(UART0_CTRL_ADDR + UART_REG_TXCTRL) |= UART_TXEN
	lw t1, UART_REG_TXCTRL(t0)
	li t2, UART_TXEN
	or t1, t1, t2
	sw t1, UART_REG_TXCTRL(t0)
	// put('A')
	li t1, 0x41
	sb t1, UART_REG_TXFIFO(t0)
	
  csrr a0, mhartid
  la a1, dtb
  li t0, XIP_TARGET_ADDR
  jr t0

  .section .rodata
dtb:
  .incbin DEVICE_TREE
