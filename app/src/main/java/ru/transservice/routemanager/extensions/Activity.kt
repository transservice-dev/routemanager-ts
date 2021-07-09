package ru.transservice.routemanager.extensions

import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.view.inputmethod.InputMethodManager

fun Activity.hideKeyboard(){
    val imm: InputMethodManager = this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    //Find the currently focused view, so we can grab the correct window token from it.
    var view: View? = this.currentFocus
    //If no view currently has focus, create a new one, just so we can grab a window token from it
    if (view == null) {
        view = View(this)
    }
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}


fun Activity.showKeyboard(){
    val imm: InputMethodManager = this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    //Find the currently focused view, so we can grab the correct window token from it.
    var view: View? = this.currentFocus
    //If no view currently has focus, create a new one, just so we can grab a window token from it
    if (view == null) {
        view = View(this)
    }
    imm.showSoftInput(view,InputMethodManager.SHOW_FORCED)
    imm.toggleSoftInputFromWindow(view.windowToken,InputMethodManager.SHOW_FORCED, InputMethodManager.SHOW_IMPLICIT)
}

fun Activity.isKeyboardOpen():Boolean{
    return keyBoardIsOpen(this)
}

fun Activity.isKeyboardClosed():Boolean{
    return !keyBoardIsOpen(this)
}

private fun keyBoardIsOpen(activity: Activity): Boolean{
    val r = Rect()
    val contentView = activity.findViewById<View>(android.R.id.content)
    contentView.getWindowVisibleDisplayFrame(r)
    val screenHeight: Int = contentView.rootView.height
    // r.bottom is the position above soft keypad or device button.
    // if keypad is shown, the r.bottom is smaller than that before.
    val keypadHeight: Int = screenHeight - r.bottom
    return keypadHeight > screenHeight * 0.15
}