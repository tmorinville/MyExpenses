package org.totschnig.myexpenses.util.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.text.format.DateFormat
import android.text.method.LinkMovementMethod
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.ColorUtils.calculateLuminance
import androidx.core.widget.ImageViewCompat
import app.futured.donut.DonutSection
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.ui.filter.ScrollingChip
import org.totschnig.myexpenses.util.PictureDirHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.io.getMimeType
import org.totschnig.myexpenses.util.readPrimaryTextColor
import org.totschnig.myexpenses.util.ui.UiUtils.DateMode
import org.totschnig.myexpenses.viewmodel.ContentResolvingAndroidViewModel.Companion.lazyMap
import org.totschnig.myexpenses.viewmodel.data.AttachmentInfo
import org.totschnig.myexpenses.viewmodel.data.PageAccount
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

interface Itag {
    val label: String

    @get:ColorInt
    val color: Int?
}


fun <T : Itag> ChipGroup.addChipsBulk(
    chips: Iterable<T>,
    closeFunction: ((T) -> Unit)? = null,
) {
    removeAllViews()
    for (chip in chips) {
        addView(ScrollingChip(context).also { scrollingChip ->
            scrollingChip.text = chip.label
            chip.color?.let { scrollingChip.setColor(it) }
            closeFunction?.let {
                scrollingChip.isCloseIconVisible = true
                scrollingChip.setOnCloseIconClickListener {
                    removeView(scrollingChip)
                    it(chip)
                }
            }
        })
    }
}

fun ChipGroup.addChipsBulk(chips: Iterable<String>) {
    addChipsBulk(chips.map {
        object : Itag {
            override val label: String = it
            override val color: Int? = null
        }
    })
}

fun colorWithPressedFeedback(@ColorInt base: Int) = ColorStateList(
    arrayOf(
        intArrayOf(android.R.attr.state_pressed),
        intArrayOf(android.R.attr.state_enabled)
    ), intArrayOf(
        ColorUtils.setAlphaComponent(base, 128),
        base
    )
)

fun Chip.setColor(@ColorInt color: Int) {
    val chipColor = colorWithPressedFeedback(color)
    chipBackgroundColor = chipColor
    val bestForeground = getBestForeground(color)
    val foreground = ColorStateList.valueOf(bestForeground)
    setTextColor(foreground)
    checkedIconTint = foreground
    closeIconTint = colorWithPressedFeedback(bestForeground)
}

fun ScrollView.postScrollToBottom() {
    post {
        fullScroll(View.FOCUS_DOWN)
    }
}

fun setNightMode(prefHandler: PrefHandler, context: Context) {
    AppCompatDelegate.setDefaultNightMode(
        when (prefHandler.getString(
            PrefKey.UI_THEME,
            context.getString(R.string.pref_ui_theme_default)
        )) {
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    )
}

fun getBestForeground(color: Int) =
    if (calculateLuminance(color) > 0.5) Color.BLACK else Color.WHITE

fun <T : View> findParentWithTypeRecursively(view: View, type: Class<T>): T? {
    if (type.isInstance(view)) {
        @Suppress("UNCHECKED_CAST")
        return view as T
    }
    val parent = view.parent
    return if (parent is View) findParentWithTypeRecursively(parent as View, type) else null
}

fun getDateMode(accountType: AccountType?, prefHandler: PrefHandler) = when {
    (accountType == null || accountType != AccountType.CASH) &&
            prefHandler.getBoolean(PrefKey.TRANSACTION_WITH_VALUE_DATE, false)
        -> DateMode.BOOKING_VALUE

    prefHandler.getBoolean(PrefKey.TRANSACTION_WITH_TIME, true) -> DateMode.DATE_TIME
    else -> DateMode.DATE
}

private fun timeFormatter(accountType: AccountType?, prefHandler: PrefHandler, context: Context) =
    if (getDateMode(accountType, prefHandler) == DateMode.DATE_TIME) {
        DateFormat.getTimeFormat(context) as SimpleDateFormat
    } else null

val SimpleDateFormat.asDateTimeFormatter: DateTimeFormatter
    get() = DateTimeFormatter.ofPattern(this.toPattern())

fun dateTimeFormatter(account: PageAccount, prefHandler: PrefHandler, context: Context) =
    when (account.grouping) {
        Grouping.DAY -> timeFormatter(account.type, prefHandler, context)?.asDateTimeFormatter
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }

fun dateTimeFormatterLegacy(account: PageAccount, prefHandler: PrefHandler, context: Context) =
    when (account.grouping) {
        Grouping.DAY -> {
            timeFormatter(account.type, prefHandler, context)?.let {
                val is24HourFormat = DateFormat.is24HourFormat(context)
                it to if (is24HourFormat) 3f else 4.6f
            }
        }

        Grouping.MONTH ->
            if (prefHandler.monthStart == 1) {
                SimpleDateFormat("dd", Utils.localeFromContext(context)) to 2f
            } else {
                Utils.localizedYearLessDateFormat(context) to 3f
            }

        Grouping.WEEK -> SimpleDateFormat("EEE", Utils.localeFromContext(context)) to 2f
        Grouping.YEAR -> Utils.localizedYearLessDateFormat(context) to 3f
        Grouping.NONE -> Utils.ensureDateFormatWithShortYear(context) to 4.6f
    }

fun Spinner.checkNewAccountLimitation(prefHandler: PrefHandler, context: Context) {
    if (selectedItemId == 0L && !prefHandler.getBoolean(PrefKey.NEW_ACCOUNT_ENABLED, true)) {
        (selectedView as? TextView)?.let {
            it.error = ""
            it.setTextColor(Color.RED)
            it.text = ContribFeature.ACCOUNTS_UNLIMITED.buildTrialString(context)
        }
    }
}

fun FloatingActionButton.setBackgroundTintList(color: Int) {
    backgroundTintList = ColorStateList.valueOf(color)
    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(getBestForeground(color)))
}

