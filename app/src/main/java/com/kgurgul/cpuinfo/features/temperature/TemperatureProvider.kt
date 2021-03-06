/*
 * Copyright 2017 KG Soft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kgurgul.cpuinfo.features.temperature

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.TextUtils
import com.kgurgul.cpuinfo.utils.Utils
import io.reactivex.Maybe
import io.reactivex.rxkotlin.toObservable
import java.io.File

/**
 * Custom provider which provides all temperatures
 *
 * @author kgurgul
 */
class TemperatureProvider(val app: Application) {

    /**
     * @return battery temperature
     */
    fun getBatteryTemperature(): Int {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = app.registerReceiver(null, iFilter)
        return batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10
    }

    /**
     * @return CPU temperature
     */
    fun getCpuTemp(path: String): Float? {
        val temp = Utils.readOneLine(File(path)) ?: 0.0

        return if (isTemperatureValid(temp)) {
            temp.toFloat()
        } else {
            (temp / 1000).toFloat()
        }
    }

    /**
     * Scan device looking for CPU temperature in all well known locations
     */
    fun getCpuTemperatureFinder(): Maybe<CpuTemperatureResult> {
        return CPU_TEMP_FILE_PATHS.toObservable()
                .map { path ->
                    val temp = Utils.readOneLine(File(path))
                    var validPath = ""
                    var currentTemp = 0.0
                    if (temp != null) {
                        // Verify if we are in normal temperature range
                        if (isTemperatureValid(temp)) {
                            validPath = path
                            currentTemp = temp
                        } else if (isTemperatureValid(temp / 1000)) {
                            validPath = path
                            currentTemp = temp / 1000
                        }
                    }
                    CpuTemperatureResult(validPath, currentTemp.toInt())
                }
                .filter { (filePath) -> !TextUtils.isEmpty(filePath) }
                .firstElement()
    }

    /**
     * Check if passed temperature is in normal range: -30 - 250 Celsius
     *
     * @param temp current temperature
     */
    private fun isTemperatureValid(temp: Double): Boolean {
        return temp >= -30.0 && temp <= 250.0
    }

    /**
     * Container for temperature value and path
     */
    data class CpuTemperatureResult(val filePath: String = "", val temp: Int = 0)

    // Ugly but currently working solution to get temperature is to search well known locations
    // If you know better solution please refactor this :)
    private val CPU_TEMP_FILE_PATHS = listOf(
            "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
            "/sys/devices/system/cpu/cpu0/cpufreq/FakeShmoo_cpu_temp",
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/i2c-adapter/i2c-4/4-004c/temperature",
            "/sys/devices/platform/tegra-i2c.3/i2c-4/4-004c/temperature",
            "/sys/devices/platform/omap/omap_temp_sensor.0/temperature",
            "/sys/devices/platform/tegra_tmon/temp1_input",
            "/sys/kernel/debug/tegra_thermal/temp_tj",
            "/sys/devices/platform/s5p-tmu/temperature",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/hwmon/hwmon0/device/temp1_input",
            "/sys/devices/virtual/thermal/thermal_zone1/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone3/temp",
            "/sys/class/thermal/thermal_zone4/temp",
            "/sys/class/hwmon/hwmonX/temp1_input",
            "/sys/devices/platform/s5p-tmu/curr_temp")
}