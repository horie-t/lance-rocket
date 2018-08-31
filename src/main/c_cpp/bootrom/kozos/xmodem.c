#include "defines.h"
#include "serial.h"
#include "lib.h"
#include "xmodem.h"

#define XMODEM_SOH 0x01
#define XMODEM_STX 0x02
#define XMODEM_EOT 0x04
#define XMODEM_ACK 0x06
#define XMODEM_NAK 0x15
#define XMODEM_CAN 0x18
#define XMODEM_EOF 0x1a

#define XMODEM_BLOCK_SIZE 128

/**
 * @breif 受信開始されるまで、送信要求を出す。
 */
static int xmodem_wait(void)
{
  long cnt = 0;

  /* 受信開始するまでNAKを定期的に送信する */
  while (!serial_is_recv_enable(SERIAL_DEFAULT_DEVICE)) {
    if (++cnt >= 8000000) {
      cnt = 0;
      serial_send_byte(SERIAL_DEFAULT_DEVICE, XMODEM_NAK);
    }
  }

  return 0;
}

/**
 * @brief ブロック単位での受信
 */
static int xmodem_read_block(unsigned char block_number, char *buf)
{
  unsigned char c, block_num, check_sum;
  int i;

  // ブロック番号を受信
  block_num = serial_recv_byte(SERIAL_DEFAULT_DEVICE);
  if (block_num != block_number)
    return -1;

  // 反転したブロック番号を受信(XORをとると全ビットが1になるはず)
  block_num ^= serial_recv_byte(SERIAL_DEFAULT_DEVICE);
  if (block_num != 0xff)
    return -1;

  // 128バイトのデータを受信する。
  check_sum = 0;
  for (i = 0; i < XMODEM_BLOCK_SIZE; i++) {
    c = serial_recv_byte(SERIAL_DEFAULT_DEVICE);
    *(buf++) = c;
    check_sum += c;
  }

  // チェックサムの受信(XORをとると0になるはず)
  check_sum ^= serial_recv_byte(SERIAL_DEFAULT_DEVICE);
  if (check_sum)
    return -1;

  return i;
}

long xmodem_recv(char *buf)
{
  int r, receiving = 0;
  long size = 0;
  unsigned char c, block_number = 1;

  while (1) {
    if (!receiving)
      xmodem_wait();

    c = serial_recv_byte(SERIAL_DEFAULT_DEVICE);

    if (c == XMODEM_EOT) {
      /* 受信終了 */
      serial_send_byte(SERIAL_DEFAULT_DEVICE, XMODEM_ACK);
      break;
    } else if (c == XMODEM_CAN) {
      /* 受信キャンセル */
      return -1;
    } else if (c == XMODEM_SOH) {
      /* 受信を開始する */
      receiving++;
      r = xmodem_read_block(block_number, buf);
      if (r < 0) {
	/* 受信エラー */
	serial_send_byte(SERIAL_DEFAULT_DEVICE, XMODEM_NAK);
      } else {
	/* 正常受信 */
	block_number++;
	size += r;
	buf  += r;
	serial_send_byte(SERIAL_DEFAULT_DEVICE, XMODEM_ACK);
      }
    } else {
      if (receiving)
	return -1;
    }
  }

  return size;
}