fun View.configurePopupAnchor(
    infoText: CharSequence,
) {
    setOnClickListener {
        val host =
            context.getActivity() ?: throw java.lang.IllegalStateException("BaseActivity expected")
        host.hideKeyboard()
        //noinspection InflateParams
        val infoTextView = LayoutInflater.from(host).inflate(R.layout.textview_info, null)
                as TextView
        PopupWindow(infoTextView).apply {
            isOutsideTouchable = true
            isFocusable = true
            //without setting background drawable, popup does not close on back button or touch outside, on older API levels
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT

            infoTextView.text = infoText
            infoTextView.movementMethod = LinkMovementMethod.getInstance()
            showAsDropDown(this@configurePopupAnchor)
        }
    }
}

fun ImageView.setAttachmentInfo(info: AttachmentInfo) {
    if (info.thumbnail != null) {
        setImageBitmap(info.thumbnail)
    } else if (info.typeIcon != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setImageIcon(info.typeIcon)
        } else {
            CrashHandler.report(IllegalStateException())
        }
    } else {
        scaleType = ImageView.ScaleType.CENTER
        setImageResource(info.fallbackResource ?: 0)
    }
}

fun attachmentInfoMap(context: Context, withFile: Boolean = false): Map<Uri, AttachmentInfo> {
    val contentResolver = context.contentResolver
    return lazyMap { uri ->
        when (uri.scheme) {
            "content" -> {
                val file = if (withFile) try {
                    PictureDirHelper.getFileForUri(context, uri)
                } catch (e: IllegalArgumentException) {
                    null
                } else null
                contentResolver.getType(uri)?.let {
                    if (it.startsWith("image")) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val size = UiUtils.dp2Px(48f, context.resources)
                            val cancellationSignal = CancellationSignal()
                            try {
                                AttachmentInfo.of(
                                    it,
                                    contentResolver.loadThumbnail(
                                        uri,
                                        Size(size, size),
                                        cancellationSignal
                                    ), file
                                )
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            AttachmentInfo.of(it, R.drawable.ic_menu_camera, file)
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val icon = contentResolver.getTypeInfo(it).icon
                            AttachmentInfo.of(it, icon, file)
                        } else {
                            AttachmentInfo.of(it, R.drawable.ic_menu_template, file)
                        }
                    }
                }
            }

            "file" -> if (uri.pathSegments.first() == "android_asset") {
                val file = uri.pathSegments[1]
                AttachmentInfo.of(
                    getMimeType(file),
                    context.assets.open(file).use(BitmapFactory::decodeStream), null
                )
            } else null

            else -> null
        } ?: AttachmentInfo.of(null, com.google.android.material.R.drawable.mtrl_ic_error, null)
    }
}

tailrec fun Context.getActivity(): BaseActivity? = this as? BaseActivity
    ?: (this as? ContextWrapper)?.baseContext?.getActivity()

fun preferredTimePickerBuilder(context: Context) = MaterialTimePicker.Builder()
    .setTimeFormat(if (DateFormat.is24HourFormat(context)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
    .apply {
        context.injector.prefHandler().getInt(PrefKey.TIME_PICKER_INPUT_MODE, -1)
            .takeIf { it != -1 }
            ?.let { setInputMode(it) }
    }

fun preferredDatePickerBuilder(context: Context) = MaterialDatePicker.Builder.datePicker().apply {
    with(context.injector.prefHandler()) {
        weekStart?.let {
            setCalendarConstraints(
                CalendarConstraints.Builder().setFirstDayOfWeek(it).build()
            )
        }
        getInt(PrefKey.DATE_PICKER_INPUT_MODE, -1)
            .takeIf { it != -1 }
            ?.let { setInputMode(it) }
    }
}

fun Context.getAmountColor(sign: Int) =
    if (sign == 0) readPrimaryTextColor(this) else
        ContextCompat.getColor(
            this,
            if (sign == -1) R.color.colorExpense else R.color.colorIncome,
        )

data class DisplayProgress(val displayValue: Int, val displayExcess: Int)

fun DisplayProgress.forViewSystem(valueColor: Int, excessColor: Int) = listOf(
    DonutSection("excess", excessColor, displayExcess.toFloat()),
    DonutSection("progress", valueColor, displayValue.toFloat())
)

fun DisplayProgress.forCompose(
    valueColor: androidx.compose.ui.graphics.Color,
    excessColor: androidx.compose.ui.graphics.Color,
) = listOf(
    app.futured.donut.compose.data.DonutSection(displayExcess.toFloat(), excessColor),
    app.futured.donut.compose.data.DonutSection(displayValue.toFloat(), valueColor)
)

fun calcProgressVisualRepresentation(progress: Int) = when {

    progress > 200 -> DisplayProgress(0,100)

    progress > 100 -> DisplayProgress(200 - progress, progress - 100)

    progress >= 0 -> DisplayProgress(progress, 0)

    else -> throw IllegalArgumentException()
}