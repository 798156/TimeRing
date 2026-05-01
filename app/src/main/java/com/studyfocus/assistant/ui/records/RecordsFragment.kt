package com.studyfocus.assistant.ui.records

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyfocus.assistant.databinding.FragmentRecordsBinding
import com.studyfocus.assistant.databinding.ItemRecordBinding
import com.studyfocus.assistant.data.entity.FocusRecord
import com.studyfocus.assistant.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordsFragment : Fragment() {

    private var _binding: FragmentRecordsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    private val adapter = RecordAdapter { record -> showDeleteDialog(record) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvRecords.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecords.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allRecords.observe(viewLifecycleOwner) { records ->
                    adapter.submitList(records)
                    if (records.isEmpty()) {
                        binding.tvEmptyRecords.visibility = View.VISIBLE
                        binding.rvRecords.visibility = View.GONE
                    } else {
                        binding.tvEmptyRecords.visibility = View.GONE
                        binding.rvRecords.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun showDeleteDialog(record: FocusRecord) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除记录")
            .setMessage("确认删除「${record.subject}」的专注记录吗？")
            .setPositiveButton("确认") { _, _ ->
                viewModel.deleteRecord(record.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class RecordAdapter(
        private val onDelete: (FocusRecord) -> Unit
    ) : RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {

        private var records = listOf<FocusRecord>()

        fun submitList(list: List<FocusRecord>) {
            records = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
            val b = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return RecordViewHolder(b)
        }

        override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
            holder.bind(records[position])
        }

        override fun getItemCount(): Int = records.size

        inner class RecordViewHolder(private val b: ItemRecordBinding) :
            RecyclerView.ViewHolder(b.root) {

            fun bind(record: FocusRecord) {
                b.tvSubject.text = record.subject
                val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    .format(Date(record.timestamp))
                b.tvTime.text = dateStr

                b.tvDuration.text = if (record.durationMinutes >= 60) {
                    val h = record.durationMinutes / 60
                    val m = record.durationMinutes % 60
                    "${h}h${m}m"
                } else {
                    "${record.durationMinutes}分钟"
                }

                b.layoutDeleteRecord.setOnClickListener {
                    onDelete(record)
                }
            }
        }
    }
}
