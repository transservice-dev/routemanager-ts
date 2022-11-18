package ru.transservice.routemanager.ui.startscreen

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.work.*
import ru.transservice.routemanager.*
import ru.transservice.routemanager.R
import ru.transservice.routemanager.animation.AnimateView
import ru.transservice.routemanager.data.local.entities.PhotoOrder
import ru.transservice.routemanager.data.local.entities.SearchType
import ru.transservice.routemanager.databinding.FragmentStartScreenBinding
import ru.transservice.routemanager.extensions.WorkInfoKeys
import ru.transservice.routemanager.extensions.shortFormat
import ru.transservice.routemanager.extensions.tag
import ru.transservice.routemanager.service.ErrorAlert
import ru.transservice.routemanager.service.LoadResult
import ru.transservice.routemanager.service.errorDescription
import ru.transservice.routemanager.ui.defects.DefectsFragment

class StartScreenFragment : BaseFragment() {

    private var _binding: FragmentStartScreenBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StartScreenViewModel by viewModels()
    private var isBtnCloseHidden = true
    private var backPressedTime:Long = 0
    private var progressDialog: AlertDialog? = null

    override fun handleExit() {
        val backToast = Toast.makeText(AppClass.appliactionContext(), "Нажмите еще раз для выхода из приложения.", Toast.LENGTH_LONG)
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            backToast.cancel()
            root.moveTaskToBack(true)
            root.finish()
        } else {
            backToast.show()
        }
        backPressedTime = System.currentTimeMillis()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().installSplashScreen().apply {
            setKeepOnScreenCondition {
                viewModel.isLoading.value
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStartScreenBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        initActionButtons()
        viewModel.getTaskParams().observe(viewLifecycleOwner, { state ->
            updateRouteInfo(true)
        })

        viewModel.getUploadingIsNotFinished().observe(viewLifecycleOwner) { result ->
            if (result) {
                showAndObserveUploadProgress()
            }
        }

        setDefectsInputListener()
    }

    private fun initViews(){
        updateRouteInfo(false)
        with(binding){
            closeLayout.visibility = View.GONE
            tvVersion.text = "Версия ${viewModel.version}"
        }
     }

    private fun updateRouteInfo(animate: Boolean){
        viewModel.getTaskParams().value?.let { state ->
            with(binding) {
                routeGroup.visibility = if (state.isLoaded) View.VISIBLE else View.GONE
                if (state.isLoaded) {
                    btnLoad.setImageResource(R.drawable.ic_replay_24)
                }else{
                    btnLoad.setImageResource(R.drawable.ic_add_24)
                }
                atAllCount.text = state.taskCountPoint.toString()
                doneCount.text = state.taskCountPointDone.toString()

                dateOfRoute.text = state.task.routeDate.shortFormat()
                vehicleNumber.text = if (state.task.search_type == SearchType.BY_VEHICLE) state.task.vehicle?.number ?: "" else state.task.route?.name ?: ""

            }
        }
        //TODO Animation
        /*val routeIsLoaded = taskState?.task?.docUid != ""
        if (routeIsLoaded) {
            /*if (animate){
                val animation = AnimateView(binding.routeGroup, requireContext(), animate)
                animation.showHeight()
            }else{
               binding.routeGroup.visibility = View.VISIBLE
            }*/
            binding.routeGroup.visibility = View.VISIBLE
            binding.btnLoad.setImageResource(R.drawable.ic_replay_24)
        }else{
            /*if (animate) {
                val animation = AnimateView(binding.routeGroup, requireContext(), animate)
                animation.hideHeight()
            }else{
                binding.routeGroup.visibility = View.GONE
            }*/
            binding.routeGroup.visibility = View.GONE
            binding.btnLoad.setImageResource(R.drawable.ic_add_24)
        }*/
    }

