package com.example.parkingmadrid

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
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
import com.google.android.material.textfield.TextInputLayout
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
    private lateinit var textInputLayoutPassword: TextInputLayout

    private var isPasswordVisible = false
    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        mAuth = FirebaseAuth.getInstance()
        FirebaseApp.initializeApp(this)
        database = FirebaseDatabase.getInstance("https://parking-madrid-fc293-default-rtdb.europe-west1.firebasedatabase.app")

        val currentUser = mAuth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            val intent = Intent(this, NavigationActivity::class.java)
            startActivity(intent)
            finish()
        }

        editTextUsernameOrEmail = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        textInputLayoutPassword = findViewById(R.id.textInputLayoutPassword)

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

        callbackManager = CallbackManager.Factory.create()

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
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        textInputLayoutPassword.setEndIconDrawable(R.drawable.view)

        textInputLayoutPassword.setEndIconOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                editTextPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                textInputLayoutPassword.setEndIconDrawable(R.drawable.ver)
            } else {
                editTextPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                textInputLayoutPassword.setEndIconDrawable(R.drawable.view)
            }
            editTextPassword.setSelection(editTextPassword.text.length)
        }

    }



    private fun signIn() {
        val usernameOrEmail = editTextUsernameOrEmail.text.toString()
        val password = editTextPassword.text.toString()

        if (usernameOrEmail.isNotEmpty() && password.isNotEmpty()) {
            val usersRef = database.reference.child("usuarios")
            usersRef.orderByChild("nombreUsuario").equalTo(usernameOrEmail).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (userSnapshot in dataSnapshot.children) {
                            val email = userSnapshot.child("email").value.toString()
                            signInWithEmailAndPassword(email, password)
                        }
                    } else {
                        signInWithEmailAndPassword(usernameOrEmail, password)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Error al buscar usuario en la base de datos.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "Por favor, ingrese correo y contraseña.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signInWithEmailAndPassword(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    if (user?.isEmailVerified == true) {
                        val intent = Intent(this, NavigationActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Por favor, verifica tu correo electrónico.", Toast.LENGTH_SHORT).show()
                        mAuth.signOut()
                    }
                } else {
                    Toast.makeText(baseContext, "Error en la autenticación.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithGoogle() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private lateinit var lastLoginResult: LoginResult

    private fun signInWithFacebook() {
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                lastLoginResult = loginResult
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                Toast.makeText(this@MainActivity, "Autenticación cancelada.", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Toast.makeText(this@MainActivity, "Error al iniciar sesión con Facebook.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleFacebookUserCollision(email: String, loginResult: LoginResult) {
        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val result = task.result
                    if (result?.signInMethods?.contains(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD) == true) {
                    } else if (result?.signInMethods?.contains(FacebookAuthProvider.PROVIDER_ID) == true) {
                        val credential = FacebookAuthProvider.getCredential(loginResult.accessToken.token)
                        mAuth.currentUser?.linkWithCredential(credential)
                            ?.addOnCompleteListener { linkTask ->
                                if (linkTask.isSuccessful) {
                                    val intent = Intent(this, NavigationActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(this, "Error al fusionar cuentas: ${linkTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    Toast.makeText(this, "Error al obtener métodos de inicio de sesión para el correo electrónico: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, NavigationActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        exception.email?.let { email ->
                            handleFacebookUserCollision(email, lastLoginResult)
                        }
                    } else {
                        Toast.makeText(this, "Error en la autenticación con Facebook.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        callbackManager.onActivityResult(requestCode, resultCode, data)

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
                    if (user?.isEmailVerified == true) {
                        val intent = Intent(this, NavigationActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Por favor, verifica tu correo electrónico.", Toast.LENGTH_SHORT).show()
                        mAuth.signOut()
                    }
                } else {
                    Toast.makeText(this, "Error en la autenticación con Google.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}
