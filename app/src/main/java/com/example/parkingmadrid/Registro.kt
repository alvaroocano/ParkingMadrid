package com.example.parkingmadrid

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.DatePicker
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStream
import java.util.*

class Registro : AppCompatActivity(), DatePickerDialog.OnDateSetListener {
    private lateinit var gifView: GifView
    private lateinit var editTextDOB: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // Inicializar GifView
        gifView = findViewById(R.id.gifView)

        // Inicializar EditText para la fecha de nacimiento
        editTextDOB = findViewById(R.id.editTextDOB)
        editTextDOB.setOnClickListener { showDatePickerDialog() }
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
        editTextDOB.setText(selectedDate)
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, this, year, month, dayOfMonth)
        datePickerDialog.show()
    }
}
