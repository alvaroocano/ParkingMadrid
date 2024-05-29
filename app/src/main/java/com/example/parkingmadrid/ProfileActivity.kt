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
        private const val DEFAULT_IMAGE_URL = "gs://parking-madrid-fc293.appspot.com/user.png"
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
                    val profileImageUrl = it.profileImage ?: DEFAULT_IMAGE_URL
                    loadImage(profileImageUrl)
                } ?: run {
                    loadImage(DEFAULT_IMAGE_URL)
                }
            }.addOnFailureListener {
                loadImage(DEFAULT_IMAGE_URL)
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

    private fun loadImage(imageUrl: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val profileImageRef = storageReference.child("profileImages/${currentUser.uid}.jpg")
            profileImageRef.downloadUrl.addOnSuccessListener { uri ->
                // Imagen personalizada encontrada
                Glide.with(this).load(uri).into(imageViewProfile)
            }.addOnFailureListener {
                // Imagen personalizada no encontrada, usar imagen por defecto
                val defaultImageRef = FirebaseStorage.getInstance().getReferenceFromUrl(DEFAULT_IMAGE_URL)
                defaultImageRef.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(this).load(uri).into(imageViewProfile)
                }.addOnFailureListener {
                    showToast("Error al cargar la imagen de perfil.")
                }
            }
        } else {
            val defaultImageRef = FirebaseStorage.getInstance().getReferenceFromUrl(DEFAULT_IMAGE_URL)
            defaultImageRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(this).load(uri).into(imageViewProfile)
            }.addOnFailureListener {
                showToast("Error al cargar la imagen de perfil.")
            }
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
                            loadImage(downloadUri.toString()) // Cargar la nueva imagen en el ImageView
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

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, NavigationActivity::class.java))
    }



    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
