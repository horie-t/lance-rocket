#ifndef _LIB_H_INCLUDED_
#define _LIB_H_INCLUDED_

/**
 * @brief 1文字送信 
 *
 * @param c 送信する文字コード
 */
int putc(unsigned char c);	

/**
 * @brief 文字列送信
 *
 * @param str 送信する文字列
 */
int puts(unsigned char *str);

#endif
