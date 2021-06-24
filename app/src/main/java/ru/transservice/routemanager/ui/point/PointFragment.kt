package ru.transservice.routemanager.ui.point

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.AppConfig
import ru.transservice.routemanager.MainActivity
import ru.transservice.routemanager.R
import ru.transservice.routemanager.data.local.entities.FailureReasons
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.data.local.entities.PointStatuses
import ru.transservice.routemanager.databinding.FragmentPointBinding
import ru.transservice.routemanager.location.NavigationServiceConnection
import ru.transservice.routemanager.ui.task.TaskListViewModel
import java.util.*

class PointFragment : Fragment() {

    private var _binding: FragmentPointBinding? = null
    private val binding get() = _binding!!
    lateinit var navController: NavController
    private lateinit var viewModel: TaskListViewModel
    private val args: PointFragmentArgs by navArgs()
    private lateinit var pointStatus: PointStatuses

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "OnCreate")
        pointStatus = args.pointAction
        initViewModel(args.point)
        initFragmentFactDialogListener()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPointBinding.inflate(inflater,container,false)
        (requireActivity() as MainActivity).supportActionBar?.show()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(requireActivity(),R.id.nav_host_fragment)
        initButtonsActions()
        initLiveDataObservers()
    }

    companion object {

        private const val TAG = "${AppClass.TAG}: PointFragment"

        @JvmStatic
        fun newInstance() =
            PointFragment().apply {

            }
    }

    private fun initViewModel(pointItem: PointItem) {
        viewModel = ViewModelProvider(requireActivity(), TaskListViewModel.TaskListViewModelFactory(requireActivity().application)).get(TaskListViewModel::class.java)
        viewModel.initPointData()
    //viewModel = ViewModelProvider(this.requireActivity(), PointViewModel.PointViewModelFactory(pointItem)).get(PointViewModel::class.java)
        //viewModel.setViewModelData(pointItem)
    }

    private fun initViews(){
        with(binding) {
            val pointItem = viewModel.getCurrentPoint().value
            pointItem?.let {
                tvPointAddress.text = it.addressName
                tvAgentName.text = it.agentName
                tvContainerName.text = it.containerName
                tvContainerCount.text = it.countPlan.toString()
                tvCommentText.text = it.comment
                commentLayout.visibility = if (it.comment == "") View.GONE else View.VISIBLE
                tvCountFact.text = if (it.countFact == -1.0) "" else it.countFact.toString()
                reasonInput.visibility = View.GONE
                /*reasonSpinner.visibility = if (pointStatus == PointStatuses.CANNOT_DONE) View.VISIBLE else View.GONE
                btnSetFact.visibility = if (pointStatus == PointStatuses.CANNOT_DONE) View.GONE else View.VISIBLE
                btnPhotoAfter.visibility = if (pointStatus == PointStatuses.CANNOT_DONE) View.GONE else View.VISIBLE*/
                btnPointDone.text = if (pointStatus == PointStatuses.CANNOT_DONE) resources.getString(R.string.confirm_title) else resources.getString(R.string.done_title)

                layoutCantDone.visibility = if (pointStatus == PointStatuses.CANNOT_DONE) View.VISIBLE else View.GONE
                buttonsToDo.visibility = if (pointStatus == PointStatuses.CANNOT_DONE) View.GONE else View.VISIBLE

                btnSetFact.visibility = if (it.noEditFact) View.GONE else View.VISIBLE

                val reasonArray = AppConfig.FAILURE_REASONS

                val arrayAdapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        reasonArray
                )
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                arrayAdapter.setNotifyOnChange(true)
                reasonSpinner.adapter = arrayAdapter

                if (viewModel.reasonComment != "") {
                    if (reasonArray.contains(viewModel.reasonComment)) {
                        val spinnerPosition: Int =
                                (reasonSpinner.adapter as ArrayAdapter<String>).getPosition(viewModel.reasonComment)
                        reasonSpinner.setSelection(spinnerPosition)
                    } else {
                        val spinnerPosition: Int =
                                (reasonSpinner.adapter as ArrayAdapter<String>).getPosition(FailureReasons.OTHER.reasonTitle)
                        reasonSpinner.setSelection(spinnerPosition)
                        reasonInput.setText(viewModel.reasonComment)
                    }
                } else {
                    val spinnerPosition: Int =
                            (reasonSpinner.adapter as ArrayAdapter<String>).getPosition(FailureReasons.NO_GARBEGE.reasonTitle)
                    reasonSpinner.setSelection(spinnerPosition)
                    reasonInput.setText(FailureReasons.NO_GARBEGE.reasonTitle)
                }

                btnCall.visibility = if (viewModel.getPhoneNumber()=="") View.GONE else View.VISIBLE
            }
        }
    }

    private fun initButtonsActions(){
        with(binding){
            btnPhotoBefore.setOnClickListener {
                takePicture(PhotoOrder.PHOTO_BEFORE)
            }

            btnPhotoCantdone.setOnClickListener {
                takePicture(PhotoOrder.PHOTO_CANTDONE)
            }

            btnPhotoAfter.setOnClickListener {
                viewModel.getCurrentPoint().value?.let {
                    when {
                        it.countFact != -1.0 && viewModel.getFileBeforeIsDone().value!! -> takePicture(PhotoOrder.PHOTO_AFTER)
                        it.noEditFact && viewModel.getFileBeforeIsDone().value!! -> takePicture(PhotoOrder.PHOTO_AFTER)
                        else ->  Toast.makeText(
                                requireContext(),
                                "Предыдущие действия не выполнены",
                                Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            btnSetFact.setOnClickListener {
                viewModel.getCurrentPoint().value?.let{
                    if (viewModel.fileBeforeIsDone.value!! || it.noPhotoAllowed) {
                        val dialog = FactDialog.newInstance(viewModel.getCurrentPoint().value!!)
                        dialog.show(childFragmentManager, "factDialog")
                    } else {
                        Toast.makeText(requireContext(), "Нет фото до", Toast.LENGTH_LONG).show()
                    }
                }
            }

            ivDonePhotoBefore.setOnClickListener {
                navController.navigate(PointFragmentDirections.actionPointFragmentToPhotoListFragment(viewModel.getCurrentPoint().value!!,PhotoOrder.DONT_SET))
            }

            ivDonePhotoAfter.setOnClickListener {
                navController.navigate(PointFragmentDirections.actionPointFragmentToPhotoListFragment(viewModel.getCurrentPoint().value!!,PhotoOrder.DONT_SET))
            }

            ivDonePhotoCantdone.setOnClickListener {
                navController.navigate(PointFragmentDirections.actionPointFragmentToPhotoListFragment(viewModel.getCurrentPoint().value!!,PhotoOrder.PHOTO_CANTDONE))
            }

            btnPointDone.setOnClickListener {
                viewModel.getCurrentPoint().value?.let { pointItem ->
                    when {
                        pointItem.done -> {
                            //viewModel.uploadPointFiles()
                            requireActivity().onBackPressed()
                        }
                        viewModel.reasonComment != "" -> {
                            val resultPoint = viewModel.getCurrentPoint().value!!
                                .copy()
                                .also {
                                    it.reasonComment = viewModel.reasonComment
                                    if (it.timestamp == null){
                                        it.timestamp = Date()
                                    }
                                }
                            viewModel.updateCurrentPoint(resultPoint)
                            requireActivity().onBackPressed()
                        }
                        else -> Toast.makeText(requireContext(),"Точка не может считаться выполненной",Toast.LENGTH_LONG).show()
                    }
                }
            }


            val itemSelectedListener: AdapterView.OnItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // Получаем выбранный объект
                    val item = parent.getItemAtPosition(position) as String
                    if (item == FailureReasons.OTHER.reasonTitle) {
                        reasonInput.visibility = ViewGroup.VISIBLE
                    } else {
                        viewModel.getCurrentPoint().value?.let {
                            viewModel.reasonComment = item
                        }
                        reasonInput.visibility = ViewGroup.GONE
                        reasonInput.text?.clear()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            reasonSpinner.onItemSelectedListener = itemSelectedListener

            reasonInput.setOnEditorActionListener { v, actionId, event ->
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    viewModel.reasonComment = v.text.toString()
                    true
                } else {
                    false
                }
            }

            btnCall.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:" + viewModel.getPhoneNumber())
                }
                if (intent.resolveActivity(requireActivity().packageManager) != null){
                    startActivity(intent)
                }
            }

        }
    }


    private fun initLiveDataObservers(){

        viewModel.getFileAfterIsDone().observe(viewLifecycleOwner, Observer {
            binding.ivDonePhotoAfter.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        viewModel.getFileBeforeIsDone().observe(viewLifecycleOwner, Observer {
            binding.ivDonePhotoBefore.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        viewModel.getFileCantDoneIsDone().observe(viewLifecycleOwner, Observer {
            binding.ivDonePhotoCantdone.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        viewModel.getCurrentPoint().observe(viewLifecycleOwner, Observer {
            initViews()
        })

        viewModel.geoIsRequired.observe(viewLifecycleOwner, { required ->
            if (required) {
                NavigationServiceConnection.getLocation()?.let {
                    Log.d(
                        TAG,
                        "UPDATE LOCATION location successfully requested lat: ${it.latitude} lon: ${it.longitude}"
                    )
                    viewModel.setPointFilesGeodata(it)
                }
            }
        })

    }

    private fun takePicture(fileOrder: PhotoOrder) {
        viewModel.currentFileOrder = fileOrder
        navController.navigate(
            PointFragmentDirections.actionPointFragmentToCameraFragment(
                viewModel.getCurrentPoint().value!!,
                viewModel.currentFileOrder,
                pointStatus
            )
        )
    }

    private fun initFragmentFactDialogListener() {
        childFragmentManager.setFragmentResultListener("countFact",this) { requestKey, bundle ->
            val result = bundle.getDouble("countFactResult")
            viewModel.getCurrentPoint().value?.let {
                val resultPoint = viewModel.getCurrentPoint().value!!
                    .copy()
                    .also { it.countFact = result }

                if (result == 0.0){
                    resultPoint.done = true
                }else {
                    val valueBefore = resultPoint.done
                    resultPoint.done = viewModel.fileAfterIsDone.value ?: false
                    if (valueBefore != resultPoint.done) {
                        if (valueBefore) {
                            Toast.makeText(
                                activity,
                                "Снято выполнение с точки, для установки выполнения сделайте фото после",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(activity, "Точка выполнена", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                resultPoint.setCountOverFromPlanAndFact()
                resultPoint.timestamp = Date()
                viewModel.updateCurrentPoint(resultPoint)
            }

        }
    }


}