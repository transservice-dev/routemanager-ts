package ru.transservice.routemanager.ui.splashscreen

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.transition.MaterialElevationScale
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.BaseFragment
import ru.transservice.routemanager.R
import ru.transservice.routemanager.databinding.FragmentSplashScreenBinding

class SplashScreenFragment : BaseFragment() {

    private var _binding: FragmentSplashScreenBinding? = null
    private val binding get() = _binding!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        exitTransition = MaterialElevationScale(false).setDuration(1000L)
        reenterTransition = MaterialElevationScale(true).setDuration(1000L)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashScreenBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun handleExit() {
        //Block back pressed
        Log.d(AppClass.TAG, "Back button press was blocked")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val animation = ValueAnimator.ofObject(ArgbEvaluator(), Color.WHITE, Color.RED)
        animation.repeatMode = ObjectAnimator.REVERSE
        animation.repeatCount = ObjectAnimator.INFINITE
        animation.duration = 2000
        animation.addUpdateListener {
            //FIXME ??? using findViewById because of binding is null after onDestroyView
            val bgView = view.findViewById<ImageView>(R.id.iv_splash_bg)
            bgView.setColorFilter(animation.animatedValue as Int, PorterDuff.Mode.MULTIPLY)
        }
        animation.start()

        val animationTextView= ValueAnimator.ofObject(ArgbEvaluator(), Color.BLACK, Color.RED)
        animationTextView.repeatMode = ObjectAnimator.REVERSE
        animationTextView.repeatCount = ObjectAnimator.INFINITE
        animationTextView.duration = 2000
        animationTextView.addUpdateListener {
            val textView = view.findViewById<TextView>(R.id.tv_splash_title)
            textView.setTextColor(animationTextView.animatedValue as Int)
        }
        animationTextView.start()

    }


    companion object {

        @JvmStatic
        fun newInstance() =
            SplashScreenFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }
}