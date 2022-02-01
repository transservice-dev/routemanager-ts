package ru.skillbranch.skillarticles.extensions

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.fragment.app.Fragment

fun Context.dpToPx(dp: Int): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        this.resources.displayMetrics

    )
}

fun Fragment.dpToPx(dp: Int): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        requireContext().resources.displayMetrics

    )
}

fun View.dpToPx(dp: Int): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        context.resources.displayMetrics

    )
}

fun Context.dpToIntPx(dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        this.resources.displayMetrics
    ).toInt()
}

fun Fragment.dpToIntPx(dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        this.requireContext().resources.displayMetrics
    ).toInt()
}

fun View.dpToIntPx(dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        this.context.resources.displayMetrics
    ).toInt()
}

fun Context.hideKeyboard(view: View){
    val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken,0)
}

fun Context.attrValue(@AttrRes res: Int, needRes : Boolean = false) : Int {
    val value : Int?
    val tv = TypedValue()
    val resolveAttribute = this.theme.resolveAttribute(res, tv, true)
    if (resolveAttribute) value = if(needRes) tv.resourceId else tv.data
    else throw Resources.NotFoundException("Resource with id $res not found")
    return value
}

fun View.attrValue(@AttrRes res: Int, needRes : Boolean = false) : Int {
    val value : Int?
    val tv = TypedValue()
    val resolveAttribute = context.theme.resolveAttribute(res, tv, true)
    if (resolveAttribute) value = if(needRes) tv.resourceId else tv.data
    else throw Resources.NotFoundException("Resource with id $res not found")
    return value
}