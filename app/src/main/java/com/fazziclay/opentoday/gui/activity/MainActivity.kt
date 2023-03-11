package com.fazziclay.opentoday.gui.activity

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.fazziclay.opentoday.R
import com.fazziclay.opentoday.app.ActivitySettings
import com.fazziclay.opentoday.app.App
import com.fazziclay.opentoday.app.FeatureFlag
import com.fazziclay.opentoday.app.Telemetry.UiClosedLPacket
import com.fazziclay.opentoday.app.Telemetry.UiOpenLPacket
import com.fazziclay.opentoday.app.receiver.ItemsTickReceiver
import com.fazziclay.opentoday.app.receiver.QuickNoteReceiver
import com.fazziclay.opentoday.app.updatechecker.UpdateChecker
import com.fazziclay.opentoday.databinding.ActivityMainBinding
import com.fazziclay.opentoday.databinding.NotificationDebugappBinding
import com.fazziclay.opentoday.databinding.NotificationUpdateAvailableBinding
import com.fazziclay.opentoday.gui.UI
import com.fazziclay.opentoday.gui.fragment.MainRootFragment
import com.fazziclay.opentoday.gui.interfaces.BackStackMember
import com.fazziclay.opentoday.util.InlineUtil.*
import com.fazziclay.opentoday.util.Logger
import com.fazziclay.opentoday.util.NetworkUtil
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val CONTAINER_ID = R.id.mainActivity_rootFragmentContainer
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var app: App
    private var lastExitClick: Long = 0

    // Current Date
    private lateinit var currentDateHandler: Handler
    private lateinit var currentDateRunnable: Runnable
    private lateinit var currentDateCalendar: GregorianCalendar
    private var activitySettings: ActivitySettings = ActivitySettings().setClockVisible(true).setNotificationsVisible(true)
    private var debugView = false
    private var debugViewSize = 13

    // Activity overrides
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.d(TAG, "onCreate", nullStat(savedInstanceState))
        app = App.get(this)
        UI.setTheme(app.settingsManager.theme)
        app.isAppInForeground = true
        app.telemetry.send(UiOpenLPacket())
        binding = ActivityMainBinding.inflate(layoutInflater)
        supportActionBar!!.hide()
        setContentView(binding.root)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(CONTAINER_ID, MainRootFragment.create(), "MainRootFragment")
                    .commit()
        }
        setupNotifications()
        setupCurrentDate()
        if (app.settingsManager.isQuickNoteNotification) {
            QuickNoteReceiver.sendQuickNoteNotification(this)
        }
        updateDebugView()
        onBackPressedDispatcher.addCallback(object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val exit = Runnable { this@MainActivity.finish() }
                val def = Runnable {
                    if (System.currentTimeMillis() - lastExitClick > 2000) {
                        Toast.makeText(this@MainActivity, R.string.exit_tab_2_count, Toast.LENGTH_SHORT).show()
                        lastExitClick = System.currentTimeMillis()
                    } else {
                        exit.run()
                    }
                }
                val fragment = supportFragmentManager.findFragmentById(CONTAINER_ID)
                if (fragment is BackStackMember) {
                    if (!fragment.popBackStack()) {
                        def.run()
                    }
                } else {
                    def.run()
                }
            }
        })
    }

    private fun setupNotifications() {
        setupAppDebugNotify()
        setupUpdateAvailableNotify()
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d(TAG, "onDestroy")
        app.isAppInForeground = false
        app.telemetry.send(UiClosedLPacket())
        currentDateHandler.removeCallbacks(currentDateRunnable)
    }

    // Current Date
    private fun setupCurrentDate() {
        currentDateCalendar = GregorianCalendar()
        setCurrentDate()
        currentDateHandler = Handler(mainLooper)
        currentDateRunnable = Runnable {
                if (isDestroyed) return@Runnable
                setCurrentDate()
                internalItemsTick()
                val millis = System.currentTimeMillis() % 1000
                currentDateHandler.postDelayed(currentDateRunnable, 1000 - millis)
        }
        currentDateHandler.post(currentDateRunnable)
        viewClick(binding.currentDateDate, Runnable {
            val dialog = DatePickerDialog(this)
            dialog.datePicker.firstDayOfWeek = app.settingsManager.firstDayOfWeek
            dialog.show()
        })
        viewClick(binding.currentDateTime, Runnable {
            val dialog = DatePickerDialog(this)
            dialog.datePicker.firstDayOfWeek = app.settingsManager.firstDayOfWeek
            dialog.show()
        })
    }

    private fun internalItemsTick() {
        if (!app.isFeatureFlag(FeatureFlag.DISABLE_AUTOMATIC_TICK)) {
            sendBroadcast(Intent(this@MainActivity, ItemsTickReceiver::class.java))
        }
    }

    private fun setCurrentDate() {
        currentDateCalendar.timeInMillis = System.currentTimeMillis()
        val time = currentDateCalendar.time

        // TODO: 11.10.2022 IDEA: Pattern to settings
        // Date
        var dateFormat = SimpleDateFormat("yyyy.MM.dd EEEE", Locale.getDefault())
        binding.currentDateDate.text = dateFormat.format(time)

        // Time
        dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        binding.currentDateTime.text = dateFormat.format(time)
    }

    // Update checker
    private fun setupUpdateAvailableNotify() {
        UpdateChecker.check(app) { available: Boolean, url: String? ->
            runOnUiThread {
                if (available) {
                    val updateAvailableLayout = NotificationUpdateAvailableBinding.inflate(layoutInflater)
                    binding.notifications.addView(updateAvailableLayout.root)
                    if (url != null) {
                        viewClick(updateAvailableLayout.root, Runnable { NetworkUtil.openBrowser(this@MainActivity, url) })
                    }
                }
            }
        }
    }

    // App is DEBUG warning notify
    private fun setupAppDebugNotify() {
        if (!App.DEBUG) return

        val b = NotificationDebugappBinding.inflate(layoutInflater)
        binding.notifications.addView(b.root)
    }

    fun toggleLogsOverlay() {
        debugView = !debugView
        updateDebugView()
    }

    private fun updateDebugView() {
        if (debugView) {
            binding.debugLogsSizeUp.visibility = View.VISIBLE
            binding.debugLogsSizeDown.visibility = View.VISIBLE
            binding.debugLogsSwitch.visibility = View.VISIBLE
            binding.debugLogsSwitch.setOnClickListener {
                viewVisible(binding.debugLogsScroll, binding.debugLogsSwitch.isChecked, View.GONE)
                binding.debugLogsText.text = Logger.getLOGS().toString()
            }
            binding.debugLogsText.textSize = debugViewSize.toFloat()
            binding.debugLogsSizeUp.setOnClickListener {
                debugViewSize++
                binding.debugLogsText.textSize = debugViewSize.toFloat()
            }
            binding.debugLogsSizeDown.setOnClickListener {
                debugViewSize--
                binding.debugLogsText.textSize = debugViewSize.toFloat()
            }
        } else {
            binding.debugLogsSizeUp.visibility = View.GONE
            binding.debugLogsSizeDown.visibility = View.GONE
            binding.debugLogsSwitch.visibility = View.GONE
            binding.debugLogsText.text = ""
        }
    }

    fun pushActivitySettings(a: ActivitySettings) {
        activitySettings = a
        updateByActivitySettings()
    }

    private fun updateByActivitySettings() {
        viewVisible(binding.currentDateDate, activitySettings.isClockVisible, View.GONE)
        viewVisible(binding.notifications, activitySettings.isNotificationsVisible, View.GONE)
    }
}