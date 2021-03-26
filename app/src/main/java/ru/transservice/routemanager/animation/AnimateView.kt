package ru.transservice.routemanager.animation

import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.children
import ru.transservice.routemanager.R

class AnimateView (var view : View, var context : Context, val animate : Boolean){

    fun hideHeight (){
        if(view is ConstraintLayout) {
            val animator = ObjectAnimator.ofPropertyValuesHolder(
                view,
                PropertyValuesHolder.ofFloat(View.SCALE_Y,0F)
            ).apply {
               doOnEnd { view.visibility = View.GONE }
            }
            if(!animate){
                animator.duration = 0
            }
            animator.start()

        }else if (view is Guideline){
            val layoutTransition = (view.parent as ConstraintLayout).layoutTransition
            layoutTransition.setDuration(300) // Change duration
            layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
            val params = (view as Guideline).layoutParams as ConstraintLayout.LayoutParams
            params.guidePercent = 1F
            view.layoutParams = params
            view.visibility = View.GONE
            view.requestLayout()

        }

    }

    fun showHeight(){

        if(view is ConstraintLayout) {
            val animator = ObjectAnimator.ofPropertyValuesHolder(
                view,
                PropertyValuesHolder.ofFloat(View.SCALE_Y,1F)
            ).apply {
                doOnStart { view.visibility = View.VISIBLE }
            }
            if(!animate){
                animator.duration = 0
            }
            animator.start()
        }else if (view is Guideline){
            val layoutTransition = (view.parent as ConstraintLayout).layoutTransition
            layoutTransition.setDuration(300) // Change duration
            layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
            val params = (view as Guideline).layoutParams as ConstraintLayout.LayoutParams
            params.guidePercent = 0.9F
            view.layoutParams = params
            view.visibility = View.VISIBLE
            view.requestLayout()
        }
        /*else{
            val show = AnimationUtils.loadAnimation(context,R.anim.hide_height)
            view.startAnimation(show)
            view.visibility = ViewGroup.VISIBLE
        }*/
    }

    fun rotate (){
        val rotate = AnimationUtils.loadAnimation(this.context, R.anim.routate_pict)
        view.startAnimation(rotate)
    }

    fun rotateBack (){
        val rotate = AnimationUtils.loadAnimation(this.context, R.anim.rotate_pict_back)
        view.startAnimation(rotate)
    }

    private fun setHeightAndVisibility(set:ConstraintSet, changeView: ConstraintLayout, height:Int, visibility:Int){
        for (child in changeView.children){
            if(child is ConstraintLayout ){
                setHeightAndVisibility(set,child,height,visibility)
            }
            set.constrainHeight(child.id,height)
            set.setVisibility(child.id,visibility)

        }
    }
}