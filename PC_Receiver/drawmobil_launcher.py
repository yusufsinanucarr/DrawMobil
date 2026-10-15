import socket
import subprocess
import time
import os
import ctypes
from ctypes import wintypes
import re

# --- WINDOWS API SETTINGS ---
UINT32 = ctypes.c_uint32
UINT64 = ctypes.c_uint64

PT_PEN = 3
POINTER_FLAG_INCONTACT = 0x00000004
POINTER_FLAG_INRANGE = 0x00000002
POINTER_FLAG_UPDATE = 0x00000008
POINTER_FLAG_DOWN = 0x00010000
POINTER_FLAG_UP = 0x00020000
POINTER_FLAG_FIRSTBUTTON = 0x00000010

PEN_FLAG_PRESSURE = 0x00000001
PEN_MASK_PRESSURE = 0x00000001

# Mouse Constants
MOUSEEVENTF_LEFTDOWN = 0x0002
MOUSEEVENTF_LEFTUP = 0x0004
MOUSEEVENTF_MOVE = 0x0001
MOUSEEVENTF_ABSOLUTE = 0x8000

class POINTER_INFO(ctypes.Structure):
    _fields_ = [
        ("pointerType", wintypes.DWORD),
        ("pointerId", UINT32),
        ("frameId", UINT32),
        ("pointerFlags", wintypes.DWORD),
        ("sourceDevice", wintypes.HANDLE),
        ("hwndTarget", wintypes.HWND),
        ("ptPixelLocation", wintypes.POINT),
        ("ptHimetricLocation", wintypes.POINT),
        ("ptPixelLocationRaw", wintypes.POINT),
        ("ptHimetricLocationRaw", wintypes.POINT),
        ("dwTime", wintypes.DWORD),
        ("historyCount", UINT32),
        ("InputData", wintypes.INT),
        ("dwKeyStates", wintypes.DWORD),
        ("PerformanceCount", UINT64),
        ("ButtonChangeType", wintypes.INT),
    ]

class POINTER_PEN_INFO(ctypes.Structure):
    _fields_ = [
        ("pointerInfo", POINTER_INFO),
        ("penFlags", wintypes.DWORD),
        ("penMask", wintypes.DWORD),
        ("pressure", UINT32),
        ("rotation", UINT32),
        ("tiltX", wintypes.INT),
        ("tiltY", wintypes.INT),
    ]

