import socket
import platform

def get_local_ip():
    system = platform.system()
    
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except:
        return "127.0.0.1"

def get_network_info():
    ip = get_local_ip()
    return {
        "local_ip": ip,
        "system": platform.system(),
        "url": f"http://{ip}:8000"
    }

if __name__ == "__main__":
    info = get_network_info()
    print(f"Local IP: {info['local_ip']}")
    print(f"Access URL: {info['url']}")
