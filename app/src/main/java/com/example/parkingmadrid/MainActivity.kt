package com.example.parkingmadrid

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.*
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager
    private lateinit var editTextUsernameOrEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var database: FirebaseDatabase

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        mAuth = FirebaseAuth.getInstance()
        FirebaseApp.initializeApp(this)
        database = FirebaseDatabase.getInstance("https://parking-madrid-fc293-default-rtdb.europe-west1.firebasedatabase.app")

        // Verificar si el usuario ya ha iniciado sesión
        if (mAuth.currentUser != null) {
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
            finish()
        }

        editTextUsernameOrEmail = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)

        val btnIniciarSesion: Button = findViewById(R.id.buttonLogin)
        btnIniciarSesion.setOnClickListener {
            signIn()
        }

        val btnRegistro: Button = findViewById(R.id.buttonRegister)
        btnRegistro.setOnClickListener {
            val intent = Intent(this, Registro::class.java)
            startActivity(intent)
        }

        val buttonLoginGoogle = findViewById<ImageButton>(R.id.buttonLoginGoogle)
        buttonLoginGoogle.setOnClickListener {
            signInWithGoogle()
        }

        // Inicializar el CallbackManager de Facebook
        callbackManager = CallbackManager.Factory.create()

        // Configurar el botón de inicio de sesión con Facebook
        val buttonLoginFacebook = findViewById<ImageButton>(R.id.buttonLoginFacebook)
        buttonLoginFacebook.setOnClickListener {
            signInWithFacebook()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        val textViewForgotPassword: TextView = findViewById(R.id.textViewForgotPassword)
        textViewForgotPassword.setOnClickListener {
            val email = editTextUsernameOrEmail.text.toString().trim()
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
                    Toast.makeText(this, "Error al enviar el correo de restablecimiento de contraseña.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signIn() {
        val usernameOrEmail = editTextUsernameOrEmail.text.toString()
        val password = editTextPassword.text.toString()

        if (usernameOrEmail.isNotEmpty() && password.isNotEmpty()) {
            // Verificar si el usuario está registrado con correo electrónico y contraseña
            val usersRef = database.reference.child("usuarios")
            usersRef.orderByChild("nombreUsuario").equalTo(usernameOrEmail).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // El usuario se ha registrado con nombre de usuario
                        for (userSnapshot in dataSnapshot.children) {
                            val email = userSnapshot.child("email").value.toString()
                            signInWithEmailAndPassword(email, password)
                        }
                    } else {
                        // Intentar iniciar sesión con correo electrónico
                        signInWithEmailAndPassword(usernameOrEmail, password)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Error al buscar usuario en la base de datos.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "Por favor, ingrese nombre de usuario/correo y contraseña.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithEmailAndPassword(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    val intent = Intent(this, NavigationActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(baseContext, "Error en la autenticación.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithGoogle() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signInWithFacebook() {
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                // El inicio de sesión con Facebook se canceló
            }

            override fun onError(error: FacebookException) {
                Toast.makeText(this@MainActivity, "Error al iniciar sesión con Facebook.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    val intent = Intent(this, NavigationActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error en la autenticación con Facebook.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Pasar el resultado al CallbackManager de Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data)

        // Resultado del inicio de sesión con Google
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Error al iniciar sesión con Google.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    val intent = Intent(this, NavigationActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Error en la autenticación con Google.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}
