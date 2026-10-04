# Walkthrough - Dashboard & Attendance Refinement

UI untuk **Dashboard** dan **Absensi Pekerja** telah diperbarui sepenuhnya agar sesuai dengan desain yang Anda berikan, lengkap dengan visualisasi data dan tata letak profesional.

## Perubahan Utama

### 1. Dashboard Visualisasi
- **Modern Bar Chart:** Menggunakan `fl_chart` untuk menampilkan grafik pengeluaran antar periode dengan label yang presisi.
- **Periode Lainnya & Transaksi:** Menambahkan list periode historis dan tabel transaksi terbaru untuk memberikan konteks data yang lebih kaya.
- **Ringkasan Material:** Sidebar baru untuk memantau sisa hutang dan pembayaran material.

### 2. Overhaul Halaman Absensi
- **Header & Stats:** Menambahkan pencarian, tombol ekspor PDF, dan indikator status absensi (Full, 1/2 Hari, Lembur).
- **Total Upah Highlight:** Kartu besar di tengah yang menampilkan total pengeluaran upah mingguan secara mencolok.
- **Split Layout:**
    - Sisi kiri: Tabel absensi mendetail dengan status harian.
    - Sisi kanan: Ringkasan mingguan dan daftar pekerja aktif untuk akses cepat.

### 3. Branding & Aesthetic
- Mengubah teks brand di Sidebar menjadi **"Project Manager"** dengan deskripsi **"Pencatat Transaksi Proyek"**.
- Menyesuaikan palet warna utama (Green Accent) agar konsisten dengan logo dan tema konstruksi.

---

## Verifikasi Visual

Anda dapat melihat perubahan ini langsung dengan menjalankan:
```bash
cd ../data_konstruksi
flutter run -d chrome
```

> [!TIP]
> Semua data yang ditampilkan saat ini adalah data statis yang didesain agar mirip dengan screenshot Anda. Langkah selanjutnya adalah menghubungkan widget ini ke `ProjectProvider` agar data diambil secara dinamis dari Supabase.
