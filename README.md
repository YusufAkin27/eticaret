# E-Ticaret (Perde Satış) Backend

Spring Boot tabanlı e-ticaret API backend uygulaması. Ürün kataloğu, sepet, sipariş, ödeme, kargo takibi, kupon, fatura ve admin paneli API'lerini sunar.

## Teknoloji Stack

| Bileşen | Teknoloji |
|--------|-----------|
| Framework | Spring Boot 3.5.7 |
| Java | 17 |
| Veritabanı | PostgreSQL |
| Kimlik Doğrulama | JWT + OAuth2 (Google) |
| Ödeme | Iyzico |
| Medya | Cloudinary |
| E-posta | JavaMail (SMTP) |
| Kargo | DHL API |
| PDF | iText 7 |

## Gereksinimler

- **Java 17** veya üzeri
- **Maven 3.6+**
- **PostgreSQL** (veya Neon gibi uyumlu bir PostgreSQL servisi)
- **.env** dosyası (aşağıdaki değişkenlerle)

## Kurulum

### 1. Projeyi klonlayın

```bash
git clone <repo-url>
cd eticaret
```

### 2. Ortam değişkenlerini ayarlayın

Proje kökünde `.env` dosyası oluşturun. Aşağıdaki değişkenleri kendi değerlerinizle doldurun:

```env
# Veritabanı
DB_URL=jdbc:postgresql://<host>:5432/<db>?sslmode=require
DB_USERNAME=<kullanici>
DB_PASSWORD=<sifre>

# Cloudinary (görsel yükleme)
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=

# E-posta (SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=

# JWT
JWT_ACCESS_SECRET=<güçlü-gizli-anahtar>
JWT_ACCESS_EXPIRATION=86400000
JWT_REFRESH_SECRET=<güçlü-gizli-anahtar>

# Iyzico (ödeme)
IYZICO_API_KEY=
IYZICO_SECRET_KEY=
IYZICO_BASE_URL=https://sandbox-api.iyzipay.com

# Google OAuth2
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

# DHL Kargo (isteğe bağlı)
DHL_API_KEY=
DHL_API_SECRET=
DHL_SHIPPER_NAME=
DHL_SHIPPER_PHONE=
DHL_SHIPPER_EMAIL=
DHL_SHIPPER_ADDRESS_LINE=
DHL_SHIPPER_CITY=
DHL_SHIPPER_POSTAL_CODE=
DHL_SHIPPER_COUNTRY_CODE=TR

# Uygulama URL'leri
APP_BACKEND_URL=http://localhost:8080
APP_FRONTEND_URL=http://localhost:3000
APP_FRONTEND_ADMIN_URL=http://localhost:5174

# IP erişim kontrolü (isteğe bağlı, virgülle ayrılmış bloklar)
# IPACCESS_BLOCKED=203.0.113.0/24,198.51.100.77
```

> **Güvenlik:** `.env` dosyasını asla versiyon kontrolüne eklemeyin. `.gitignore` içinde olduğundan emin olun.

### 3. Uygulamayı çalıştırın

```bash
./mvnw spring-boot:run
```

Windows için:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend varsayılan olarak **http://localhost:8080** adresinde çalışır.

### 4. JAR ile çalıştırma

```bash
./mvnw clean package -DskipTests
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

## Proje Yapısı (Özet)

```
src/main/java/eticaret/demo/
├── ETicaretApplication.java    # Giriş noktası
├── admin/                      # Admin panel API'leri
│   ├── AdminOrderController, AdminProductController, ...
│   └── analytics/              # Raporlama ve analitik
├── auth/                       # Kimlik doğrulama, JWT, OAuth2
├── cart/                       # Sepet işlemleri
├── contact_us/                 # İletişim formu
├── contract/                   # Sözleşme yönetimi
├── cookie/                     # Çerez tercihleri
├── coupon/                     # Kupon ve indirim
├── cloudinary/                 # Medya yükleme
├── invoice/                    # Fatura oluşturma (PDF)
├── mail/                       # E-posta gönderimi
├── order/                      # Sipariş yönetimi
├── payment/                    # Iyzico ödeme entegrasyonu
├── product/                   # Ürün, kategori, yorum
├── recommendation/             # Ürün önerileri
├── security/                   # Güvenlik, IP kontrolü
├── shipping/                   # DHL kargo entegrasyonu
├── user/                       # Kullanıcı profili
└── visitor/                    # Ziyaretçi analitiği
```

## API Genel Bilgi

- **Müşteri API:** Ürün listeleme, sepet, sipariş, ödeme, profil, yorum, kupon, iletişim.
- **Admin API:** Ürün/kategori/kullanıcı/sipariş/kupon yönetimi, raporlar, fatura, ayarlar, denetim logları.
- **Kimlik:** JWT access/refresh token; isteğe bağlı Google OAuth2.

Frontend uygulaması (`APP_FRONTEND_URL`) ve admin paneli (`APP_FRONTEND_ADMIN_URL`) bu backend’e istek atacak şekilde yapılandırılmalıdır.

## Test

```bash
./mvnw test
```

## Lisans

Bu proje demo amaçlıdır.
