package ru.transservice.routemanager.ui.point

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import ru.transservice.routemanager.AppClass
import ru.transservice.routemanager.AppConfig
import ru.transservice.routemanager.MainActivity
import ru.transservice.routemanager.R
import ru.transservice.routemanager.data.local.entities.*
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
    private var lastPointDoneStatus: Boolean = false

    private val requestKeyPolygon = "polygonForPoint"

    private val callbackExit = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            checkForCompletion(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "OnCreate")
        pointStatus = args.pointAction
        initViewModel()
        initFragmentFactDialogListener()
        initPolygonSelectionListener()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPointBinding.inflate(inflater,container,false)
        (requireActivity() as MainActivity).supportActionBar?.show()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(this, callbackExit)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, callbackExit)
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



    private fun initViewModel() {
        viewModel = ViewModelProvider(requireActivity(), TaskListViewModel.TaskListViewModelFactory(requireActivity().application)).get(TaskListViewModel::class.java)
        viewModel.initPointData()
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
                tvCountFact.text = if (it.countFact == -1.0) "" else it.countFact.toString()
                tvPolygon.text = if (it.isPolygonEmpty()) "" else it.polygonName

                swPointStatus.isEnabled = !pointItem.done

                swPointStatus.isChecked = pointStatus != PointStatuses.CANNOT_DONE
                swPointStatus.setOnCheckedChangeListener { _, isChecked ->
                    pointStatus = when {
                        isChecked && it.done -> PointStatuses.DONE
                        isChecked && !it.done -> PointStatuses.NOT_VISITED
                        else -> PointStatuses.CANNOT_DONE
                    }
                    setViewsVisibility(it)
                }

                setViewsVisibility(it)

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
                            (reasonSpinner.adapter as ArrayAdapter<String>).getPosition(FailureReasons.EMPTY_VALUE.reasonTitle)
                    reasonSpinner.setSelection(spinnerPosition)
                    reasonInput.setText(FailureReasons.EMPTY_VALUE.reasonTitle)
                }

                btnCall.visibility = if (viewModel.getPhoneNumber()=="") View.GONE else View.VISIBLE
            }
        }
    }

    private fun setViewsVisibility(point: PointItem) {
        with(binding) {

            val cannotDone = pointStatus == PointStatuses.CANNOT_DONE
            layoutCantDone.visibility = View.VISIBLE
            layoutTakePhotoCantDone.visibility = if (cannotDone) View.VISIBLE else View.GONE
            reasonLayout.visibility = if (cannotDone || point.countFact == 0.0) View.VISIBLE else View.GONE
            btnsMainAction.visibility = if (cannotDone) View.GONE else View.VISIBLE
            tvPolygon.visibility = if (cannotDone || !point.polygonByRow) View.GONE else View.VISIBLE
            commentLayout.visibility = if (point.comment == "") View.GONE else View.VISIBLE
            btnPointDone.text = if (cannotDone || point.countFact == 0.0) resources.getString(R.string.confirm_title) else resources.getString(R.string.done_title)
            //reasonInput.visibility = View.GONE
            btnSetFact.visibility = if (point.noEditFact) View.GONE else View.VISIBLE

            if (point.done) {
                ivPointStatus.setImageResource(R.drawable.ic_check_24_small)
                ivPointStatus.setColorFilter(Color.GREEN)
                ivPointStatus.visibility = View.VISIBLE

            }else if (point.reasonComment != "" || point.countFact == 0.0) {
                ivPointStatus.setImageResource(R.drawable.ic_block_24_small)
                ivPointStatus.visibility = View.VISIBLE
                ivPointStatus.setColorFilter(Color.RED)
            }else{
                ivPointStatus.visibility = View.GONE
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

            tvPolygon.setOnClickListener {
                viewModel.getCurrentPoint().value?.let {
                    if (it.countFact != -1.0 && viewModel.getFileBeforeIsDone().value!! && viewModel.getFileAfterIsDone().value!!) {
                        navController.navigate(PointFragmentDirections.actionPointFragmentToPolygonListFragment(requestKeyPolygon))
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Предыдущие действия не выполнены",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            btnViewPhotoBefore.setOnClickListener {
                navController.navigate(PointFragmentDirections.actionPointFragmentToPhotoListFragment(viewModel.getCurrentPoint().value!!,PhotoOrder.DONT_SET))
            }

            btnViewPhotoAfter.setOnClickListener {
                navController.navigate(PointFragmentDirections.actionPointFragmentToPhotoListFragment(viewModel.getCurrentPoint().value!!,PhotoOrder.DONT_SET))
            }

            ivDonePhotoCantdone.setOnClickListener {
                navController.navigate(PointFragmentDirections.actionPointFragmentToPhotoListFragment(viewModel.getCurrentPoint().value!!,PhotoOrder.DONT_SET))
            }

            btnPointDone.setOnClickListener {
                checkForCompletion()
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

            reasonInput.setOnEditorActionListener { v, actionId, _ ->
                if(actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_UNSPECIFIED){   //Добавил неопределенное событие для контроля некоторых телефонов
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
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(requireContext(),R.string.application_not_found,Toast.LENGTH_LONG).show()
                }
                /*if (intent.resolveActivity(requireActivity().packageManager) != null){
                    startActivity(intent)
                }*/
            }

        }
    }


    private fun initLiveDataObservers(){

        viewModel.getFileAfterIsDone().observe(viewLifecycleOwner, {
            binding.btnViewPhotoAfter.visibility = if (it == true) View.VISIBLE else View.INVISIBLE
        })

        viewModel.getFileBeforeIsDone().observe(viewLifecycleOwner,  {
            with(binding){
                btnViewPhotoBefore.visibility = if (it == true) View.VISIBLE else View.INVISIBLE
                ivDonePhotoCantdone.visibility = if (it == true) View.VISIBLE else View.GONE
                btnSetFact.isEnabled = it

                reasonSpinner.isEnabled = (it || ivDonePhotoCantdone.isVisible)
                reasonInput.isEnabled = (it || ivDonePhotoCantdone.isVisible)
            }
        })

        viewModel.getFileCantDoneIsDone().observe(viewLifecycleOwner,  {
            with(binding){
                ivDonePhotoCantdone.visibility = if (it == true || btnViewPhotoBefore.isVisible) View.VISIBLE else View.GONE
                reasonSpinner.isEnabled = (it || btnViewPhotoBefore.isVisible)
                reasonInput.isEnabled = (it || btnViewPhotoBefore.isVisible)
            }

        })

        viewModel.getCurrentPoint().observe(viewLifecycleOwner,  {
            if (lastPointDoneStatus != it.done) {
                val msg = if (it.done) "Точка выполнена!" else "Точка НЕ выполнена!"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG)
                        .show()
            }
            lastPointDoneStatus = it.done
            binding.btnPhotoAfter.isEnabled = it.countFact > 0.0
            if (it.status != PointStatuses.NOT_VISITED)
                pointStatus = it.status
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
            viewModel.setFactForPoint(result)
        }
    }

    private fun initPolygonSelectionListener() {
        // for Fragment (edit polygon)
        setFragmentResultListener(
            requestKeyPolygon
        ) { _, bundle ->
            val polygon = bundle.get("polygon") as PolygonItem
            viewModel.setPolygonForPoint(polygon)
        }
    }



    fun checkForCompletion(goBack: Boolean = false) {
        viewModel.getCurrentPoint().value?.let { pointItem ->
            when {
                pointStatus == PointStatuses.DONE || pointStatus == PointStatuses.NOT_VISITED-> {
                    when {
                        pointItem.done ->  navController.navigate(PointFragmentDirections.actionPointFragmentToTaskListFragment())
                        else -> Toast.makeText(requireContext(),"Точка не может считаться выполненной",Toast.LENGTH_LONG).show()
                    }
                }
                pointStatus == PointStatuses.CANNOT_DONE -> {
                    when {
                        viewModel.reasonComment == FailureReasons.EMPTY_VALUE.reasonTitle
                                && viewModel.getFileBeforeIsDone().value ?: false -> {
                            val snackMsg = Snackbar.make(
                                binding.root,
                                "Вы не указали причину невывоза!",
                                Snackbar.LENGTH_LONG
                            )
                            val snackTextView = snackMsg.view.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
                            snackTextView.maxLines = 10
                            snackMsg.show()
                        }
                        viewModel.reasonComment != "" -> {
                            viewModel.updateUndonePoint()
                            navController.navigate(PointFragmentDirections.actionPointFragmentToTaskListFragment())
                        }
                    }

                }
                goBack -> navController.navigate(PointFragmentDirections.actionPointFragmentToTaskListFragment())
                //else -> navController.navigate(PointFragmentDirections.actionPointFragmentToTaskListFragment())
                //TODO testing
            }
        }
    }
}