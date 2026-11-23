package ru.transservice.routemanager.ui.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import ru.transservice.routemanager.MainNavigationDirections
import ru.transservice.routemanager.R

private const val PERMISSIONS_REQUEST_CODE = 10
private val PERMISSIONS_REQUIRED = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.READ_EXTERNAL_STORAGE
)

class PermissionFragment : Fragment() {
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var allGranted = true
            permissions.entries.forEach {
                if (!it.value) allGranted = false
            }
            if (allGranted) {
                Toast.makeText(context, "Разрешения предоставлены", Toast.LENGTH_LONG)
                    .show()
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                    .navigate(
                        MainNavigationDirections.actionGlobalStartScreenFragment()
                    )
            } else {
                Toast.makeText(context, "Разрешения НЕ предоставлены", Toast.LENGTH_LONG)
                    .show()
            }

        }

    override fun onStart() {
        super.onStart()
        if (!hasPermissions(requireContext())) {
            requestPermissionLauncher.launch(PERMISSIONS_REQUIRED)
        } else {
            Navigation.findNavController(
                requireActivity(),
                R.id.nav_host_fragment).
            navigate(MainNavigationDirections.actionGlobalStartScreenFragment())
        }
    }

    companion object {
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}