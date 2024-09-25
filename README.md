# ChatGPT APIを用いたAndroidアプリ

このプロジェクトは、GitHubのリポジトリ検索やOpenAIのAPIを活用し、Androidならではのインターフェースを用いた開発支援を提供するアプリケーションです。

## 主な機能

### 1. GitHubリポジトリのREADME翻訳機能
- GitHub上のリポジトリを検索
- OpenAI APIを使用してREADMEを日本語に翻訳

### 2. カメラで撮影した画像の認識機能
- OpenAI APIを利用した画像認識
- 複数のテンプレートを用意し、指定したプロンプトに基づいて解析

### 3. ChatGPT機能
- ChatGPT APIを使用したQ&Aや会話

### 4. 音声認識と翻訳機能
- Androidデバイスのマイクで音声録音
- Whisper APIでテキスト化
- ChatGPTで翻訳
- TTS（Text-to-Speech）で音声再生

## PCとの連携機能

### QRコードによるPCとの通信

1. PC側でHTTPサーバを起動し、QRコードを生成
2. AndroidアプリでQRコードを読み取り
3. 読み取ったURLにデータを送信
4. PC上で処理（URLや画像の表示、ChatGPT応答の処理など）

## プロジェクトの構成

- **Androidアプリ（masterブランチ）**: 
  - GitHubリポジトリの検索・翻訳
  - カメラ機能
  - 音声認識機能
  - ChatGPT機能

- **PC側プログラム**:
  - QRコードを介した通信機能
  - URLや画像データの受信・処理

## インストールとセットアップ

### Androidアプリ

1. このリポジトリをクローン
2. Android Studioでプロジェクトを開く
3. 必要なAPIキーを設定（OpenAI API、Whisper APIなど）
4. アプリをビルドしAndroidデバイスにインストール

### PC側

1. 必要なライブラリをインストール:
   ```bash
   pip install flask qrcode[pil]
   ```

2. サーバーを起動しQRコードを生成:
   ```bash
   python app.py
   ```

3. AndroidアプリでQRコードをスキャンし、データを送信



## 連絡先

[連絡先や問い合わせ方法をここに記載]
