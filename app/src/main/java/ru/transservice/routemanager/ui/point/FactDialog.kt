package ru.transservice.routemanager.ui.point

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import ru.transservice.routemanager.R
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.databinding.FragmentFactDialogBinding


class FactDialog : DialogFragment() {

    private var _binding: FragmentFactDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var point: PointItem
    private var plan: Double = 0.0
    private var fact: Double = 0.0

    //var plan = point.value!!.getCountPlan()
    //private var fact : Double = if (point.value!!.getCountFact()==-1.0) {0.0} else {point.value!!.getCountFact()}

    companion object {
        fun newInstance(point: PointItem):FactDialog {
            val args = Bundle()
            args.putSerializable("point",point)
            val fragment = FactDialog()
            fragment.arguments = args
            return fragment
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentFactDialogBinding.inflate(requireActivity().layoutInflater)

        val args = arguments ?: return dialog!!
        point = args.getSerializable("point") as PointItem

        plan = point.countPlan
        fact = if (point.countFact ==-1.0) {point.countPlan} else {point.countFact}

        val tvfact = binding.factTextSet as TextView
        tvfact.text = fact.toString()

        binding.setHigh.setOnTouchListener(RepeatListener(400,100
        ) {
            fact += 0.5
            tvfact.text = fact.toString()
        })

        binding.setLow.setOnTouchListener(RepeatListener(400,100
        ) {
            if (fact >= 0.5){
                fact -= 0.5
                tvfact.text = fact.toString()
            }
        })

        val etFact = binding.factTextSet as EditText

        etFact.addTextChangedListener {
            try {
                fact = if (etFact.text.toString() != ""){
                    etFact.text.toString().toDouble()
                } else {
                    0.0
                }

            } catch (e : Exception) {
                Toast.makeText(requireContext(),"Факт введен некорректно",Toast.LENGTH_SHORT).show()
            }
        }

        val builder = AlertDialog.Builder(activity, R.style.ThemeOverlay_AppCompat_Dialog)
        builder.setView(binding.root)
            .setPositiveButton("ОК") {  _,_ ->
                setFragmentResult("countFact", bundleOf("countFactResult" to fact))}
            .setNegativeButton("Отмена") { _,_ -> this.dismiss()}


        val dialog = builder.create()
        dialog.window?.setLayout(50, 100)

        return dialog
    }

    class RepeatListener(
        private val initialInterval: Long, private val normalInterval: Long,
        val clickListener: View.OnClickListener
    ) : OnTouchListener {
        private val handler: Handler = Handler()
        private var touchedView: View? = null
        private var wasLongClick : Boolean = false

        private val handlerRunnable: Runnable = object : Runnable {
            override fun run() {
                if (touchedView!!.isEnabled) {
                    handler.postDelayed(this, normalInterval)
                    clickListener.onClick(touchedView)
                    wasLongClick = true

                } else {
                    // if the view was disabled by the clickListener, remove the callback
                    handler.removeCallbacks(this)
                    touchedView!!.isPressed = false
                    touchedView = null
                }
            }
        }

        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    handler.removeCallbacks(handlerRunnable)
                    handler.postDelayed(handlerRunnable, initialInterval)
                    touchedView = view
                    touchedView!!.isPressed = true
                    clickListener.onClick(view)
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(handlerRunnable)
                    touchedView!!.isPressed = false
                    touchedView = null
                    return true
                }
            }
            return false
        }

        init {
            require(!(initialInterval < 0 || normalInterval < 0)) { "negative interval" }

        }
    }

}

