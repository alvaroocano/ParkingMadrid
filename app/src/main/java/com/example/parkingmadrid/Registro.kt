package com.example.parkingmadrid

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingmadrid.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class Registro : AppCompatActivity(), DatePickerDialog.OnDateSetListener {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var editTextFirstName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextUsername: EditText
    private lateinit var editTextDOB: EditText
    private lateinit var editTextPassword: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

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

        // Registrar al usuario en Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registro exitoso
                    val user = mAuth.currentUser
                    // Guardar los datos adicionales del usuario en Firebase Realtime Database
                    user?.let {
                        val userId = it.uid
                        val userRef = database.getReference("users").child(userId)
                        val userData = HashMap<String, Any>()
                        userData["firstName"] = firstName
                        userData["email"] = email
                        userData["username"] = username
                        userData["dob"] = dob
                        userRef.setValue(userData)
                    }
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Manejar errores de registro
                }
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

