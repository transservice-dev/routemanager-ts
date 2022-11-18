package ru.transservice.routemanager.ui.defects

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import ru.transservice.routemanager.databinding.FragmentDefectsBinding
import ru.transservice.routemanager.extension.viewBinding

class DefectsFragment : DialogFragment() {

    private val binding: FragmentDefectsBinding by viewBinding (FragmentDefectsBinding::bind)
    private val viewModel: DefectsViewModel by viewModels()

    private fun confirm() {
        val defects = binding.fragmentDefectsText.text.toString()
        if (defects.isEmpty()) {
            Toast.makeText(requireActivity(),"Укажите отметки о неполадках", Toast.LENGTH_LONG).show()
            return
        }

        setFragmentResult(REQUEST_CODE, bundleOf(DEFECTS_RESULT to defects))
        dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentDefectsBinding.inflate(inflater,container,false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
        initViews()
    }

    private fun initViews() = with(binding) {
        fragmentDefectsConfirm.setOnClickListener {
            confirm()
        }
        fragmentDefectsCancel.setOnClickListener { dismiss() }
    }

    companion object {
        const val REQUEST_CODE = "defects_request"
        const val DEFECTS_RESULT = "defects_result"
    }
}