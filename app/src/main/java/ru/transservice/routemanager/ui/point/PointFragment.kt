package ru.transservice.routemanager.ui.point

import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.muslimcompanion.utills.GPSTracker
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.AppConfig
import ru.transservice.routemanager.MainActivity
import ru.transservice.routemanager.R
import ru.transservice.routemanager.camera.PhotoFragment
import ru.transservice.routemanager.data.local.entities.FailureReasons
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.PointItem
import ru.transservice.routemanager.data.local.entities.PointStatuses
import ru.transservice.routemanager.databinding.FragmentPointBinding
import ru.transservice.routemanager.extensions.hideKeyboard
import ru.transservice.routemanager.ui.task.TaskListViewModel
import java.util.*

class PointFragment : Fragment() {

    private var _binding: FragmentPointBinding? = null
    private val binding get() = _binding!!
    lateinit var navController: NavController
    private lateinit var viewModel: TaskListViewModel
    private val args: PointFragmentArgs by navArgs()
    private lateinit var pointStatus: PointStatuses
    private var gps: GPSTracker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pointStatus = args.pointAction
        initViewModel(args.point)
        initFragmentFactDialogListener()
        navController = Navigation.findNavController(requireActivity(),R.id.nav_host_fragment)
        gps = GPSTracker.getGPSTracker(requireContext().applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        gps!!.stopUsingGPS()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initButtonsActions()
        initLiveDataObservers()
    }

    companion object {

        private const val TAG = "Route_Manager: PointFragment"

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
                reasonSpinner.visibility = if (pointStatus == PointStatuses.CANNOT_DONE) View.VISIBLE else View.GONE
                btnSetFact.visibility = if (pointStatus == PointStatuses.CANNOT_DONE) View.GONE else View.VISIBLE
                btnPhotoAfter.visibility = if (pointStatus == PointStatuses.CANNOT_DONE) View.GONE else View.VISIBLE
                btnPointDone.text = if (pointStatus == PointStatuses.CANNOT_DONE) resources.getString(R.string.confirm_title) else resources.getString(R.string.done_title)

                val reasonArray = AppConfig.FAILURE_REASONS

                val arrayAdapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        reasonArray
                )
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                arrayAdapter.setNotifyOnChange(true)
                reasonSpinner.adapter = arrayAdapter

                if (it.reasonComment != "") {
                    if (reasonArray.contains(it.reasonComment)) {
                        val spinnerPosition: Int =
                                (reasonSpinner.adapter as ArrayAdapter<String>).getPosition(it.reasonComment)
                        reasonSpinner.setSelection(spinnerPosition)
                    } else {
                        val spinnerPosition: Int =
                                (reasonSpinner.adapter as ArrayAdapter<String>).getPosition(FailureReasons.OTHER.reasonTitle)
                        reasonSpinner.setSelection(spinnerPosition)
                        reasonInput.setText(pointItem.reasonComment)
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
                takePicture(
                    if (pointStatus != PointStatuses.CANNOT_DONE) {
                        PhotoOrder.PHOTO_BEFORE
                    } else {
                        PhotoOrder.PHOTO_CANTDONE
                    }
                )
            }

            btnPhotoAfter.setOnClickListener {
                if (viewModel.getFileBeforeIsDone().value!!
                    && viewModel.getCurrentPoint().value!!.countFact != -1.0
                ) {
                    takePicture(PhotoOrder.PHOTO_AFTER)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Предыдущие действия не выполнены",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            btnSetFact.setOnClickListener {
                viewModel.getCurrentPoint().value?.let{
                    if (viewModel.fileBeforeIsDone.value!!) {
                        val dialog = FactDialog.newInstance(viewModel.getCurrentPoint().value!!)
                        dialog.show(childFragmentManager, "factDialog")
                    } else {
                        Toast.makeText(requireContext(), "Нет фото до", Toast.LENGTH_LONG).show()
                    }
                }
            }

            ivDonePhotoBefore.setOnClickListener {
                navController.navigate(PointFragmentDirections.actionPointFragmentToPhotoListFragment(viewModel.getCurrentPoint().value!!,PhotoOrder.PHOTO_BEFORE))
            }

            ivDonePhotoAfter.setOnClickListener {
                navController.navigate(PointFragmentDirections.actionPointFragmentToPhotoListFragment(viewModel.getCurrentPoint().value!!,PhotoOrder.PHOTO_AFTER))
            }

            btnPointDone.setOnClickListener {
                viewModel.getCurrentPoint().value?.let {
                    if (it.done || it.reasonComment != "") {
                        requireActivity().onBackPressed()
                    }else{
                        Toast.makeText(requireContext(),"Точка не может считаться выполненной",Toast.LENGTH_LONG).show()
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
                            val resultPoint = viewModel.getCurrentPoint().value!!
                                .copy()
                                .also { it.reasonComment = item }
                            viewModel.updateCurrentPoint(resultPoint)
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
                    val resultPoint = viewModel.getCurrentPoint().value!!
                        .copy()
                        .also { it.reasonComment = v.text.toString() }
                    viewModel.updateCurrentPoint(resultPoint)
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

        viewModel.getCurrentPoint().observe(viewLifecycleOwner, Observer {
            initViews()
        })

        viewModel.geoIsRequired.observe(viewLifecycleOwner, { required ->
            if (required) {
                gps?.let {
                    if (it.canGetLocation) {
                        Log.d(TAG, "UPDATE LOCATION location successfully requested lat: ${it.location?.latitude} lon: ${it.location?.longitude}")
                        it.location?.let { location ->
                            viewModel.setPointFilesGeodata(location)
                        }
                    }
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