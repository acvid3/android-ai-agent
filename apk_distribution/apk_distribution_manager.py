import os
import sys
import time
import threading
import subprocess
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler
from .local_server.server import app
from .qr_generator.qr_generator import generate_qr_code
from .network_detector.network_detector import get_local_ip
from .apk_registry.apk_registry import APKRegistry

class APKDistributionManager:
    def __init__(self, port=8000):
        self.port = port
        self.registry = APKRegistry()
        self.local_ip = get_local_ip()
        self.server_running = False
        self.observer = None
        self.watcher_thread = None
        
    def start(self):
        print(f"APK Distribution System starting...")
        print(f"Local IP: {self.local_ip}")
        
        self.start_build_watcher()
        self.start_server()
        
    def start_server(self):
        import uvicorn
        self.server_running = True
        print(f"APK available at: http://{self.local_ip}:{self.port}/download/app.apk")
        
        qr_url = f"http://{self.local_ip}:{self.port}/download/app.apk"
        generate_qr_code(qr_url, "apk_qr.png")
        print(f"QR Code generated: apk_qr.png")
        
        uvicorn.run(app, host="0.0.0.0", port=self.port)
        
    def start_build_watcher(self):
        apk_dir = "apks"
        if not os.path.exists(apk_dir):
            os.makedirs(apk_dir)
            
        event_handler = APKBuildHandler(self.registry)
        self.observer = Observer()
        self.observer.schedule(event_handler, apk_dir, recursive=False)
        self.observer.start()
        
        self.watcher_thread = threading.Thread(target=self.monitor_lan_status)
        self.watcher_thread.daemon = True
        self.watcher_thread.start()
        
    def monitor_lan_status(self):
        while True:
            current_ip = get_local_ip()
            if current_ip != self.local_ip:
                print(f"IP changed: {self.local_ip} -> {current_ip}")
                self.local_ip = current_ip
                qr_url = f"http://{self.local_ip}:{self.port}/download/app.apk"
                generate_qr_code(qr_url, "apk_qr.png")
            time.sleep(30)
            
    def stop(self):
        if self.observer:
            self.observer.stop()
            self.observer.join()
        self.server_running = False

class APKBuildHandler(FileSystemEventHandler):
    def __init__(self, registry):
        self.registry = registry
        
    def on_created(self, event):
        if event.src_path.endswith('.apk'):
            filename = os.path.basename(event.src_path)
            size = os.path.getsize(event.src_path)
            version = self.extract_version(filename)
            self.registry.register_apk(filename, version, size)
            print(f"New APK detected: {filename} (v{version}, {size} bytes)")
            
    def on_modified(self, event):
        if event.src_path.endswith('.apk'):
            filename = os.path.basename(event.src_path)
            size = os.path.getsize(event.src_path)
            version = self.extract_version(filename)
            self.registry.register_apk(filename, version, size)
            print(f"APK updated: {filename} (v{version}, {size} bytes)")
            
    def extract_version(self, filename):
        parts = filename.replace('.apk', '').split('-')
        if len(parts) > 1:
            return parts[-1]
        return "1.0"

if __name__ == "__main__":
    manager = APKDistributionManager()
    try:
        manager.start()
    except KeyboardInterrupt:
        manager.stop()
