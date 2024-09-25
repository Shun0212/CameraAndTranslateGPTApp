import socket
import qrcode
from flask import Flask, request, render_template, send_file
import os
import webbrowser
import threading
import base64

app = Flask(__name__)
saved_image_path = None
gpt_result = None

# ローカルIPアドレスを取得する関数
def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # 外部のホストに接続し、ローカルのIPアドレスを取得
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = "127.0.0.1"  # エラーが発生した場合はlocalhostを使用
    finally:
        s.close()
    return ip

@app.route('/', methods=['POST'])
def receive_url():
    global saved_image_path, gpt_result

    data = request.get_json()  # JSON形式のデータを取得
    github_url = data.get('github_url')
    image_data = data.get('image')
    gpt_result = data.get('gpt_result')

    print(f"Received POST request data: github_url={github_url}, image_data={'Yes' if image_data else 'No'}, gpt_result={gpt_result}")

    if github_url:
        print(f"Received GitHub URL: {github_url}")
        success = webbrowser.open(github_url)
        if success:
            print("URL successfully opened in browser")
        else:
            print("Failed to open URL in browser")

    if image_data:
        try:
            # Base64でエンコードされた画像データをデコードして保存
            image_bytes = base64.b64decode(image_data)
            saved_image_path = 'static/received_image.jpg'  # Flaskのstaticフォルダに保存
            with open(saved_image_path, 'wb') as f:
                f.write(image_bytes)
            print(f"Image saved at {saved_image_path}")
        except Exception as e:
            print(f"Failed to save image: {e}")

    # データ受信後にresult.htmlを表示
    return render_template('result.html', image_path=saved_image_path, gpt_result=gpt_result)


# 画像と結果を表示するためのHTMLページ
@app.route('/result')
def display_result():
    global saved_image_path, gpt_result
    if saved_image_path and gpt_result:
        return render_template('result.html', image_path=saved_image_path, gpt_result=gpt_result)
    else:
        return "No data available", 400

# QRコード生成して表示するルート
@app.route('/qrcode')
def generate_qr():
    # サーバのURLをQRコード化
    server_url = f"http://{get_local_ip()}:5000"
    img = qrcode.make(server_url)
    
    # QRコードを保存
    img.save("qrcode.png")
    
    # テンプレートにQRコード画像を埋め込んで表示
    return render_template('qrcode.html')

# QRコード画像を返すルート
@app.route('/qrcode_image')
def qrcode_image():
    return send_file("qrcode.png", mimetype='image/png')

# Flaskサーバを別スレッドで起動し、ブラウザで自動的にQRコードページを開く
def start_server():
    local_ip = get_local_ip()  # 自分のIPアドレスを取得
    print(f"Starting server on {local_ip}:5000")
    app.run(host=local_ip, port=5000)

if __name__ == '__main__':
    # Flaskサーバを別スレッドで起動
    threading.Thread(target=start_server).start()
    
    # ブラウザで自動的にQRコードページを開く
    local_ip = get_local_ip()
    webbrowser.open(f"http://{local_ip}:5000/qrcode")

    # データ送信後にresultページを自動で開く
    webbrowser.open(f"http://{local_ip}:5000/result")
