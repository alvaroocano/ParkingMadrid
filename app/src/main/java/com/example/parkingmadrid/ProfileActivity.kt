package com.example.parkingmadrid

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.parkingmadrid.Clases.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ProfileActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var imageViewProfile: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storageReference: StorageReference

    companion object {
        private const val REQUEST_IMAGE_PICK = 100
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

        // Obtener datos del usuario y mostrarlos en las vistas
        val currentUser = auth.currentUser
        if (currentUser != null) {
            editTextEmail.setText(currentUser.email)
            database.child(currentUser.uid).get().addOnSuccessListener {
                val user = it.getValue(User::class.java)
                user?.let {
                    editTextName.setText(it.fullName)
                    // Cargar imagen con Glide
                    if (!it.profileImage.isNullOrEmpty()) {
                        Glide.with(this).load(it.profileImage).into(imageViewProfile)
                    } else {
                        imageViewProfile.setImageResource(R.drawable.defaultuser)
                    }
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
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                uploadImageToFirebase(uri)
            }
        }
    }

    private fun uploadImageToFirebase(uri: Uri) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val ref = storageReference.child("profileImages/${currentUser.uid}.jpg")

            ref.putFile(uri).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setPhotoUri(downloadUri)
                        .build()

                    currentUser.updateProfile(profileUpdates).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            database.child(currentUser.uid).child("profileImage").setValue(downloadUri.toString())
                            Glide.with(this).load(downloadUri).into(imageViewProfile)
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
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, NavigationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
