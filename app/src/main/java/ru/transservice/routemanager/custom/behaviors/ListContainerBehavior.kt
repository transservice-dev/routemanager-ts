package ru.transservice.routemanager.custom.behaviors

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.children
import com.google.android.material.appbar.AppBarLayout

class ListContainerBehavior() : AppBarLayout.ScrollingViewBehavior() {

    override fun onMeasureChild(
        parent: CoordinatorLayout,
        child: View,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
    ): Boolean {

        var innerChild = (child as CoordinatorLayout).children.first() as CoordinatorLayout
        if (innerChild.children.firstOrNull()?.isNestedScrollingEnabled?.not() == true) {
            /*val appbar = parent.children.find { it is AppBarLayout }
            val ah = appbar?.measuredHeight ?: 0
            val bottombar = parent.children.find { it is BottomNavigationView }
            val bh = if (bottombar?.isVisible == true) bottombar.measuredHeight else 0
            val size = View.MeasureSpec.getSize(parentHeightMeasureSpec)
            val height = size - ah - bh
            parent.onMeasureChild(
                child, parentWidthMeasureSpec, widthUsed,
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
                heightUsed
            )*/
        }

        return super.onMeasureChild(
            parent,
            child,
            parentWidthMeasureSpec,
            widthUsed,
            parentHeightMeasureSpec,
            heightUsed
        )
    }

}