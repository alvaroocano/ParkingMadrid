package com.example.parkingmadrid

import android.app.DatePickerDialog
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.widget.DatePicker
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class Registro : AppCompatActivity(), DatePickerDialog.OnDateSetListener {
    private lateinit var textureViewBackground: TextureView
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var editTextDOB: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // Inicializar TextureView
        textureViewBackground = findViewById(R.id.textureViewBackground)

        // Inicializar EditText para la fecha de nacimiento
        editTextDOB = findViewById(R.id.editTextDOB)
        editTextDOB.setOnClickListener { showDatePickerDialog() }

        // Configurar el listener de SurfaceTexture para el TextureView
        textureViewBackground.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                val surface = Surface(surface)
                mediaPlayer = MediaPlayer.create(applicationContext, R.drawable.parking)
                mediaPlayer.setSurface(surface)
                mediaPlayer.isLooping = true
                mediaPlayer.start()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                mediaPlayer.stop()
                mediaPlayer.release()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
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
