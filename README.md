<div align="center">

# ✍️ DrawMobil
### Turn Your Android Device into a Low-Latency Windows Graphics Tablet
**Mobil Cihazınızı Windows İçin Düşük Gecikmeli Sanal Grafik Tablete Dönüştürün**

[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Windows-blue.svg)](https://github.com/yusufsinanucarr/DrawMobil)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-purple.svg)](https://kotlinlang.org/)
[![Python](https://img.shields.io/badge/Python-3.10+-yellow.svg)](https://www.python.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

<br/>

*(İsteğe bağlı: Çalışma anını gösteren bir GIF veya ekran görüntüsünü buraya ekleyebilirsiniz)*  
<!-- <img src="docs/demo.gif" alt="DrawMobil Demo" width="700"/> -->

</div>

---

## 📌 Proje Hakkında (About the Project)

Piyasadaki profesyonel çizim ve grafik tabletleri, öğrenciler ve hobi sahipleri için maliyetli olabilmektedir. **DrawMobil**, halihazırda sahip olduğunuz Android cihazların dokunmatik ekranını ve stylus (S-Pen vb.) donanımını kullanarak bilgisayarınızda (Photoshop, Paint, Krita vb.) pürüzsüz çizim yapmanızı sağlayan donanım-yazılım köprüsüdür.

Bu proje; düşük seviyeli sistem çağrıları (Win32 Pointer API), gerçek zamanlı soket iletişimi ve yüksek kare hızlı giriş yakalama mimarisi üzerine inşa edilmiştir.

---

## 🌟 Öne Çıkan Özellikler (Key Features)

* **🖊️ Stylus & Basınç (Pressure) Desteği:** S-Pen veya uyumlu dijital kalemlerde 1024 seviyeli basınç hassasiyeti ve silgi (eraser) modu tespiti.
* **⚡ Düşük Gecikme (Ultra-Low Latency):** USB üzerinden `ADB Reverse Port Forwarding` ve Kotlin Coroutine `Channel.CONFLATED` yapısı ile tampon gecikmesini (buffer bloat) engelleyen gerçek zamanlı veri akışı.
* **🎯 Windows Donanım Entegrasyonu:** Python `ctypes` aracılığıyla Windows'un yerel `CreateSyntheticPointerDevice` ve `InjectSyntheticPointerInput` API'leri kullanılarak işletim sistemine gerçek bir sayısallaştırıcı tablet (digitizer pen) gibi tanıtılır. Kalem API'sinin desteklenmediği durumlarda pürüzsüz **Absolute Mouse** moduna otomatik geri çekilme (fallback).
* **📐 Dinamik Ekran Ölçekleme:** Android ekran çözünürlüğünü PC ekran çözünürlüğüne piksel piksel haritalayan normalize mutlak koordinat (0-65535) algoritması.

---

## 🏗️ Mimari & Çalışma Mantığı (Architecture)

```text
+-----------------------+                    +------------------------+
|    Android Cihaz      |                    |       Windows PC       |
|  (DrawMobil Mobil)    |                    |   (PC Receiver Script) |
+-----------------------+                    +------------------------+
| • MotionEvent Yakalama|                    | • TCP Socket Server    |
| • Basınç / Araç Tipi  |   USB / ADB        | • Koordinat Eşleme     |
| • Conflated Channel   | ═════════════════> | • Win32 Synthetic      |
| • TCP Socket Client   |   Port: 5990       |   Pointer Input        |
+-----------------------+                    +-----------┬------------+
                                                         |
                                                         v
                                             +------------------------+
                                             |  Çizim Yazılımları     |
                                             | (Photoshop/Paint/vb.)  |
                                             +------------------------+
```

---

## 🛠️ Kullanılan Teknolojiler (Tech Stack)

| Alan | Teknoloji / Kütüphane | Açıklama |
| :--- | :--- | :--- |
| **Android UI** | Kotlin, Jetpack Compose | Modern ve reaktif arayüz bileşenleri |
| **Android Asenkron** | Kotlin Coroutines & Channels | `Channel.CONFLATED` ile kuyruk tıkanması engellenmiş I/O işleme |
| **Donanım Algılama** | Android `MotionEvent` API | Dokunma, stylus, silgi ve basınç verilerinin ham okunması |
| **İletişim / Ağ** | TCP Sockets & ADB Port Forwarding | Cihaz ile PC arasında sıfıra yakın gecikmeli USB tüneli |
| **PC Receiver** | Python 3 | Windows soket dinleyicisi ve veri ayrıştırıcı (Regex) |
| **Sistem Entegrasyonu**| Windows Win32 API (`ctypes`) | `user32.dll` üzerinden sanal giriş cihazı simülasyonu |

---

## 🚀 Kurulum ve Çalıştırma (Getting Started)

### Gereksinimler
1. Android 8.0+ yüklü bir mobil cihaz (Tercihen Stylus/S-Pen destekli).
2. Windows 10/11 yüklü bir bilgisayar.
3. Bilgisayarda **Python 3.8+** ve Android Platform Tools (**ADB**).
4. Cihazınızda **USB Hata Ayıklama (USB Debugging)** açık olmalıdır.

### 1. Adım: PC Alıcısını Başlatma
1. Telefonunuzu USB kablosuyla bilgisayara bağlayın.
2. Proje dizinindeki `Baslat_PC.bat` dosyasına sağ tıklayıp **Yönetici Olarak Çalıştır**'ı seçin.
   > *Not: Windows'un çizim uygulamalarına kalem girdisi enjekte edebilmesi için yönetici yetkisi gereklidir.*
3. Konsolda `[OK] Sanal Kalem Aygıtı Hazır.` ve `[*] Sunucu Aktif. Port: 5990` mesajlarını görün.

### 2. Adım: Mobil Uygulamayı Çalıştırma
1. Projeyi Android Studio ile açın ve telefonunuza yükleyin (`Run 'DrawMobil'`).
2. Uygulama açıldığında ekranda `BAĞLANDI! - PC Aktif` ibaresi belirecektir.
3. Bilgisayarınızda Paint, Photoshop veya Krita'yı açıp çizim yapmaya başlayabilirsiniz.

---

## 👥 Geliştirici Ekibi (Contributors)

Bu proje, üniversite 2. sınıf Yazılım Mühendisliği öğrencileri tarafından bir takım çalışması olarak geliştirilmektedir:

* **Yusuf Sinan Uçar** — *Proje Yürütücüsü & Android / Entegrasyon Geliştirici*
* Ekip Arkadaşları / Katkıda Bulunanlar:
  * Batuhan K.
  * Barış U.
  * Taha Ö.
  * Yiğit E.

---

## 📄 Lisans (License)

Bu proje [MIT Lisansı](LICENSE) kapsamında açık kaynak olarak lisanslanmıştır.
