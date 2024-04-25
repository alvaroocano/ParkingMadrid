package com.example.parkingmadrid

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.facebook.*
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val btnRegistro: Button = findViewById(R.id.buttonRegister)
        btnRegistro.setOnClickListener {
            val intent = Intent(this, Registro::class.java)
            startActivity(intent)
        }

        mAuth = FirebaseAuth.getInstance()

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

    }

    private fun signInWithGoogle() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signInWithFacebook() {
        // Configurar los permisos que necesitas
        val permissions = listOf("email", "public_profile")

        // Iniciar sesión con Facebook
        LoginManager.getInstance().logInWithReadPermissions(this, permissions)
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Inicio de sesión exitoso
                    val user = mAuth.currentUser
                    // Ir a la siguiente actividad
                    val intent = Intent(this, NavigationActivity::class.java)
                    startActivity(intent)
                    finish() // Finalizar la actividad actual si no se desea volver atrás
                } else {
                    // Manejar errores
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Pasar el resultado del inicio de sesión con Facebook al callbackManager
        callbackManager.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Autenticar con Google
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Manejar errores
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Inicio de sesión exitoso
                    val user = mAuth.currentUser
                    // Ir a la siguiente actividad
                    val intent = Intent(this, NavigationActivity::class.java)
                    startActivity(intent)
                    finish() // Finalizar la actividad actual si no se desea volver atrás
                } else {
                    // Manejar errores
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "MainActivity"
    }
}


