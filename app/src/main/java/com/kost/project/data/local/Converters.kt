package com.kost.project.data.local

import androidx.room.TypeConverter
import com.kost.project.data.model.AttendanceStatus
import com.kost.project.data.model.WorkerRole

class Converters {
    @TypeConverter
    fun fromWorkerRole(role: WorkerRole?): String? = role?.name

    @TypeConverter
    fun toWorkerRole(value: String?): WorkerRole? = value?.let { WorkerRole.valueOf(it) }

    @TypeConverter
    fun fromAttendanceStatus(status: AttendanceStatus?): String? = status?.name

    @TypeConverter
    fun toAttendanceStatus(value: String?): AttendanceStatus? = value?.let { AttendanceStatus.valueOf(it) }
}
