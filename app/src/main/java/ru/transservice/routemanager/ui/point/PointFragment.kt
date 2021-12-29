package ru.transservice.routemanager.ui.point

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
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import ru.transservice.routemanager.*
import ru.transservice.routemanager.data.local.entities.*
import ru.transservice.routemanager.databinding.FragmentPointBinding
import ru.transservice.routemanager.extensions.navViewModelsFactory

class PointFragment : BaseFragment() {

    private var _binding: FragmentPointBinding? = null
    private val binding get() = _binding!!
    lateinit var navController: NavController
    private val args: PointFragmentArgs by navArgs()
    private val viewModel: PointItemViewModel by navViewModelsFactory(R.id.navPoint) { PointItemViewModel(args.point.lineUID) }
    private lateinit var pointStatus: PointStatuses
    private var lastPointDoneStatus: Boolean = false

    private val requestKeyPolygon = "polygonForPoint"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "OnCreate")
        pointStatus = PointStatuses.NOT_VISITED
        initFragmentFactDialogListener()
        initPolygonSelectionListener()
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

    override fun handleExit() {
        checkForCompletion(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(requireActivity(),R.id.nav_host_fragment)
        initButtonsActions()
        initLiveDataObservers()
    }

    private fun initViews(state: PointWithData){
        with(binding) {
            tvPointAddress.text = state.point.addressName
            tvAgentName.text = state.point.agentName
            tvContainerName.text = state.point.containerName
            tvContainerCount.text = state.point.countPlan.toString()
            tvCommentText.text = state.point.comment
            tvCountFact.text = if (state.point.countFact == -1.0) "" else state.point.countFact.toString()


            //TODO transfer polygon to PointItemState
            //tvPolygon.text = if (state.isPolygonEmpty()) "" else state.polygonName

            swPointStatus.isEnabled = !state.point.done

            swPointStatus.isChecked = pointStatus != PointStatuses.CANNOT_DONE
            swPointStatus.setOnCheckedChangeListener { _, isChecked ->
                pointStatus = when {
                    isChecked && state.point.done -> PointStatuses.DONE
                    isChecked && !state.point.done -> PointStatuses.NOT_VISITED
                    else -> PointStatuses.CANNOT_DONE
                }
                setViewsVisibility(state)
            }

            setViewsVisibility(state)

            val reasonArray = AppConfig.FAILURE_REASONS
            val arrayAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                reasonArray
            )
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            arrayAdapter.setNotifyOnChange(true)
            reasonSpinner.adapter = arrayAdapter

            //TODO viewModel comment
            /*if (viewModel.reasonComment != "") {
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

            btnCall.visibility = if (viewModel.getPhoneNumber() == "") View.GONE else View.VISIBLE*/
        }

    }

    private fun setViewsVisibility(state: PointWithData) {
        with(binding) {

            val cannotDone = pointStatus == PointStatuses.CANNOT_DONE
            layoutCantDone.visibility = View.VISIBLE
            layoutTakePhotoCantDone.visibility = if (cannotDone) View.VISIBLE else View.GONE
            reasonLayout.visibility = if (cannotDone || state.point.countFact == 0.0) View.VISIBLE else View.GONE
            btnsMainAction.visibility = if (cannotDone) View.GONE else View.VISIBLE
            tvPolygon.visibility = if (cannotDone || !state.point.polygonByRow) View.GONE else View.VISIBLE
            commentLayout.visibility = if (state.point.comment == "") View.GONE else View.VISIBLE
            btnPointDone.text = if (cannotDone || state.point.countFact == 0.0) resources.getString(R.string.confirm_title) else resources.getString(R.string.done_title)

            if (state.point.done) {
                ivPointStatus.setImageResource(R.drawable.ic_check_24_small)
                ivPointStatus.setColorFilter(Color.GREEN)
                ivPointStatus.visibility = View.VISIBLE

            }else if (state.point.reasonComment != "" || state.point.countFact == 0.0) {
                ivPointStatus.setImageResource(R.drawable.ic_block_24_small)
                ivPointStatus.visibility = View.VISIBLE
                ivPointStatus.setColorFilter(Color.RED)
            }else{
                ivPointStatus.visibility = View.GONE
            }

            btnViewPhotoAfter.visibility = if (state.countFilesAfter > 0) View.VISIBLE else View.INVISIBLE

            val isPhotoBeforeDone = state.countFilesBefore > 0
            btnViewPhotoBefore.visibility = if (isPhotoBeforeDone) View.VISIBLE else View.INVISIBLE
            ivDonePhotoCantdone.visibility = if (isPhotoBeforeDone) View.VISIBLE else View.GONE
            ivDonePhotoCantdone.visibility = if (isPhotoBeforeDone) View.VISIBLE else View.GONE

            btnSetFact.isEnabled = isPhotoBeforeDone
            btnPhotoAfter.isEnabled = isPhotoBeforeDone && state.point.countFact > 0
            tvPolygon.isEnabled = isPhotoBeforeDone && state.point.countFact > 0 && state.countFilesAfter > 0
            reasonSpinner.isEnabled = (isPhotoBeforeDone || ivDonePhotoCantdone.isVisible)
            reasonInput.isEnabled = (isPhotoBeforeDone || ivDonePhotoCantdone.isVisible)
            reasonSpinner.isEnabled = (isPhotoBeforeDone)
            reasonInput.isEnabled = (isPhotoBeforeDone)

        }
    }

    private fun initButtonsActions(){
        with(binding){
            btnPhotoBefore.setOnClickListener { takePicture(PhotoOrder.PHOTO_BEFORE) }
            btnPhotoCantdone.setOnClickListener { takePicture(PhotoOrder.PHOTO_CANTDONE) }
            btnPhotoAfter.setOnClickListener { takePicture(PhotoOrder.PHOTO_AFTER) }
            btnSetFact.setOnClickListener {
                val dialog = FactDialog.newInstance(args.point)
                dialog.show(childFragmentManager, "factDialog")
            }
            tvPolygon.setOnClickListener {
                navController.navigate(PointFragmentDirections.actionPointFragmentToPolygonListFragment(requestKeyPolygon))
            }
            btnViewPhotoBefore.setOnClickListener { navigateToPictures() }
            btnViewPhotoAfter.setOnClickListener { navigateToPictures() }
            ivDonePhotoCantdone.setOnClickListener { navigateToPictures() }
            btnPointDone.setOnClickListener { checkForCompletion() }

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
                        viewModel.state.value?.let {
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
            }

        }
    }

    private fun navigateToPictures(){
        viewModel.state.value?.let {
            navController.navigate(
                PointFragmentDirections.actionPointFragmentToPhotoListFragment(
                    it.point,
                    PhotoOrder.DONT_SET
                )
            )
        }
    }

    private fun initLiveDataObservers(){

        viewModel.state.observe(viewLifecycleOwner,{ state ->
            state?.let {
                if (lastPointDoneStatus != state.point.done) {
                    val msg = if (state.point.done) "Точка выполнена!" else "Точка НЕ выполнена!"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG)
                        .show()
                }
                initViews(state)
            }
        })

        /*viewModel.geoIsRequired.observe(viewLifecycleOwner, { required ->
            if (required) {
                NavigationServiceConnection.getLocation()?.let {
                    Log.d(
                        TAG,
                        "UPDATE LOCATION location successfully requested lat: ${it.latitude} lon: ${it.longitude}"
                    )
                    viewModel.setPointFilesGeodata(it)
                }
            }
        })*/

    }

    private fun takePicture(fileOrder: PhotoOrder) {
        viewModel.state.value?.let { state ->
            navController.navigate(
                PointFragmentDirections.actionPointFragmentToCameraFragment(
                    state.toPointFileParams(fileOrder)
                )
            )
        }

    }

    private fun initFragmentFactDialogListener() {
        childFragmentManager.setFragmentResultListener("countFact",this) { requestKey, bundle ->
            val result = bundle.getDouble("countFactResult")
            viewModel.setFact(result)
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
       viewModel.state.value?.let { pointState ->
            when {
                pointStatus == PointStatuses.DONE || pointStatus == PointStatuses.NOT_VISITED  && !goBack -> {
                    when {
                        pointState.point.done ->  navController.navigate(PointFragmentDirections.actionPointFragmentToTaskListFragment())
                        else -> Toast.makeText(requireContext(),"Точка не может считаться выполненной",Toast.LENGTH_LONG).show()
                    }
                }
                pointStatus == PointStatuses.CANNOT_DONE -> {
                    when {
                        viewModel.reasonComment == FailureReasons.EMPTY_VALUE.reasonTitle
                                && pointState.countFilesBefore > 0 -> {
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
                        else -> {}
                    }

                }
                goBack -> navController.popBackStack()
                else -> {}
            }
        }
    }

    companion object {
        private const val TAG = "${AppClass.TAG}: PointFragment"
        @JvmStatic
        fun newInstance() =
            PointFragment().apply {
            }
    }
}