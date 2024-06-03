package com.example.parkingmadrid

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parkingmadrid.Clases.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class ProfileActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var imageViewProfile: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storageReference: StorageReference

    companion object {
        private const val REQUEST_IMAGE_PICK = 100
        private const val DEFAULT_IMAGE_PATH = "user.png"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("users")
        storageReference = FirebaseStorage.getInstance().reference

        // Inicializar vistas
        editTextName = findViewById(R.id.editTextName)
        editTextEmail = findViewById(R.id.editTextEmail)
        imageViewProfile = findViewById(R.id.imageViewProfile)
        val buttonSave = findViewById<Button>(R.id.buttonSave)
        descargarImagenFirebase(imageViewProfile)
        // Obtener datos del usuario y mostrarlos en las vistas
        val currentUser = auth.currentUser
        if (currentUser != null) {
            editTextEmail.setText(currentUser.email)
            database.child(currentUser.uid).get().addOnSuccessListener { it ->
                val user = it.getValue(User::class.java)
                user?.let {
                    editTextName.setText(it.fullName)
                    // Cargar imagen del perfil

                }
            }.addOnFailureListener {
                showToast("Error al obtener los datos del usuario.")
            }
        }

        // Asignar listener al botón de guardar cambios
        buttonSave.setOnClickListener {
            saveChanges()
        }

        // Asignar listener al imageViewProfile para cambiar la imagen del perfil
        imageViewProfile.setOnClickListener {
            openGallery()
        }
    }

    private fun saveChanges() {
        val newName = editTextName.text.toString().trim()
        val currentUser = auth.currentUser

        if (currentUser != null && newName.isNotEmpty()) {
            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()

            currentUser.updateProfile(profileUpdate).addOnCompleteListener { profileTask ->
                if (profileTask.isSuccessful) {
                    database.child(currentUser.uid).child("fullName").setValue(newName)
                    showToast("Cambios guardados exitosamente.")
                    val intent = Intent(this, NavigationActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                } else {
                    showToast("Error al actualizar el nombre. Por favor, inténtalo de nuevo.")
                }
            }
        } else {
            showToast("El nombre no puede estar vacío.")
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imagen = data.data
            if (imagen != null) {
                agregarImagenFirebase(imagen)
            }
        }
    }

    private fun agregarImagenFirebase(imagen: Uri) {
        val email = FirebaseAuth.getInstance().currentUser?.email
        val rutaImagen = FirebaseStorage.getInstance().reference.child("images").child("$email.jpg")

        rutaImagen.putFile(imagen)
            .addOnSuccessListener {
                rutaImagen.downloadUrl.addOnSuccessListener { uri ->
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setPhotoUri(uri)
                        .build()

                    FirebaseAuth.getInstance().currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            database.child(auth.currentUser?.uid!!).child("images").setValue(uri.toString())
                            descargarImagenFirebase(imageViewProfile)
                            showToast("Imagen de perfil actualizada.")
                        } else {
                            showToast("Error al actualizar la imagen de perfil.")
                        }
                    }
                }
            }.addOnFailureListener {
                showToast("Error al subir la imagen. Por favor, inténtalo de nuevo.")
            }
    }

    private fun descargarImagenFirebase(imagen: ImageView) {
        val email = FirebaseAuth.getInstance().currentUser?.email
        val rutaImagen = FirebaseStorage.getInstance().reference.child("images").child("$email.jpg")
        val archivoLocal = File.createTempFile("tempImage", "jpg")

        rutaImagen.getFile(archivoLocal)
            .addOnSuccessListener {
                val bitmap = BitmapFactory.decodeFile(archivoLocal.absolutePath)
                imagen.setImageBitmap(bitmap)
            }
            .addOnFailureListener {
                imagen.setImageResource(R.drawable.baseline_account_circle_24)
            }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, NavigationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
