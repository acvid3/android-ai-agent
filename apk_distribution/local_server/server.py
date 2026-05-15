from fastapi import FastAPI, File, UploadFile
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
import socket
import os

app = FastAPI()

APK_DIRECTORY = "apks"
os.makedirs(APK_DIRECTORY, exist_ok=True)

app.mount("/static", StaticFiles(directory=APK_DIRECTORY), name="static")

def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except:
        return "127.0.0.1"

@app.get("/")
async def root():
    return {
        "message": "APK Distribution Server",
        "local_ip": get_local_ip()
    }

@app.get("/download/{filename}")
async def download_apk(filename: str):
    file_path = os.path.join(APK_DIRECTORY, filename)
    if os.path.exists(file_path):
        return FileResponse(file_path, media_type="application/vnd.android.package-archive")
    return {"error": "File not found"}

@app.post("/upload")
async def upload_apk(file: UploadFile = File(...)):
    file_path = os.path.join(APK_DIRECTORY, file.filename)
    with open(file_path, "wb") as f:
        f.write(await file.read())
    return {"message": "APK uploaded successfully", "filename": file.filename}

if __name__ == "__main__":
    import uvicorn
    ip = get_local_ip()
    print(f"APK available at: http://{ip}:8000/download/app.apk")
    uvicorn.run(app, host="0.0.0.0", port=8000)
