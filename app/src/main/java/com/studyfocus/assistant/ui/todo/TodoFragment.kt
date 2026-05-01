package com.studyfocus.assistant.ui.todo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studyfocus.assistant.data.entity.TodoItem
import com.studyfocus.assistant.databinding.FragmentTodoBinding
import com.studyfocus.assistant.databinding.ItemTodoBinding
import com.studyfocus.assistant.viewmodel.TodoViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TodoFragment : Fragment() {

    private var _binding: FragmentTodoBinding? = null
    private val binding get() = _binding!!
    private val todoVM: TodoViewModel by activityViewModels()

    private val adapter = TodoAdapter(
        onToggle = { todoVM.toggleTodo(it) },
        onDelete = { todoVM.deleteTodo(it.id) },
        onSetDeadline = { showDeadlinePicker(it) }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvTodos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTodos.adapter = adapter
        binding.btnAddTodo.setOnClickListener { addTodo() }
        binding.etTodoInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { addTodo(); true } else false
        }
        observeData()
        todoVM.loadTodos()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                todoVM.todos.observe(viewLifecycleOwner) { todos ->
                    val sorted = todos.sortedBy { it.completed }
                    adapter.submitList(sorted)
                    val completed = todos.count { it.completed }
                    binding.tvCompletedCount.text = "${completed}/${todos.size}"
                    val progress = if (todos.isEmpty()) 0 else (completed * 100 / todos.size)
                    binding.progressBar.progress = progress
                    if (todos.isEmpty()) {
                        binding.tvTodoEmpty.visibility = View.VISIBLE
                        binding.rvTodos.visibility = View.GONE
                    } else {
                        binding.tvTodoEmpty.visibility = View.GONE
                        binding.rvTodos.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun showDeadlinePicker(todo: TodoItem) {
        val cal = Calendar.getInstance()
        if (todo.deadline != null) {
            cal.timeInMillis = todo.deadline!!
        } else {
            cal.add(Calendar.HOUR_OF_DAY, 1)
        }

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        val dpd = DatePickerDialog(requireContext(), { _, y, m, d ->
            val tpCal = Calendar.getInstance()
            tpCal.set(Calendar.YEAR, y)
            tpCal.set(Calendar.MONTH, m)
            tpCal.set(Calendar.DAY_OF_MONTH, d)
            tpCal.set(Calendar.HOUR_OF_DAY, hour)
            tpCal.set(Calendar.MINUTE, minute)

            TimePickerDialog(requireContext(), { _, h, min ->
                tpCal.set(Calendar.HOUR_OF_DAY, h)
                tpCal.set(Calendar.MINUTE, min)
                tpCal.set(Calendar.SECOND, 0)
                tpCal.set(Calendar.MILLISECOND, 0)
                todoVM.setDeadline(todo, tpCal.timeInMillis)
            }, hour, minute, true).show()

        }, year, month, day)
        dpd.setTitle("选择截止日期")
        dpd.show()
    }

    private fun addTodo() {
        val text = binding.etTodoInput.text.toString().trim()
        if (text.isEmpty()) return
        todoVM.addTodo(text)
        binding.etTodoInput.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class TodoAdapter(
        private val onToggle: (TodoItem) -> Unit,
        private val onDelete: (TodoItem) -> Unit,
        private val onSetDeadline: (TodoItem) -> Unit
    ) : ListAdapter<TodoItem, TodoAdapter.VH>(DiffCallback) {

        object DiffCallback : DiffUtil.ItemCallback<TodoItem>() {
            override fun areItemsTheSame(old: TodoItem, new: TodoItem): Boolean = old.id == new.id
            override fun areContentsTheSame(old: TodoItem, new: TodoItem): Boolean =
                old.title == new.title && old.completed == new.completed && old.deadline == new.deadline
        }

        private val shortFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(getItem(position))
        }

        inner class VH(private val b: ItemTodoBinding) : RecyclerView.ViewHolder(b.root) {
            private var currentItem: TodoItem? = null

            fun bind(item: TodoItem) {
                currentItem = item
                b.cbTodo.setOnCheckedChangeListener(null)
                b.cbTodo.isChecked = item.completed
                b.tvTodoTitle.text = item.title

                if (item.deadline != null) {
                    b.tvDeadline.visibility = View.VISIBLE
                    b.tvDeadline.text = "⏰ ${shortFormat.format(item.deadline)}"
                    b.tvDeadline.setTextColor(
                        if (item.deadline!! < System.currentTimeMillis() && !item.completed)
                            b.root.resources.getColor(android.R.color.holo_red_dark, null)
                        else b.tvDeadline.textColors.defaultColor
                    )
                } else {
                    b.tvDeadline.visibility = View.GONE
                }

                if (item.completed) {
                    b.tvTodoTitle.paintFlags = b.tvTodoTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    b.tvTodoTitle.alpha = 0.45f
                    b.root.alpha = 0.7f
                } else {
                    b.tvTodoTitle.paintFlags = b.tvTodoTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    b.tvTodoTitle.alpha = 1f
                    b.root.alpha = 1f
                }

                b.cbTodo.setOnCheckedChangeListener { _, _ -> currentItem?.let { onToggle(it) } }
                b.layoutDeleteBtn.setOnClickListener { currentItem?.let { onDelete(it) } }
                b.root.setOnLongClickListener { currentItem?.let { onSetDeadline(it) }; true }
            }
        }
    }
}
