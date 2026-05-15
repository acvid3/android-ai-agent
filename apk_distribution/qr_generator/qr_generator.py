import qrcode

def generate_qr_code(url: str, filename: str = "qr_code.png"):
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_L,
        box_size=10,
        border=4,
    )
    qr.add_data(url)
    qr.make(fit=True)
    
    img = qr.make_image(fill_color="black", back_color="white")
    img.save(filename)
    return filename

if __name__ == "__main__":
    generate_qr_code("http://192.168.1.100:8000/download/app.apk")
