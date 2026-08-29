package com.yaarapp.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.yaarapp.app.data.YaarRepository
import com.yaarapp.app.firebase.FirebaseModule

class YaarApplication : Application() {

    lateinit var repository: YaarRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialisation Firebase AVANT de créer le repository :
        // cela évite "Default FirebaseApp is not initialized in this process".
        val firebaseReady = FirebaseModule.initialize(this)
        if (firebaseReady) {
            Log.i(TAG, "Firebase initialisé : ${FirebaseApp.getInstance().options.projectId}")
        } else {
            Log.e(
                TAG,
                "Firebase non initialisé. Vérifiez que app/google-services.json est présent " +
                    "et que le plugin com.google.gms.google-services a été appliqué."
            )
        }

        repository = YaarRepository(this)
        repository.startRemoteSync()
    }

    companion object {
        private const val TAG = "YaarApplication"
    }
}
