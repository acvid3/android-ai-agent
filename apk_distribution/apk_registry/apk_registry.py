import os
import json
from datetime import datetime
from typing import List, Dict

APK_DIRECTORY = "apks"
REGISTRY_FILE = "apk_registry.json"

class APKRegistry:
    def __init__(self):
        self.apks: Dict[str, Dict] = {}
        self.load_registry()
    
    def load_registry(self):
        if os.path.exists(REGISTRY_FILE):
            with open(REGISTRY_FILE, 'r') as f:
                self.apks = json.load(f)
    
    def save_registry(self):
        with open(REGISTRY_FILE, 'w') as f:
            json.dump(self.apks, f, indent=2)
    
    def register_apk(self, filename: str, version: str, size: int):
        self.apks[filename] = {
            "version": version,
            "size": size,
            "uploaded_at": datetime.now().isoformat(),
            "download_count": 0
        }
        self.save_registry()
    
    def get_latest_apk(self) -> str:
        if not self.apks:
            return None
        return max(self.apks.keys(), key=lambda k: self.apks[k]["uploaded_at"])
    
    def increment_download(self, filename: str):
        if filename in self.apks:
            self.apks[filename]["download_count"] += 1
            self.save_registry()
    
    def get_all_apks(self) -> List[Dict]:
        return [
            {"filename": k, **v} 
            for k, v in self.apks.items()
        ]

if __name__ == "__main__":
    registry = APKRegistry()
    print("APK Registry initialized")
