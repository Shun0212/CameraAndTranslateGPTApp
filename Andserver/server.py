import socket
import qrcode
from flask import Flask, request, render_template, send_file
import os
import webbrowser
import threading

app = Flask(__name__)

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
    github_url = request.form.get('github_url')
    if github_url:
        print(f"Received GitHub URL: {github_url}")
        
        # URLをブラウザで自動的に開く
        webbrowser.open(github_url)
        
        return "URL received and opened", 200
    else:
        return "No URL received", 400

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