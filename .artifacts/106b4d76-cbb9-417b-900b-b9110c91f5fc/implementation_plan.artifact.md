# Penyesuaian Warna Ikon Filter Transaksi

User ingin menyelaraskan warna ikon kalender pada bagian "Filter list Transaksi" di Dashboard Periode 4 dengan warna ikon-ikon lain di halaman tersebut (Biru Soft Elegan/`0xFF5474B3`).

## Perubahan yang Diusulkan

### 1. Penyesuaian Warna Ikon Kalender
- **File**: `DateRangeFilter.kt`
- **Tindakan**:
    - Mengatur `tint` pada `Icon` kalender di dalam `FilterChip` agar selalu menggunakan warna `Color(0xFF5474B3)`.
    - Memastikan warna ikon tetap konsisten baik saat filter aktif maupun tidak aktif.
    - Menyesuaikan `leadingIconColor` pada `FilterChipDefaults.filterChipColors` untuk memastikan keterbacaan yang baik.

## Rencana Verifikasi
### Verifikasi Manual
1. Menjalankan aplikasi dan membuka Dashboard "Periode 4".
2. Scroll ke bagian bawah pada filter tanggal.
3. Memastikan kedua ikon kalender ("Dari" dan "Sampai") berwarna biru senada dengan ikon kategori di atasnya.
4. Memastikan fungsi filter tanggal tetap berjalan dengan normal.