    private fun showHideButtonClose(){
        with(binding.closeLayout){
            val animateView = this.context?.let {
                    context -> AnimateView(this, context, true)
            }
            if (!isBtnCloseHidden) {
                animateView!!.hideHeight()
            }else{
                animateView!!.showHeight()
            }

        }

        with(binding.imageOpenCloseRoute) {
            val animateView = this.context?.let {
                    context -> AnimateView(this, context, true)
            }

            if (!isBtnCloseHidden) {
                animateView!!.rotateBack()
            }else{
                animateView!!.rotate()
            }
        }

        isBtnCloseHidden = !isBtnCloseHidden
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun initActionButtons(){
        binding.photoLayout.setOnClickListener {
            navController.navigate(MainNavigationDirections.actionGlobalPhotoListFragment(null,PhotoOrder.DONT_SET))
        }

        binding.vehicleLayout.setOnClickListener {
            navController.navigate(StartScreenFragmentDirections.actionStartScreenFragmentToRouteSettingsFragment())
        }

        binding.routeGroup.setOnClickListener {
            navController.navigate(StartScreenFragmentDirections.actionStartScreenFragmentToTaskListFragment())
        }

        binding.btnLoad.setOnClickListener{
            syncTask()
        }

        binding.imageOpenCloseRoute.setOnClickListener {
            showHideButtonClose()
        }

        binding.btnFinishRoute.setOnClickListener {
           handleFinishRoute()
        }

    }

    private fun syncTask() {
        viewModel.syncTaskData().observe(viewLifecycleOwner, {
            when (it) {
                is LoadResult.Loading -> {
                    (requireActivity() as MainActivity).swipeLayout.isRefreshing = true
                }
                is LoadResult.Success -> {
                    (requireActivity() as MainActivity).swipeLayout.isRefreshing = false
                    Toast.makeText(context, "Добавлено новых строк: ${it.data} ", Toast.LENGTH_LONG)
                        .show()
                    //Snackbar.make(binding.root,"Добавлено новых строк: ${it.data} ",Snackbar.LENGTH_LONG).show()
                    /*viewModel.getTaskParams().value?.let { task->
                            binding.atAllCount.text = task.taskCountPoint.toString()
                        }*/
                }
                is LoadResult.Error -> {
                    (requireActivity() as MainActivity).swipeLayout.isRefreshing = false
                    if (it.e != null) {
                        ErrorAlert.showAlert(
                            "${it.errorDescription()} Отправить отчет об ошибке?",
                            requireContext()
                        )
                    }
                    Toast.makeText(context, "Ошибка загрузки ${it.errorMessage}", Toast.LENGTH_LONG)
                        .show()
                    //Snackbar.make(binding.root,"Ошибка загрузки ${it.errorMessage}",Snackbar.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun handleFinishRoute() {
        val alertBuilder = AlertDialog.Builder(context).apply {
            setTitle("Подтвердите действие")
            setMessage("Вы уверены, что хотите завершить маршрут?")
            setPositiveButton("Да, завершить") { _,_ ->
                navController.navigate(StartScreenFragmentDirections.actionStartScreenFragmentToDefectsFragment(DefectsFragment.REQUEST_CODE))
            }
            setNegativeButton("Нет, отменить") { _,_ ->
                showHideButtonClose()
                Toast.makeText(context, "Действие отменено пользователем",Toast.LENGTH_SHORT).show()
            }
        }
        val alert = alertBuilder.create()
        alert.show()
    }

    private fun setDefectsInputListener() {
        setFragmentResultListener(DefectsFragment.REQUEST_CODE) { _, bundle ->
            bundle.getString(DefectsFragment.DEFECTS_RESULT)?.let {
                viewModel.finishRoute(it)
                showAndObserveUploadProgress()
            }
        }
    }

    private fun showAndObserveUploadProgress() {
        progressDialog = createProgressDialog()
        progressDialog?.show()
        observeUpload()
    }

    private fun createProgressDialog(): AlertDialog {
        val builder = AlertDialog.Builder(context)
        val inflater = requireActivity().layoutInflater
        builder
            .setView(inflater.inflate(R.layout.dialog_upload_progress,null))
            .setNegativeButton(R.string.cancel) {_,_ ->
                handleCancelUploadWork()
            }
            .setCancelable(false)
        return builder.create()
    }

    private fun observeUpload() {
        viewModel.getUploadWorkerId().removeObservers(viewLifecycleOwner)
        viewModel.getUploadWorkerId().observe(viewLifecycleOwner) { requestId ->
            requestId?.let {
                WorkManager.getInstance(AppClass.appliactionContext())
                    .getWorkInfoByIdLiveData(requestId)
                    .observe(requireActivity(), Observer { workInfo ->
                        workInfo?.let {
                            updateProgressDialog(it)
                        }
                    })
            }
        }
    }

    private fun updateProgressDialog(info: WorkInfo) {
        progressDialog?.window?.let { dialogWindow ->
            val progressDescription =
                dialogWindow.findViewById<TextView>(R.id.tv_progressDescription)
            val progressBar = dialogWindow.findViewById<ProgressBar>(R.id.progressBar)
            when (info.state) {
                WorkInfo.State.ENQUEUED -> {
                    val countAttempts = info.progress.getInt(WorkInfoKeys.CountAttempts,0)
                    progressBar.isIndeterminate = true
                    progressDescription.text = if (countAttempts == 0) getString(R.string.UploadAwaiting) else "Ожидание выгрузки. Попытка №$countAttempts"
                    setShimmerEffect(dialogWindow, false)
                }
                WorkInfo.State.RUNNING -> {
                    info.progress.getString(WorkInfoKeys.Description)?.let {
                        progressDescription.text = it
                    }
                    setShimmerEffect(dialogWindow, true)
                    val currentProgress = info.progress.getInt(WorkInfoKeys.Progress, 0)
                    Log.d(tag(), "Uploading result current progress $currentProgress ")
                    if (currentProgress != 0) {
                        progressBar.isIndeterminate = false
                        progressBar.progress = info.progress.getInt(WorkInfoKeys.Progress, 0)
                    }else {

                    }
                }
                WorkInfo.State.SUCCEEDED -> {
                    info.progress.getString(WorkInfoKeys.Description)?.let {
                        progressDescription.text = it
                        setShimmerEffect(dialogWindow, false)
                    }
                    progressBar.progress = info.progress.getInt(WorkInfoKeys.Progress, 100)
                    progressDialog?.dismiss()
                    progressDialog = null
                    Toast.makeText(context, "Данные выгружены успешно!", Toast.LENGTH_LONG).show()
                }
                else -> {
                    progressDialog?.dismiss()
                    progressDialog = null
                    info.outputData.keyValueMap[WorkInfoKeys.Error]?.let {
                        ErrorAlert.showAlert("$it Отправить отчет об ошибке?", requireContext())
                    }
                }
            }

        }
    }

    private fun setShimmerEffect(dialog: Window, turnOn: Boolean){
        val progressDescription =
            dialog.findViewById<TextView>(R.id.tv_progressDescription)
        val progressDescriptionShimmer =
            dialog.findViewById<TextView>(R.id.tv_progressDescription_shimmer)

        progressDescription.isVisible = !turnOn
        progressDescriptionShimmer.isVisible = turnOn
        progressDescriptionShimmer.text = progressDescription.text
    }

    private fun handleCancelUploadWork() {
        val alertBuilder = AlertDialog.Builder(context).apply {
            setTitle("Подтвердите действие")
            setMessage("Вы уверены, что хотите отменить выгрузку? Может произойти потеря данных.")
            setPositiveButton("Да") { _,_ ->
                cancelUploadWork()
                showHideButtonClose()
            }
            setNegativeButton("Нет") { _,_ ->
                viewModel.checkForIncompleteWork()
            }
        }
        val alert = alertBuilder.create()
        alert.show()
    }

    private fun cancelUploadWork() {
        viewModel.cancelUploadWorker()
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            StartScreenFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }

}