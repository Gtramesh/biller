package com.invoicesaver.app.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ListResult
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.File

class BillRepository {

    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun authStateListener(callback: (FirebaseUser?) -> Unit): FirebaseAuth.AuthStateListener {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            callback(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        return listener
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }

    fun signIn(email: String, password: String): com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult> =
        auth.signInWithEmailAndPassword(email, password)

    fun signUp(email: String, password: String): com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult> =
        auth.createUserWithEmailAndPassword(email, password)

    fun signOut() = auth.signOut()

    fun uploadExcel(file: File, fileName: String): UploadTask {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")
        return storage.reference.child("bills/$uid/$fileName").putFile(Uri.fromFile(file))
    }

    fun listMyExcels(): com.google.android.gms.tasks.Task<ListResult> {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")
        return storage.reference.child("bills/$uid").listAll()
    }

    fun downloadExcel(fileName: String, dest: File): com.google.android.gms.tasks.Task<File> {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Not signed in")
        val ref: StorageReference = storage.reference.child("bills/$uid/$fileName")
        return ref.getFile(dest).continueWith { dest }
    }
}
