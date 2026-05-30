package com.cxfcxf.mirror

import android.Manifest
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cxfcxf.mirror.service.AirPlayService
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cxfcxf.mirror.ui.MainScreen
import com.cxfcxf.mirror.ui.MirroringView
import com.cxfcxf.mirror.ui.TvSettingsScreen
import com.cxfcxf.mirror.ui.TvIdleScreen
import com.cxfcxf.mirror.ui.theme.AirPlayTheme
import com.cxfcxf.mirror.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun android.content.Context.isAndroidTv(): Boolean =
    packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var service: AirPlayService? = null
    val isInPip = mutableStateOf(false)
    private val logCallback: (String) -> Unit = { viewModel.addLog(it) }
    private val pinCallback: (String?) -> Unit = { viewModel.showPin(it) }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val svc = (binder as? AirPlayService.LocalBinder)?.service ?: return
            service = svc
            svc.logCallback = logCallback
            svc.pinCallback = pinCallback
            viewModel.bindService(svc)
            if (viewModel.autoStart.value && svc.serverState.value == AirPlayService.ServerState.STOPPED) {
                viewModel.startServer()
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            viewModel.unbindService()
        }
    }

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, service works either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        bindService(Intent(this, AirPlayService::class.java), connection, BIND_AUTO_CREATE)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    viewModel.updateFromService()
                    delay(200)
                }
            }
        }

        setContent {
            AirPlayTheme {
                if (isAndroidTv()) {
                    var showTvSettings by remember { mutableStateOf(false) }
                    val deviceName by viewModel.serverName.collectAsState()
                    val pin by viewModel.pinCode.collectAsState()
                    val serverState by viewModel.serverState.collectAsState()
                    val connectionCount by viewModel.connectionCount.collectAsState()
                    val videoAspect by viewModel.videoAspect.collectAsState()
                    val audioOnly by viewModel.audioOnly.collectAsState()

                    when {
                        showTvSettings -> {
                            BackHandler { showTvSettings = false }
                            TvSettingsScreen(viewModel)
                        }
                        connectionCount > 0 && !audioOnly -> {
                            var backConfirmPending by remember { mutableStateOf(false) }
                            val scope = rememberCoroutineScope()
                            BackHandler {
                                if (backConfirmPending) {
                                    viewModel.stopServer()
                                    moveTaskToBack(true)
                                } else {
                                    backConfirmPending = true
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Press Back again to exit",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    scope.launch {
                                        delay(2000)
                                        backConfirmPending = false
                                    }
                                }
                            }
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                            ) {
                                MirroringView(
                                    onSurfaceAvailable = { viewModel.onSurfaceAvailable(it) },
                                    onSurfaceDestroyed = { viewModel.onSurfaceDestroyed() },
                                    aspectRatio = videoAspect,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
                        }
                        else -> {
                            TvIdleScreen(
                                deviceName = deviceName,
                                pin = pin,
                                isRunning = serverState == AirPlayService.ServerState.RUNNING,
                                connectionCount = connectionCount,
                                onSettings = { showTvSettings = true },
                            )
                        }
                    }
                } else {
                    MainScreen(
                        viewModel = viewModel,
                        isInPip = isInPip.value,
                        onSurfaceAvailable = { viewModel.onSurfaceAvailable(it) },
                        onSurfaceDestroyed = { viewModel.onSurfaceDestroyed() },
                        onPip = { enterPip() }
                    )
                }
            }
        }
    }

    fun enterPip() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val aspect = viewModel.videoAspect.value
        val rational = Rational(
            (aspect * 1000).toInt().coerceIn(1, 2390),
            1000.coerceIn(1, 2390)
        )
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(rational)
            .build()
        enterPictureInPictureMode(params)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (viewModel.serverState.value == AirPlayService.ServerState.RUNNING &&
            viewModel.connectionCount.value > 0) {
            enterPip()
        }
    }

    override fun onPictureInPictureModeChanged(inPip: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(inPip, newConfig)
        isInPip.value = inPip
    }

    override fun onStart() {
        super.onStart()
        // Service already bound but server was stopped (e.g. double-back exit):
        // auto-restart so the app is usable when the user re-opens it.
        val svc = service ?: return
        if (viewModel.autoStart.value && svc.serverState.value == AirPlayService.ServerState.STOPPED) {
            viewModel.startServer()
        }
    }

    override fun onDestroy() {
        service?.let {
            if (it.logCallback === logCallback) it.logCallback = null
            if (it.pinCallback === pinCallback) it.pinCallback = null
        }
        unbindService(connection)
        super.onDestroy()
    }
}
