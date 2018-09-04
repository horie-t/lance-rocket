# Lance Rocket

RISC-Vの最新の仕様の実装を見てみたい場合は、[Rocket Chip Generator](https://github.com/freechipsproject/rocket-chip)が良いです。ですが、これだけだと周辺機器のモジュールが全然ないので、FPGA上で動かしても面白みがないです。Rocket Chipに周辺機器のコントロール用のモジュールとBoard Support Package等のSDKを加えたものが、[Freedom](https://github.com/sifive/freedom)です。これは、各種周辺機器のモジュールもついて、ソフトウェアの開発環境まで揃っています。これがあれば十分とも言えるのですが、Freedomは、SoC(System on Chip)のカスタマイズの販売で企業を維持していくようで、FPGA向けのコードは、SoCのコードにラッパー・コードを追加するような形になっています。そのため、FreedomでサポートしているFPGAボード以外に移植しようとすると、結構大変です。

本プロジェクトでは、Rocket Chip GeneratorやFreedomからコードを持ってきて、FPGA向けに簡素な形で実装し、制約ファイルの編集ぐらいで、各種のボードに移植できるような実装を作成します。

以下の4種類のレベルのものを実装する予定です。

1. RISC-Vの学習用(TinyLance)  
32ビット版Rocket Chipを動かしてみて、RISC-Vの勉強をする目的用です。[KOZOS](http://kozos.jp/kozos/)を移植して、組み込みOSの勉強ができるようにします。周辺機器として、スイッチ、LED、UARTを持っています。
2. 組込み用マイクロ・コントローラ用(BabyLance)  
IoT(Internet of Things)といった用途向けです。1.の周辺機器に加え、DRAMやSDカード、イーサネット、GPIOを備えます。
3. パーソナル・コンピュータ用(FryingLance)  
64ビット版Rocket Chipをベースに、2の周辺機器に加え、VGAアダプタ、キーボードやマウス、USBを加えます。ここで、[はりぼてOS](http://hrb.osask.jp/)のRISC-V版として、「かきわりOS」なるものを作成します。(できれば、Linuxも動かしたい)
4. ハイ・パフォーマンス・コンピュータ用(ThunderLance)  
3.のマルチコア版です。(できれば、複数のボードを繋いで、NUMAアーキテクチャを実装したいが…)


