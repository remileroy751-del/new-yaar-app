package com.yaarapp.app

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
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

        // [CrashActivity] tourne volontairement dans un process séparé (":crash_report",
        // voir AndroidManifest.xml) pour pouvoir survivre au crash du process principal.
        // Application.onCreate() s'exécute alors aussi dans CE process séparé : il ne
        // faut surtout pas y réinitialiser la base de données ni relancer la synchro
        // Supabase, sous peine de reproduire le même plantage dans l'écran d'erreur
        // lui-même. On saute donc l'initialisation lourde dans ce process précis.
        if (isCrashReportProcess()) return

        repository = YaarRepository(this)
        repository.startRemoteSync()
    }

    private fun isCrashReportProcess(): Boolean {
        val pid = android.os.Process.myPid()
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        val processName = manager.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName
        return processName?.endsWith(":crash_report") == true
    }

    /**
     * ---------------------------------------------------------------------------------
     * FILET DE SÉCURITÉ ANTI-FERMETURE BRUTALE ("Ma boutique" fermait l'app sans message)
     * ---------------------------------------------------------------------------------
     * Toute exception non interceptée (Compose, coroutine mal protégée, etc.) est
     * capturée ici avant qu'Android ne tue le processus. Au lieu de laisser l'application
     * disparaître sans explication, on :
     *  1. Journalise le détail complet (Logcat "YAAR_FATAL" + fichier crash_log.txt).
     *  2. Ouvre [CrashActivity], qui tourne dans un PROCESS SÉPARÉ (":crash_report") et
     *     affiche le message d'erreur exact à l'écran avec un bouton "Copier". C'est
     *     nécessaire car le process principal (celui qui a planté) va être tué juste
     *     après : seule une Activity dans un autre process peut survivre pour l'afficher.
     *  3. Tue le process principal proprement, puis l'utilisateur peut relancer l'app
     *     depuis l'écran d'erreur.
     *
     * Ceci est un filet de sécurité, pas un correctif de la cause racine : envoyez le
     * texte copié depuis l'écran d'erreur pour obtenir une correction définitive.
     */
    private fun installCrashGuard() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stackTrace = logCrash(thread.name, throwable)
            runCatching { launchCrashScreen(stackTrace) }
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(10)
        }
    }

    private fun launchCrashScreen(stackTrace: String) {
        val intent = Intent(this, CrashActivity::class.java).apply {
            putExtra(CrashActivity.EXTRA_STACK_TRACE, stackTrace)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun logCrash(threadName: String, throwable: Throwable): String {
        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val fullReport = "===== ${java.util.Date()} (thread: $threadName) =====\n$stackTrace"
        Log.e("YAAR_FATAL", "Exception non interceptée sur le thread \"$threadName\" :\n$stackTrace")
        runCatching { File(filesDir, "crash_log.txt").appendText("\n\n$fullReport") }
        return fullReport
    }
}
