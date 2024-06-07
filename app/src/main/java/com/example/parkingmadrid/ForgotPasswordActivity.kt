package com.example.parkingmadrid

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var editTextEmail: EditText
    private lateinit var buttonSend: Button
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        mAuth = FirebaseAuth.getInstance()

        editTextEmail = findViewById(R.id.editTextEmail)
        buttonSend = findViewById(R.id.buttonSend)

        buttonSend.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                resetPassword(email)
            } else {
                Toast.makeText(this, "Por favor, ingrese un correo electrónico válido.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetPassword(email: String) {
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Correo de restablecimiento de contraseña enviado.", Toast.LENGTH_SHORT).show()
                } else {
                    if (task.exception is FirebaseAuthInvalidUserException) {
                        Toast.makeText(this, "No existe una cuenta con ese correo electrónico.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error al enviar el correo de restablecimiento de contraseña.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }
}
