@echo off
:: Calisma dizinini bat dosyasinin oldugu yer yap (Yonetici olarak calistirildiginda System32'ye gitmemesi icin)
cd /d "%~dp0"

title DrawMobil PC Receiver - Sanal Tablet
echo ==========================================
echo    DrawMobil Sanal Tablet Baslatiliyor
echo ==========================================
echo.

:: Yonetici kontrolu
net session >nul 2>&1
if %errorLevel% == 0 (
    echo [OK] Yonetici yetkisi onaylandi.
) else (
    echo [UYARI] Yonetici yetkisi bulunamadi!
    echo Lutfen bu dosyaya sag tiklayip 'Yonetici Olarak Calistir' deyin.
    echo Aksi takdirde Paint/Photoshop icine cizim yapilamaz.
    echo.
    pause
)

echo [1/2] ADB Cihazlari Kontrol Ediliyor...
adb devices
echo.
echo [2/2] Sanal Kalem Surucusu Baslatiliyor...
python PC_Receiver\drawmobil_launcher.py
pause
