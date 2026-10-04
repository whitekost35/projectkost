# Fix Attendance Status Discrepancy (LEMBUREKSTRA)

The application currently fails to map the `LEMBUREKSTRA` status from Supabase to the internal `LEMBUR_EKSTRA` enum because of a naming mismatch in the `@SerialName` annotation. This results in the app defaulting to `OFF` (Libur) when displaying attendance data fetched from the cloud.

## Proposed Changes

### [Component Name] Data Model

#### [MODIFY] [Worker.kt](file:///C:/Users/aidan/Aplikasi/ProjectKost/app/src/main/java/com/kost/project/data/model/Worker.kt)
- Update the `@SerialName` for `AttendanceStatus.LEMBUR_EKSTRA` from `"LEMBUR_EKSTRA"` to `"LEMBUREKSTRA"` to match the Supabase schema.
- (Optional but recommended) Verify and update other status serial names if they follow the same pattern (e.g., `LEMBUR1` instead of `LEMBUR_1`). Based on the screenshot, only `LEMBUREKSTRA` is confirmed.

## Verification Plan

### Manual Verification
- Re-run the app.
- Perform a "Sync" or "Fetch from Cloud" action.
- Verify that workers with `LEMBUREKSTRA` status in Supabase now show the "Ekstra" (Star icon) status instead of "OFF" (Red minus icon) on the **Absensi Pekerja** screen.
- Verify that the wage calculation correctly includes the `LEMBUREKSTRA` days.
