package ru.transservice.routemanager.ui.polygon

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import ru.transservice.routemanager.R
import ru.transservice.routemanager.databinding.DialogPolygonSelectionBinding
import ru.transservice.routemanager.ui.task.TaskListFragmentDirections

class PolygonSelectionDialog: DialogFragment() {

    private var _binding:DialogPolygonSelectionBinding? = null
    private val binding get() = _binding!!
    lateinit var navController: NavController
    private lateinit var viewModel: PolygonViewModel
    private val requestKeyPolygon = "polygonResult"

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogPolygonSelectionBinding.inflate(requireActivity().layoutInflater)
        val builder = AlertDialog.Builder(activity, R.style.ThemeOverlay_AppCompat_Dialog)
        builder.setView(binding.root)
            .setPositiveButton("ОК") {  _,_ ->
                viewModel.addNewPolygonToPointList()
                setFragmentResult(requestKeyPolygon, bundleOf())
            }
            .setNegativeButton("Отмена") { _,_ -> this.dismiss()}


        val dialog = builder.create()
        dialog.window?.setLayout(50, 100)

        return dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        viewModel.getPolygon().observe(viewLifecycleOwner,{
            binding.tvPolygon.text = it.name
        })
        binding.tvPolygon.setOnClickListener {
            navController.navigate(TaskListFragmentDirections.actionTaskListFragmentToPolygonListFragment(requestKeyPolygon))
        }
    }

    fun initViewModel(){
        viewModel  = ViewModelProvider(requireActivity(), PolygonViewModel.Factory()).get(
            PolygonViewModel::class.java)
        viewModel.setPolygonByDefault()
    }


    companion object {
        fun newInstance(): PolygonSelectionDialog {
            return PolygonSelectionDialog()
        }
    }
}