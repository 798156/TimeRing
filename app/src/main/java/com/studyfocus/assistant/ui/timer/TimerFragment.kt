package com.studyfocus.assistant.ui.timer

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.studyfocus.assistant.R
import com.studyfocus.assistant.databinding.DialogStopTimerBinding
import com.studyfocus.assistant.databinding.FragmentTimerBinding
import com.studyfocus.assistant.viewmodel.MainViewModel
import java.util.Locale

class TimerFragment : Fragment() {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    private val handler = Handler(Looper.getMainLooper())
    private var elapsedSeconds = 0
    private var isRunning = false
    private var isPaused = false
    private var selectedSubject = "数学"

    private val presetSubjects = listOf("数学", "英语", "背单词", "专业课一", "专业课二")
    private var customSubjects = mutableListOf<String>()
    private val prefs by lazy { requireContext().getSharedPreferences("timer_subjects", Context.MODE_PRIVATE) }
    private val gson = Gson()

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                elapsedSeconds++
                updateTimerDisplay()
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCustomSubjects()
        setupSubjectSpinner()
        setupButtons()
        setupCustomSubject()

        binding.tvTimer.text = formatTime(0)
        binding.tvTimerStatus.text = ""
    }

    private fun loadCustomSubjects() {
        val json = prefs.getString("custom_subjects", null)
        if (json != null) {
            try {
                customSubjects = gson.fromJson(json, object : TypeToken<List<String>>() {}.type) ?: mutableListOf()
            } catch (_: Exception) { customSubjects = mutableListOf() }
        }
    }

    private fun saveCustomSubjects() {
        prefs.edit().putString("custom_subjects", gson.toJson(customSubjects)).apply()
    }

    private fun buildSpinnerList(): List<String> {
        val all = presetSubjects.toMutableList()
        all.addAll(customSubjects)
        return all + "＋ 自定义"
    }

    private fun setupSubjectSpinner() {
        refreshSpinner()
    }

    private fun refreshSpinner() {
        val list = buildSpinnerList()
        val adapter = object : ArrayAdapter<String>(
            requireContext(), android.R.layout.simple_spinner_item, list
        ) {
            override fun getCount(): Int = super.getCount()
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSubject.adapter = adapter

        val idx = list.indexOf(selectedSubject)
        if (idx >= 0) binding.spinnerSubject.setSelection(idx)

        binding.spinnerSubject.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val chosen = buildSpinnerList().getOrElse(position) { presetSubjects[0] }
                if (chosen == "＋ 自定义") {
                    binding.etCustomSubject.setText("")
                    binding.layoutCustomSubject.visibility = View.VISIBLE
                    binding.btnConfirmCustom.visibility = View.VISIBLE
                    binding.btnDeleteCustom.visibility = View.GONE
                    binding.etCustomSubject.requestFocus()
                } else if (customSubjects.contains(chosen)) {
                    binding.etCustomSubject.setText(chosen)
                    binding.layoutCustomSubject.visibility = View.VISIBLE
                    binding.btnConfirmCustom.visibility = View.GONE
                    binding.btnDeleteCustom.visibility = View.VISIBLE
                    selectedSubject = chosen
                } else {
                    binding.layoutCustomSubject.visibility = View.GONE
                    selectedSubject = chosen
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCustomSubject() {
        binding.btnConfirmCustom.setOnClickListener {
            val text = binding.etCustomSubject.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            if (!customSubjects.contains(text) && !presetSubjects.contains(text)) {
                customSubjects.add(text)
                saveCustomSubjects()
            }
            selectedSubject = text
            refreshSpinner()
            binding.etCustomSubject.text?.clear()
            binding.layoutCustomSubject.visibility = View.GONE
        }

        binding.btnDeleteCustom.setOnClickListener {
            val text = binding.etCustomSubject.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            customSubjects.remove(text)
            saveCustomSubjects()
            selectedSubject = presetSubjects[0]
            refreshSpinner()
            binding.etCustomSubject.text?.clear()
            binding.layoutCustomSubject.visibility = View.GONE
        }
    }

    private fun setupButtons() {
        binding.btnStart.setOnClickListener {
            if (!isRunning && !isPaused) startTimer()
            else if (isPaused) resumeTimer()
        }
        binding.btnPause.setOnClickListener { if (isRunning) pauseTimer() }
        binding.btnStop.setOnClickListener { if (isRunning || isPaused) stopTimer() }
    }

    private fun startTimer() {
        isRunning = true; isPaused = false; elapsedSeconds = 0
        updateTimerDisplay()
        handler.postDelayed(timerRunnable, 1000)
        binding.btnStart.text = "计时中"
        binding.tvTimerStatus.text = "专注中 · $selectedSubject"
        binding.tvTimerStatus.visibility = View.VISIBLE
    }

    private fun pauseTimer() {
        isRunning = false; isPaused = true
        binding.btnStart.text = getString(R.string.resume)
        binding.tvTimerStatus.text = "⏸ 已暂停"
        binding.tvTimerStatus.visibility = View.VISIBLE
    }

    private fun resumeTimer() {
        isRunning = true; isPaused = false
        handler.postDelayed(timerRunnable, 1000)
        binding.btnStart.text = "计时中"
        binding.tvTimerStatus.text = "专注中 · $selectedSubject"
        binding.tvTimerStatus.visibility = View.VISIBLE
    }

    private fun stopTimer() {
        isRunning = false; isPaused = false
        val finalSubject = selectedSubject
        val durationText = formatTime(elapsedSeconds)
        val roundedMinutes = Math.round(elapsedSeconds / 60.0).toInt()

        val dialogBinding = DialogStopTimerBinding.inflate(LayoutInflater.from(requireContext()), null, false)
        dialogBinding.tvDialogInfo.text = if (roundedMinutes < 1) {
            "科目：$finalSubject\n时长：$durationText\n\n不足1分钟，确定保存？"
        } else "科目：$finalSubject\n时长：$durationText"

        val dialog = AlertDialog.Builder(requireContext(), R.style.GlassDialogTheme)
            .setView(dialogBinding.root).create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_glass)

        dialogBinding.btnDialogSave.setOnClickListener {
            if (roundedMinutes >= 1) { viewModel.insertRecord(finalSubject, roundedMinutes); viewModel.refreshAll() }
            resetTimer(); dialog.dismiss()
        }
        dialogBinding.btnDialogCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun resetTimer() {
        handler.removeCallbacks(timerRunnable)
        elapsedSeconds = 0; updateTimerDisplay()
        binding.btnStart.text = getString(R.string.start)
        binding.tvTimerStatus.text = ""; binding.tvTimerStatus.visibility = View.GONE
    }

    private fun updateTimerDisplay() { binding.tvTimer.text = formatTime(elapsedSeconds) }

    private fun formatTime(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timerRunnable)
        _binding = null
    }
}
