package com.yaarapp.app.util

/**
 * Construit la page HTML minimale qui charge le widget Kkiapay et l'ouvre automatiquement.
 * Chargée dans une WebView par [com.yaarapp.app.ui.screens.KkiapayCheckoutScreen], qui
 * expose un objet JavaScript "Android" (voir KkiapayBridge) pour recevoir les évènements
 * "success" / "failed" / "close" du widget.
 */
object KkiapayHtmlBuilder {

    fun build(amountFcfa: Int, description: String, whatsappForReceipt: String): String {
        val safeDescription = description.replace("\"", "'")
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <script src="https://cdn.kkiapay.me/k.js"></script>
            </head>
            <body style="margin:0;background:#ffffff;">
                <script>
                    function launchWidget() {
                        openKkiapayWidget({
                            amount: $amountFcfa,
                            api_key: "${KkiapayConfig.PUBLIC_API_KEY}",
                            sandbox: ${KkiapayConfig.SANDBOX},
                            phone: "$whatsappForReceipt",
                            data: "$safeDescription"
                        });
                    }

                    addKkiapayListener('success', function (response) {
                        if (window.Android) { Android.onPaymentSuccess(JSON.stringify(response)); }
                    });
                    addKkiapayListener('failed', function (response) {
                        if (window.Android) { Android.onPaymentFailed(JSON.stringify(response)); }
                    });
                    addKkiapayListener('close', function () {
                        if (window.Android) { Android.onWidgetClosed(); }
                    });

                    window.onload = launchWidget;
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
