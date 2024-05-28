package com.example.parkingmadrid

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingmadrid.Clases.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar
import java.util.regex.Pattern

class Registro : AppCompatActivity(), DatePickerDialog.OnDateSetListener {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage

    private lateinit var editTextFirstName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextUsername: EditText
    private lateinit var editTextDOB: EditText
    private lateinit var editTextPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://parking-madrid-fc293-default-rtdb.europe-west1.firebasedatabase.app")
        storage = FirebaseStorage.getInstance()

        // Inicializar EditTexts
        editTextFirstName = findViewById(R.id.editTextFirstName)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextDOB = findViewById(R.id.editTextDOB)
        editTextPassword = findViewById(R.id.editTextPassword)

        // Botón de registro
        val btnRegistrarse: Button = findViewById(R.id.buttonRegistrarse)
        btnRegistrarse.setOnClickListener {
            registrarUsuario()
        }

        // Inicializar EditText para la fecha de nacimiento
        editTextDOB.setOnClickListener { showDatePickerDialog() }
    }

    private fun registrarUsuario() {
        val firstName = editTextFirstName.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val username = editTextUsername.text.toString().trim()
        val dob = editTextDOB.text.toString().trim()
        val password = editTextPassword.text.toString()

        if (firstName.isEmpty() || email.isEmpty() || username.isEmpty() || dob.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, "Correo no válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidName(firstName)) {
            Toast.makeText(this, "El nombre no debe contener caracteres especiales", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidDOB(dob)) {
            Toast.makeText(this, "Debes ser mayor de edad", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidPassword(password)) {
            Toast.makeText(this, "Al menos 6 caracteres, una mayúscula y un carácter especial", Toast.LENGTH_SHORT).show()
            return
        }

        // Registrar al usuario en Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registro exitoso
                    val user = mAuth.currentUser
                    user?.let {
                        val userId = it.uid
                        val usersRef = database.reference.child("users").child(userId)

                        // URL de la imagen predeterminada
                        val defaultProfileImageUrl = "gs://parking-madrid-fc293.appspot.com/user.png"

                        val userData = User(dob, firstName, username, email, defaultProfileImageUrl)

                        usersRef.setValue(userData).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                sendEmailVerification()
                            } else {
                                Toast.makeText(this, "Error al guardar datos del usuario", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Error al registrar usuario", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendEmailVerification() {
        val user = mAuth.currentUser
        user?.sendEmailVerification()
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Correo de verificación enviado a ${user.email}", Toast.LENGTH_SHORT).show()
                    // Cerrar sesión del usuario
                    mAuth.signOut()
                    // Redirigir al usuario a la actividad de inicio de sesión
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error al enviar correo de verificación.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidName(name: String): Boolean {
        val regex = "^[a-zA-ZñÑáéíóúÁÉÍÓÚ ]+$"
        return name.matches(regex.toRegex())
    }

    private fun isValidDOB(dob: String): Boolean {
        val parts = dob.split("/")
        if (parts.size != 3) return false

        val day = parts[0].toIntOrNull() ?: return false
        val month = parts[1].toIntOrNull() ?: return false
        val year = parts[2].toIntOrNull() ?: return false

        val dobCalendar = Calendar.getInstance()
        dobCalendar.set(year, month - 1, day)

        val today = Calendar.getInstance()

        if (dobCalendar.after(today)) return false

        var age = today.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR)

        if (today.get(Calendar.DAY_OF_YEAR) < dobCalendar.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        return age >= 18
    }

    private fun isValidPassword(password: String): Boolean {
        val passwordPattern = Pattern.compile("^(?=.*[A-Z])(?=.*[@#\$%^&+=!]).{6,}$")
        return passwordPattern.matcher(password).matches()
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
        editTextDOB.setText(selectedDate)
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()

        calendar.add(Calendar.YEAR, -18)
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, this, year, month, dayOfMonth)
        datePickerDialog.datePicker.maxDate = Calendar.getInstance().timeInMillis // Deshabilitar fechas futuras
        datePickerDialog.show()
    }
}
