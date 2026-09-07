package com.yaarapp.app

import android.app.Application
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.yaarapp.app.data.YaarRepository
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class YaarApplication : Application() {
    lateinit var repository: YaarRepository
        private set

    override fun onCreate() {
        super.onCreate()
        installCrashGuard()
        repository = YaarRepository(this)
        repository.startRemoteSync()
    }

    /**
     * ---------------------------------------------------------------------------------
     * FILET DE SÉCURITÉ ANTI-FERMETURE BRUTALE ("Ma boutique" se fermait sans message)
     * ---------------------------------------------------------------------------------
     * Un `Thread.setDefaultUncaughtExceptionHandler` classique ne suffit PAS à empêcher
     * une fermeture d'application : quand une exception non interceptée traverse
     * `Looper.loop()` sur le thread principal (ce qui est le cas de la quasi-totalité
     * des plantages Compose/Vues), Android considère le thread principal comme terminé
     * et tue le processus juste après avoir appelé ce handler — journaliser ne change
     * rien à la fermeture.
     *
     * La technique ci-dessous (utilisée par plusieurs bibliothèques anti-crash Android
     * connues, ex. "Cockroach") consiste à RELANCER nous-mêmes `Looper.loop()` depuis le
     * handler d'exception, sur le thread principal. Cela réinjecte une nouvelle boucle de
     * traitement des messages sur la même file d'attente : le thread principal ne
     * "termine" donc jamais réellement, et Android ne tue pas le processus. L'écran en
     * cours peut rester figé une fraction de seconde le temps de la recomposition
     * suivante, mais l'application reste ouverte au lieu de se fermer brutalement.
     *
     * Chaque exception interceptée est :
     *  - journalisée dans Logcat sous le tag "YAAR_FATAL" (visible avec :
     *    adb logcat -s YAAR_FATAL)
     *  - écrite dans un fichier texte persistant : /data/data/com.yaarapp.app/files/crash_log.txt
     *    récupérable avec : adb shell run-as com.yaarapp.app cat files/crash_log.txt
     *  - signalée à l'utilisateur par un court message, pour qu'il sache qu'un souci a
     *    été rattrapé automatiquement au lieu de se retrouver sans explication.
     *
     * Ceci est un filet de sécurité, pas un correctif de la cause racine : si un problème
     * revient souvent, consultez crash_log.txt (ou Logcat) pour en connaître la cause
     * exacte et corriger le code concerné.
     */
    private fun installCrashGuard() {
        val mainThread = Looper.getMainLooper().thread

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logCrash(thread.name, throwable)

            if (thread === mainThread) {
                // On relance la boucle principale au lieu de laisser le processus mourir.
                // En cas de nouvelle exception, ce même handler sera réinvoqué : c'est
                // volontaire (relance récursive), c'est ce qui permet de survivre à des
                // plantages répétés sans fermer l'application.
                try {
                    Looper.loop()
                } catch (_: Throwable) {
                    // Sera recapturé par ce handler ; rien à faire ici.
                }
            } else {
                // Un crash sur un thread secondaire non protégé reste fatal : on ne peut
                // pas relancer un Looper qui n'existe pas sur ce thread. On tue proprement
                // le processus dans ce seul cas (rare, car toute la logique métier passe
                // par des coroutines déjà protégées par runCatching/.catch{}).
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    private fun logCrash(threadName: String, throwable: Throwable) {
        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        Log.e("YAAR_FATAL", "Exception non interceptée sur le thread \"$threadName\" :\n$stackTrace")

        runCatching {
            File(filesDir, "crash_log.txt").appendText(
                "\n\n===== ${java.util.Date()} (thread: $threadName) =====\n$stackTrace"
            )
        }

        if (threadName == Looper.getMainLooper().thread.name) {
            runCatching {
                android.os.Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        this,
                        "Un problème inattendu a été évité automatiquement. Réessayez l'action.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
