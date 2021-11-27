package com.android.chatapp

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import com.android.chatapp.databinding.ActivityMainBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class AuthActivity : AppCompatActivity() {

    private var forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    private var mCallBacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks? = null
    private var mVerificationId: String? = null
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        phoneLl.visibility = View.VISIBLE
        codeLl.visibility = View.GONE

        firebaseAuth= FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please Wait")
        progressDialog.setCanceledOnTouchOutside(false)

        mCallBacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                progressDialog.dismiss()
                Toast.makeText(this@AuthActivity,"${e.message}", Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                mVerificationId = verificationId
                forceResendingToken = token
                progressDialog.dismiss()

                phoneLl.visibility = View.GONE
                codeLl.visibility = View.VISIBLE

                Toast.makeText(this@AuthActivity,"Verification code sent ...", Toast.LENGTH_SHORT).show()


            }
        }

        phoneContinueBtn.setOnClickListener{
            val phone = phoneEt.text.toString().trim()
            if(TextUtils.isEmpty(phone)){
                Toast.makeText(this@AuthActivity,"Please Enter phone number", Toast.LENGTH_SHORT).show()
            }else{
                startPhoneNumberVerification(phone)
            }
        }
        resendCodeTv.setOnClickListener{
            val phone = phoneEt.text.toString().trim()
            if(TextUtils.isEmpty(phone)){
                Toast.makeText(this@AuthActivity,"Please Enter phone number", Toast.LENGTH_SHORT).show()
            }else{
                resendVerificationCode(phone, forceResendingToken)
            }
        }

        codeSubmitBtn.setOnClickListener {
            val code = codeEt.text.trim()
            if(TextUtils.isEmpty(code)){
                Toast.makeText(this@AuthActivity,"Please Enter code", Toast.LENGTH_SHORT).show()
            }else{
                verifyPhoneNumberWithCode(mVerificationId, code.toString())
            }
        }

    }
    private fun startPhoneNumberVerification(phone:String){
        progressDialog.setMessage("Verifying Phone Number ...")
        progressDialog.show()

        val options = mCallBacks?.let {
            PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(it)
                .build()
        }

        if (options != null) {
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    private fun resendVerificationCode(phone: String, token: PhoneAuthProvider.ForceResendingToken?){
        progressDialog.setMessage("Resending Verification code ...")
        progressDialog.show()

        val options = token?.let {
            mCallBacks?.let { it1 ->
                PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(phone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(it1)
                    .setForceResendingToken(it)
                    .build()
            }
        }

        if (options != null) {
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    private fun verifyPhoneNumberWithCode(verificationId:String?, code:String){
        progressDialog.setMessage("Verifying code ...")
        progressDialog.show()

        val credential = verificationId?.let { PhoneAuthProvider.getCredential(it, code) }
        if (credential != null) {
            signInWithPhoneAuthCredential(credential)
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        progressDialog.setMessage("Logging in")
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener{
                progressDialog.dismiss()
                val phone = firebaseAuth.currentUser?.phoneNumber
                Toast.makeText(this,"Logged in as $phone", Toast.LENGTH_SHORT).show()

                saveUserToFirebaseDatabase()
                val intent = Intent(this, LatestMessagesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()

            }
            .addOnFailureListener{e->
                progressDialog.dismiss()
                Toast.makeText(this,"${e.message}", Toast.LENGTH_SHORT).show()
            }

    }

    private fun saveUserToFirebaseDatabase(){
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        val user = User(uid,phoneEt.text.toString())

        ref.setValue(user)
            .addOnSuccessListener {

                Toast.makeText(this,"Created user successfully", Toast.LENGTH_SHORT).show()
            }
    }
}
class User(val uid: String, val username: String){
    constructor(): this("","")
}