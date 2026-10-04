# Implementation Plan - Data Konstruksi (macOS Aesthetic & Supabase Sync)

Membangun ulang sistem manajemen "Project Manager" ke platform Web menggunakan Flutter dengan sinkronisasi data langsung ke akun Supabase yang sama dengan project Android.

## User Review Required

> [!IMPORTANT]
> **Data Consistency:** Menggunakan akun Supabase yang sama (`sjkhwgmkkcnwoxbplbxz`).
> **Table Names:** Menjamin penulisan nama tabel identik dengan versi Android (`gondes_workers`, `gondes_attendance`, dll).
> **UI Aesthetic:** Tetap mempertahankan gaya macOS/iMac (Glassmorphism & SF Symbols).

## Proposed Changes

### 1. Backend Configuration (Supabase)
- **Supabase Integration:** Mengganti Firebase dengan `supabase_flutter`.
- **Credential Sync:** Menggunakan URL dan Key yang sama persis dengan `SupabaseClientInstance.kt` di Android.
- **Service Layer:** Implementasi `SupabaseService` di Flutter untuk CRUD yang kompatibel dengan skema DB yang sudah ada.

### 2. UI/UX Design (macOS Look)
- **Sidebar & Dashboard:** Tetap menggunakan efek blur transparan dan ikon Cupertino.
- **Interactive Tables:** Tabel absensi yang disinkronkan langsung dengan tabel `gondes_attendance`.

---

## Verification Plan

### Automated Tests
- Menjalankan lint check untuk memastikan tidak ada konflik antar package UI.

### Manual Verification
- Verifikasi visual efek blur pada Sidebar di browser Chrome/Safari.
- Memastikan responsivitas layout tetap terjaga di berbagai ukuran jendela browser.