class StylusSimulator:
    def __init__(self):
        self.user32 = ctypes.WinDLL('user32', use_last_error=True)
        self.handle = None
        try:
            self.handle = self.user32.CreateSyntheticPointerDevice(PT_PEN, 1, 0)
            if self.handle:
                print("[OK] Sanal Kalem Aygıtı Hazır.")
            else:
                print("[!] UYARI: Kalem oluşturulamadı. Mutlak Fare moduna geçiliyor.")
        except:
            print("[!] UYARI: Kalem API pasif. Mutlak Fare moduna geçiliyor.")

        self.pc_w = self.user32.GetSystemMetrics(0)
        self.pc_h = self.user32.GetSystemMetrics(1)
        self.phone_w = 1.0
        self.phone_h = 1.0
        self.is_down = False

    def set_phone_size(self, w, h):
        self.phone_w = float(w)
        self.phone_h = float(h)
        print(f"[*] Kalibrasyon: Telefon {w}x{h} -> PC {self.pc_w}x{self.pc_h}")

    def inject_input(self, x, y, pressure, tool_type):
        # 0.0 - 1.0 arası normalize koordinatlar
        norm_x = max(0.0, min(1.0, x / self.phone_w))
        norm_y = max(0.0, min(1.0, y / self.phone_h))

        # Windows Absolute (0-65535) koordinatları
        win_x = int(norm_x * 65535)
        win_y = int(norm_y * 65535)

        # Pixel koordinatları (Loglar için)
        px_x = int(norm_x * self.pc_w)
        px_y = int(norm_y * self.pc_h)

        if self.handle:
            # --- KALEM MODU ---
            info = POINTER_PEN_INFO()
            ctypes.memset(ctypes.byref(info), 0, ctypes.sizeof(info))
            info.pointerInfo.pointerType = PT_PEN
            info.pointerInfo.ptPixelLocation = wintypes.POINT(px_x, px_y)
            info.pointerInfo.ptHimetricLocation = wintypes.POINT(win_x, win_y)

            flags = POINTER_FLAG_INRANGE | POINTER_FLAG_INCONTACT | POINTER_FLAG_FIRSTBUTTON
            if not self.is_down:
                flags |= POINTER_FLAG_DOWN
                self.is_down = True
            else:
                flags |= POINTER_FLAG_UPDATE

            info.pointerInfo.pointerFlags = flags
            info.penFlags = PEN_FLAG_PRESSURE
            info.penMask = PEN_MASK_PRESSURE
            info.pressure = int(pressure * 1024)
            self.user32.InjectSyntheticPointerInput(self.handle, ctypes.byref(info), 1)
        else:
            # --- MUTLAK FARE MODU (Hassas Oranlama ve Akıcı Çizim) ---
            # MOUSEEVENTF_ABSOLUTE kullanarak koordinatları Windows'a doğrudan 65535 ölçeğinde veriyoruz.
            # Bu, ekranın köşelerine tam ulaşmayı ve piksel piksel takip etmeyi sağlar.

            if not self.is_down:
                # 1. Önce imleci mutlak konuma taşı
                self.user32.mouse_event(MOUSEEVENTF_MOVE | MOUSEEVENTF_ABSOLUTE, win_x, win_y, 0, 0)
                # 2. Sol tık bas
                self.user32.mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
                self.is_down = True
            else:
                # Hareket halindeyken hem MOVE hem ABSOLUTE kullanarak "sürükleme" yap
                # Bu sinyal Paint'in aradaki noktaları görmesini sağlar
                self.user32.mouse_event(MOUSEEVENTF_MOVE | MOUSEEVENTF_ABSOLUTE, win_x, win_y, 0, 0)

    def lift_pen(self):
        if not self.is_down: return

        if self.handle:
            info = POINTER_PEN_INFO()
            ctypes.memset(ctypes.byref(info), 0, ctypes.sizeof(info))
            info.pointerInfo.pointerType = PT_PEN
            info.pointerInfo.pointerFlags = POINTER_FLAG_UP | POINTER_FLAG_INRANGE
            self.user32.InjectSyntheticPointerInput(self.handle, ctypes.byref(info), 1)
        else:
            # Mutlak Fare Modu - Bırakma
            self.user32.mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)

        self.is_down = False
        print("[^] KALEM/MOUSE KALDIRILDI")

# --- NETWORK SETTINGS ---
PORT = 5990

def setup_adb_reverse():
    print(f"[*] ADB Reverse (Port {PORT})...")
    subprocess.run(["adb", "reverse", f"tcp:{PORT}", f"tcp:{PORT}"], capture_output=True)

# Veri ayrıştırma için Regex pre-compile
LINE_PATTERN = re.compile(r"X:([\d.]+),Y:([\d.]+),P:([\d.]+),T:(\d+)")

def parse_line(line):
    try:
        match = LINE_PATTERN.search(line)
        if match:
            return {
                "X": float(match.group(1)),
                "Y": float(match.group(2)),
                "P": float(match.group(3)),
                "T": int(match.group(4))
            }
    except:
        pass
    return None

def main():
    os.system('cls' if os.name == 'nt' else 'clear')
    print("="*50)
    print("      DrawMobil - Sanal Grafik Tablet v4.7")
    print("="*50)

    setup_adb_reverse()
    simulator = StylusSimulator()

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
        server.bind(("0.0.0.0", PORT))
        server.listen(1)
        print(f"[*] Sunucu Aktif. Port: {PORT}")
    except Exception as e:
        print(f"!!! Port Bağlanamadı: {e}")
        return

    while True:
        try:
            client, addr = server.accept()
            print(f"\n[+] BAĞLANTI KURULDU: {addr[0]}")

            buffer = ""
            while True:
                data = client.recv(4096).decode('utf-8', errors='ignore')
                if not data: break

                buffer += data
                if "\n" in buffer:
                    lines = buffer.split("\n")
                    for line in lines[:-1]:
                        line = line.strip()
                        if not line: continue

                        if "SIZE:" in line:
                            parts = line.split(":")[1].split(",")
                            simulator.set_phone_size(int(parts[0]), int(parts[1]))
                        elif "UP" in line:
                            simulator.lift_pen()
                        elif "X:" in line:
                            parsed = parse_line(line)
                            if parsed:
                                simulator.inject_input(
                                    parsed["X"], parsed["Y"],
                                    parsed["P"], parsed["T"]
                                )
                    buffer = lines[-1]

        except Exception as e:
            print(f"\n[-] İletişim Hatası: {e}")
            simulator.lift_pen()
        finally:
            try: client.close()
            except: pass

if __name__ == "__main__":
    main()
