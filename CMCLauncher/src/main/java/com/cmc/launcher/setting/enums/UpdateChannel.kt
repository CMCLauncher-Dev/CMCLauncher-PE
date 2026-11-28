package com.cmc.launcher.setting.enums

import androidx.annotation.StringRes
import com.cmc.launcher.R

enum class UpdateChannel(@StringRes val textRes: Int) {
    STABLE(R.string.pe_update_channel_stable),
    BETA(R.string.pe_update_channel_beta)
}