# Implementasi Fitur "Lihat Semua" Transaksi

Rencana ini bertujuan untuk memfungsikan tombol "Lihat semua" di layar detail transaksi agar tampilan awal lebih ringkas dan hanya menampilkan 5 transaksi terbaru.

## User Review Required

> [!NOTE]
> Perubahan ini hanya akan diterapkan pada `TransactionDetailScreen` (halaman kategori seperti Gondes, Puing, dll) karena halaman ini yang paling sering memiliki banyak transaksi.

## Proposed Changes

### UI Layer

#### [MODIFY] [TransactionDetailScreen.kt](file:///C:/Users/aidan/AndroidStudioProjects/Projectkost/app/src/main/java/com/kost/project/ui/screen/TransactionDetailScreen.kt)
- Tambahkan state `showAllTransactions` menggunakan `remember { mutableStateOf(false) }`.
- Modifikasi logika tampilan list: jika `showAllTransactions` false, hanya tampilkan 5 item pertama.
- Update tombol "Lihat semua":
    - Berfungsi sebagai *toggle* (Lihat semua <-> Sembunyikan).
    - Hanya muncul jika jumlah transaksi > 5.

## Verification Plan

### Manual Verification
- Buka kategori dengan lebih dari 5 transaksi (misal: Gondes).
- Pastikan awalnya hanya 5 transaksi terbaru yang muncul.
- Klik "Lihat semua" dan pastikan seluruh transaksi muncul dan teks tombol berubah menjadi "Sembunyikan".
- Klik "Sembunyikan" dan pastikan list kembali menciut menjadi 5 item.
- Buka kategori dengan 5 atau kurang transaksi, pastikan tombol "Lihat semua" tidak muncul.
