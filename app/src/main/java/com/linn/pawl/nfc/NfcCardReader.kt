package com.linn.pawl.nfc

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.cardemulation.CardEmulation
import com.linn.pawl.model.NfcCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NfcCardReader(private val context: Context) {
    private val nfcAdapter: NfcAdapter? by lazy {
        (context.getSystemService(Context.NFC_SERVICE) as? NfcManager)?.defaultAdapter
    }

    private val cardEmulation: CardEmulation? by lazy {
        CardEmulation.getInstance(nfcAdapter)
    }

    @SuppressLint("QueryPermissionsNeeded")
    suspend fun getStoredCards(): List<NfcCard> = withContext(Dispatchers.IO) {
        val cards = mutableListOf<NfcCard>()

        try {
            // Get all NFC payment services
            val pm = context.packageManager
            val nfcServices = pm.queryIntentServices(
                Intent("android.nfc.cardemulation.action.HOST_APDU_SERVICE"),
                PackageManager.MATCH_DEFAULT_ONLY
            )

            nfcServices.forEach { resolveInfo ->
                val serviceInfo = resolveInfo.serviceInfo
                val appLabel = pm.getApplicationLabel(serviceInfo.applicationInfo).toString()
                val componentName = ComponentName(
                    serviceInfo.packageName,
                    serviceInfo.name
                )

                // Check if this service is default for payments
                val isDefault = cardEmulation?.isDefaultServiceForCategory(
                    componentName,
                    CardEmulation.CATEGORY_PAYMENT
                ) == true

                cards.add(
                    NfcCard(
                        id = componentName.flattenToString(),
                        type = if (isDefault) "Default Card" else "Payment Card",
                        lastReadTime = System.currentTimeMillis(),
                        description = "From $appLabel"
                    )
                )
            }

            // Check for off-host services (secure element)
            val offHostServices = pm.queryIntentServices(
                Intent("android.nfc.cardemulation.action.OFF_HOST_APDU_SERVICE"),
                PackageManager.MATCH_DEFAULT_ONLY
            )

            offHostServices.forEach { resolveInfo ->
                val serviceInfo = resolveInfo.serviceInfo
                val appLabel = pm.getApplicationLabel(serviceInfo.applicationInfo).toString()
                val componentName = ComponentName(
                    serviceInfo.packageName,
                    serviceInfo.name
                )

                cards.add(
                    NfcCard(
                        id = componentName.flattenToString(),
                        type = "Secure Element Card",
                        lastReadTime = System.currentTimeMillis(),
                        description = "From $appLabel (Secure Element)"
                    )
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        cards
    }
} 