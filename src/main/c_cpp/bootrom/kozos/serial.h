#ifndef _SERIAL_H_INCLUDED_
#define _SERIAL_H_INCLUDED_

/**
 * @brief デバイスの初期化を実行します。
 */
int serial_init(int index);

/**
 * @breif 送信可能かどうかを返します。
 *
 * @return 送信可能ならば0以外、そうでなければ0を返します。
 */
int serial_is_send_enable(int index);

/**
 * @brief 1バイト送信します。
 *
 * @param b 送信するバイト
 */
int serial_send_byte(int index, unsigned char b);

/**
 * @brief 受信可能かどうかを返します。
 */
int serial_is_recv_enable(int index);

/**
 * @brief 1文字受信
 */
unsigned char serial_recv_byte(int index);

#endif
