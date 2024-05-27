package com.example.parkingmadrid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var imageViewProfile: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    companion object {
        private const val REQUEST_IMAGE_PICK = 100
        private const val PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Inicializar vistas
        editTextName = findViewById(R.id.editTextName)
        editTextEmail = findViewById(R.id.editTextEmail)
        imageViewProfile = findViewById(R.id.imageViewProfile)
        val buttonSave = findViewById<Button>(R.id.buttonSave)

        // Obtener datos del usuario y mostrarlos en las vistas
        val currentUser = getCurrentUser()
        editTextName.setText(currentUser.name)
        editTextEmail.setText(currentUser.email)

        // Asignar listener al botón de guardar cambios
        buttonSave.setOnClickListener {
            saveChanges()
        }

        // Asignar listener al imageViewProfile para cambiar la imagen del perfil
        imageViewProfile.setOnClickListener {
            changeProfileImage()
        }
    }

    private fun getCurrentUser(): User {
        val currentUser = auth.currentUser
        val name = currentUser?.displayName ?: "Nombre no disponible"
        val email = currentUser?.email ?: "Email no disponible"
        return User(name, email)
    }

    data class User(val name: String, val email: String)

    private fun saveChanges() {
        // Obtener el nuevo nombre ingresado por el usuario
        val newName = editTextName.text.toString()

        val currentUser = auth.currentUser

        currentUser?.let { user ->
            // Actualizar el nombre en la autenticación de Firebase
            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()

            user.updateProfile(profileUpdate)
                .addOnCompleteListener { profileTask ->
                    if (profileTask.isSuccessful) {
                        showToast("Cambios guardados exitosamente.")
                    } else {
                        showToast("Error al actualizar el nombre. Por favor, inténtalo de nuevo.")
                    }
                }
        }
    }

    private fun changeProfileImage() {
        // Verificar si ya se han otorgado los permisos
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Si ya se han otorgado los permisos, abrir la galería
            openGallery()
        } else {
            // Si los permisos no se han otorgado, solicitarlos al usuario
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Si el usuario otorga los permisos, abrir la galería
                openGallery()
            } else {
                // Si el usuario deniega los permisos, mostrar un mensaje indicando que la operación no se puede realizar
                showToast("Permiso denegado para acceder a la galería")
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

