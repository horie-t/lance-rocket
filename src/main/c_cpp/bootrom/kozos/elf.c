#include "defines.h"
#include "elf.h"
#include "lib.h"

struct elf_header {
  struct {
    unsigned char magic[4];	/* マジック・ナンバー */
    unsigned char class;	/* 32/64ビットの区分 */
    unsigned char format;	/* エンディアン情報等 */
    unsigned char version;	/* ELFフォーマットバージョン */
    unsigned char abi;		/* ABI(Abstruct Binary Interface)の種類 */
    unsigned char abi_version; 	/* ABIのバージョン */
    unsigned char reserved[7];	/* 予約領域 */
  } id;
  short type;			/* ファイルの種類 */
  short arch;			/* CPUの種類 */
  long version;			/* ELF形式のバージョン */
  long entry_point;		/* 実行開始アドレス */
  long program_header_offset;	/* プログラム・ヘッダ・テーブルの位置 */
  long section_header_offset;	/* セクション・ヘッダ・テーブルの位置 */
  long flags;			/* 各種フラグ */
  short header_size;		/* ELFヘッダのサイズ */
  short program_header_size;	/* プログラム・ヘッダ・テーブルのサイズ */
  short program_header_num;	/* プログラム・ヘッダ・テーブルの数*/
  short section_header_size;	/* セクション・ヘッダ・テーブルのサイズ */
  short section_header_num;	/* セクション・ヘッダ・テーブルの数 */
  short section_name_index;	/* セクション名を格納するセクション */
};

struct elf_program_header {
  long type;			/* セグメントの種別 */
  long offset;			/* ファイル内の位置 */
  long virtual_addr;		/* 論理アドレス */
  long physical_addr;		/* 物理アドレス */
  long file_size;		/* ファイル内のサイズ */
  long memory_size;		/* メモリ上でのサイズ */
  long flags;			/* 各種フラグ */
  long align;			/* アライメント */
};

/**
 * @brief ELFヘッダのチェック
 */
static int elf_check(struct elf_header *header)
{
  if (memcmp(header->id.magic, "\x7f" "ELF", 4))
    return -1;

  if (header->id.class   != 1) return -1;  /* ELF32 */
  if (header->id.format  != 1) return -1;  /* Little Endian */
  if (header->id.version != 1) return -1;  /* バージョンは1 */
  if (header->type       != 2) return -1;  /* 実行可能ファイル */
  if (header->version    != 1) return -1;  /* バージョンは1 */

  if (header->arch     != 243) return -1; /* EM_RISCV(243) */

  return 0;
}

/**
 * セグメント単位でのロード
 */
static int elf_load_program(struct elf_header *header)
{
  int i;
  struct elf_program_header *phdr;

  for (i = 0; i < header->program_header_num; i++) {
    phdr = (struct elf_program_header *)
      ((char *)header + header->program_header_offset + header->program_header_size * i);

    if (phdr->type != 1)	/* ロード可能なセグメントか */
      continue;

    memcpy((char *)phdr->physical_addr, (char *)header + phdr->offset,
	   phdr->file_size);
    memset((char *)phdr->physical_addr + phdr->file_size, 0,
	   phdr->memory_size - phdr->file_size);
  }

  return 0;
}

/**
 * ELF形式を解析する
 */
char *elf_load(char *buf)
{
  struct elf_header *header = (struct elf_header *)buf;

  if (elf_check(header) < 0)
    return NULL;

  if (elf_load_program(header) < 0)
    return NULL;

  return (char *)header->entry_point;
}
