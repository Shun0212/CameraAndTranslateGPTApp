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


This project is an Android application that leverages the GitHub repository search and OpenAI API, offering development support through an Android-specific interface.

## Key Features

### 1. GitHub Repository README Translation Feature
- Search for repositories on GitHub
- Use OpenAI API to translate the README into Japanese

### 2. Image Recognition Function via Camera
- Image recognition using the OpenAI API
- Analyze based on specific prompts with multiple templates available

### 3. ChatGPT Functionality
- Q&A and conversations using the ChatGPT API

### 4. Voice Recognition and Translation Feature
- Record voice using the Android device's microphone
- Convert speech to text using the Whisper API
- Translate text with ChatGPT
- Play back the translated text using Text-to-Speech (TTS)

## PC Integration Feature

### QR Code-Based PC Communication

1. Start an HTTP server on the PC and generate a QR code
2. Scan the QR code with the Android app
3. Send data to the URL read from the QR code
4. Process the data on the PC (e.g., display the URL or image, handle ChatGPT responses)

## Project Structure

- **Android App (master branch)**: 
  - GitHub repository search and translation
  - Camera functionality
  - Voice recognition
  - ChatGPT functionality

- **PC-Side Program**:
  - Communication via QR code
  - Receiving and processing URLs or image data

## Installation and Setup

### Android App

1. Clone this repository
2. Open the project in Android Studio
3. Set up the necessary API keys (OpenAI API, Whisper API, etc.)
4. Build the app and install it on an Android device

### PC-Side

1. Install the required libraries:
   ```bash
   pip install flask qrcode[pil]
   ```

2. Start the server and generate the QR code:
   ```bash
   python app.py
   ```

3. Scan the QR code with the Android app and send the data


