ChatGPT APIを用いたAndroidアプリ

このプロジェクトは、GitHubのリポジトリ検索や、OpenAIのAPIを活用してAndroidならではのインタフェースを提供するアプリケーションです。主に以下の機能を実装しています。
主な機能
1. GitHubリポジトリのREADME翻訳機能

GitHub上のリポジトリを検索し、そのREADMEをOpenAIのAPIを使用して日本語に翻訳できます。これにより、英語のリポジトリでも簡単に内容を理解することができます。
2. カメラで撮影した画像の認識機能

カメラで撮影した画像をOpenAIのAPIを利用して画像認識を行います。いくつかのテンプレートを用意しており、指定したプロンプトに基づいて画像を解析します。
3. ChatGPT機能

ChatGPT APIを使用して、ユーザーが自由に質問をしたり、会話をしたりすることができます。
4. 音声認識と翻訳機能

Androidデバイスのマイクを使用して音声を録音し、Whisper APIでテキスト化します。その後、ChatGPTを使って翻訳を行い、翻訳結果をTTS（Text-to-Speech）で音声として再生します。
PCとの連携機能
QRコードによるPCとの通信

GitHubリポジトリや撮影した画像をPCに送信するための機能も実装しています。以下の流れでAndroidとPCが連携します。

    PC側でHTTPサーバを起動
    サーバのURLが含まれたQRコードを生成します。

    AndroidアプリでQRコードを読み取る
    読み取ったQRコードからPCのURLを取得し、リポジトリURLや撮影した画像をPC側に送信します。

    PC上で処理
    PCでは、受け取ったURLや画像を表示したり、ChatGPTの返答を処理するプログラムが動作します。これにより、PCのブラウザで現在Androidアプリで見ているGitHubページを表示したり、画像解析の結果を表示することができます。

プロジェクトの構成

    Androidアプリ（masterブランチ）:
        GitHubリポジトリの検索・翻訳、カメラ機能、音声認識機能、ChatGPT機能を実装。
    PC側プログラム:
        QRコードを介した通信機能を提供。URLや画像データを受け取り、処理を行います。

インストールとセットアップ
Androidアプリ

    このリポジトリをクローンし、Android Studioでプロジェクトを開きます。
    必要なAPIキーを設定します（OpenAIのAPIキー、Whisper APIキーなど）。
    アプリをビルドして、Androidデバイスにインストールします。

PC側

    Flaskや必要なライブラリをインストールします。

    bash

pip install flask qrcode[pil]

PC側のサーバーを起動し、QRコードを生成します。

bash

    python app.py

    AndroidアプリでQRコードをスキャンし、データを送信します。

