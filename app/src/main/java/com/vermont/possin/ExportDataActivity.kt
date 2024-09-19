package com.vermont.possin

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.vermont.possin.database.AppDatabase
import com.vermont.possin.viewmodel.TransactionViewModel
import com.vermont.possin.viewmodel.TransactionViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExportDataActivity : AppCompatActivity() {

    private lateinit var viewModel: TransactionViewModel
    private lateinit var fromDateEditText: EditText
    private lateinit var toDateEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var backArrow: ImageView

    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_data)

        window.statusBarColor = ContextCompat.getColor(this, R.color.darkerRed)

        // Initialize the TransactionDao
        val transactionDao = AppDatabase.getDatabase(application).transactionDao()

        // Initialize the ViewModel using the factory
        val factory = TransactionViewModelFactory(transactionDao)
        viewModel = ViewModelProvider(this, factory).get(TransactionViewModel::class.java)

        fromDateEditText = findViewById(R.id.fromDateEditText)
        toDateEditText = findViewById(R.id.toDateEditText)
        submitButton = findViewById(R.id.submitButton)
        backArrow = findViewById(R.id.back_arrow)

        backArrow.setOnClickListener {
            onBackPressed()
        }

        val fromDateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateEditText(fromDateEditText)
        }

        val toDateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateEditText(toDateEditText)
        }

        fromDateEditText.setOnClickListener {
            DatePickerDialog(
                this, fromDateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        toDateEditText.setOnClickListener {
            DatePickerDialog(
                this, toDateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        submitButton.setOnClickListener {
            val fromDate = fromDateEditText.text.toString()
            val toDate = toDateEditText.text.toString()
            if (fromDate.isNotEmpty() && toDate.isNotEmpty()) {
                viewModel.exportTransactions(this@ExportDataActivity, fromDate, toDate, "transactions_export")
            } else {
                Toast.makeText(this@ExportDataActivity, "Please select both dates", Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun updateDateEditText(editText: EditText) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        editText.setText(dateFormat.format(calendar.time))
    }
}
