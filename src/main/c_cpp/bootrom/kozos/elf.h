#ifndef _ELF_H_INCLUDED_
#define _ELF_H_INCLUDED_

/**
 * @brief ELF形式の管理情報のチェックと開始アドレスの取得
 *
 * @param ELFバイナリイメージ
 * @return 読み込めたときは、開始アドレスを返します。
 */
char *elf_load(char *buf);

#endif
