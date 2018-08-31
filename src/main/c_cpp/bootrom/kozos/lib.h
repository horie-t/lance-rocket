#ifndef _LIB_H_INCLUDED_
#define _LIB_H_INCLUDED_

void *memset(void *b, int c, long len);
void *memcpy(void *dst, const void *src, long len);
int memcmp(const void *b1, const void *b2, long len);
int strlen(const char *s);
char *strcpy(char *dst, const char *src);
int strcmp(const char *s1, const char *s2);
int strncmp(const char *s1, const char *s2, int len);

/**
 * @brief 1文字送信 
 *
 * @param c 送信する文字コード
 */
int putc(char c);

/**
 * @brief 1文字受信
 */
unsigned char getc(void);

/**
 * @brief 文字列送信
 *
 * @param str 送信する文字列
 */
int puts(const char *str);

/**
 * @brief 文字受信
 *
 * @param buf 受信データ用バッファ
 */
int gets(unsigned char *buf);

/**
 * @brief 数値の16進数表記
 *
 * @param value 表示する値
 * @param column 表示桁数
 */
int putxval(unsigned long value, int column);

#endif
