# Implementation Plan - Remove Filter from Periode 4 Summary Screen

The user wants to remove the date filter UI from the "Periode 4" summary screen because it doesn't contain a transaction list. The filter should remain on other pages like category details (Bumimas, Puing), other periods (P1, Mykost), and kasbon.

## Proposed Changes

### UI Components

#### [MODIFY] [ClassificationScreen.kt](file:///C:/Users/aidan/AndroidStudioProjects/Projectkost/app/src/main/java/com/kost/project/ui/screen/ClassificationScreen.kt)
- Conditionally hide the `DateRangeFilter` component.
- The condition will be `if (periodId != "p4")`. Since `ClassificationScreen` is currently only used for "Periode 4" (`p4`), this will effectively remove it from that screen while preserving it if the screen is ever reused for other periods that might require it (though the user specifically mentioned other periods use a different screen).

## Verification Plan

### Manual Verification
1. Open the app and navigate to "Periode 4" from the Dashboard.
2. Verify that the date filter (Dari/Sampai) is no longer visible at the top.
3. Navigate to "Periode 1" or "Mykost" and verify the filter is still present.
4. Navigate to a category detail (e.g., "Gondes" or "Puing") and verify the filter is still present.
