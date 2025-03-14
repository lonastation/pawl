package com.linn.pawl.nfc

import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.*
import com.linn.pawl.model.NfcCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NfcCardReader(private val context: Context) {
    private val nfcAdapter: NfcAdapter? by lazy {
        (context.getSystemService(Context.NFC_SERVICE) as? NfcManager)?.defaultAdapter
    }

    suspend fun discoverExistingCards(): List<NfcCard> = withContext(Dispatchers.IO) {
        val cards = mutableListOf<NfcCard>()

        try {
            // Enable reader mode to discover cards
            nfcAdapter?.enableReaderMode(null, { tag ->
                processTag(tag)?.let { card ->
                    cards.add(card)
                }
            }, NfcAdapter.FLAG_READER_NFC_A or 
               NfcAdapter.FLAG_READER_NFC_B or 
               NfcAdapter.FLAG_READER_NFC_F or 
               NfcAdapter.FLAG_READER_NFC_V or 
               NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        cards
    }

    private fun processTag(tag: Tag): NfcCard? {
        return when {
            IsoDep.get(tag) != null -> createCardFromTag(tag, "ISO-DEP Card", getIsoDepDetails(tag))
            MifareClassic.get(tag) != null -> createCardFromTag(tag, "MIFARE Classic", getMifareClassicDetails(tag))
            MifareUltralight.get(tag) != null -> createCardFromTag(tag, "MIFARE Ultralight", getMifareUltralightDetails(tag))
            NfcA.get(tag) != null -> createCardFromTag(tag, "NFC-A Card", getNfcADetails(tag))
            NfcB.get(tag) != null -> createCardFromTag(tag, "NFC-B Card", getNfcBDetails(tag))
            NfcF.get(tag) != null -> createCardFromTag(tag, "NFC-F Card", getNfcFDetails(tag))
            NfcV.get(tag) != null -> createCardFromTag(tag, "NFC-V Card", getNfcVDetails(tag))
            else -> createCardFromTag(tag, "Unknown Card", "Unknown NFC card type")
        }
    }

    private fun createCardFromTag(tag: Tag, type: String, description: String): NfcCard {
        return NfcCard(
            id = bytesToHexString(tag.id) ?: "Unknown",
            type = type,
            lastReadTime = System.currentTimeMillis(),
            description = description
        )
    }

    private fun getIsoDepDetails(tag: Tag): String {
        val isoDep = IsoDep.get(tag) ?: return "ISO/IEC 14443-4 compatible card"
        return try {
            isoDep.connect()
            val maxTransceiveLength = isoDep.maxTransceiveLength
            val timeout = isoDep.timeout
            isoDep.close()
            "ISO/IEC 14443-4 card (Max length: $maxTransceiveLength bytes, Timeout: ${timeout}ms)"
        } catch (e: Exception) {
            "ISO/IEC 14443-4 compatible card"
        }
    }

    private fun getMifareClassicDetails(tag: Tag): String {
        val mifareClassic = MifareClassic.get(tag) ?: return "MIFARE Classic card"
        return try {
            mifareClassic.connect()
            val size = mifareClassic.size
            val sectorCount = mifareClassic.sectorCount
            mifareClassic.close()
            "MIFARE Classic card (Size: $size bytes, Sectors: $sectorCount)"
        } catch (e: Exception) {
            "MIFARE Classic card"
        }
    }

    private fun getMifareUltralightDetails(tag: Tag): String {
        val mifareUltralight = MifareUltralight.get(tag) ?: return "MIFARE Ultralight card"
        return try {
            mifareUltralight.connect()
            val type = when(mifareUltralight.type) {
                MifareUltralight.TYPE_ULTRALIGHT -> "Ultralight"
                MifareUltralight.TYPE_ULTRALIGHT_C -> "Ultralight C"
                else -> "Unknown"
            }
            mifareUltralight.close()
            "MIFARE Ultralight card (Type: $type)"
        } catch (e: Exception) {
            "MIFARE Ultralight card"
        }
    }

    private fun getNfcADetails(tag: Tag): String {
        val nfcA = NfcA.get(tag) ?: return "NFC-A compatible card"
        return try {
            nfcA.connect()
            val maxTransceiveLength = nfcA.maxTransceiveLength
            val timeout = nfcA.timeout
            nfcA.close()
            "NFC-A card (Max length: $maxTransceiveLength bytes, Timeout: ${timeout}ms)"
        } catch (e: Exception) {
            "NFC-A compatible card"
        }
    }

    private fun getNfcBDetails(tag: Tag): String {
        val nfcB = NfcB.get(tag) ?: return "NFC-B compatible card"
        return try {
            nfcB.connect()
            val maxTransceiveLength = nfcB.maxTransceiveLength
            nfcB.close()
            "NFC-B card (Max length: $maxTransceiveLength bytes)"
        } catch (e: Exception) {
            "NFC-B compatible card"
        }
    }

    private fun getNfcFDetails(tag: Tag): String {
        val nfcF = NfcF.get(tag) ?: return "NFC-F compatible card"
        return try {
            nfcF.connect()
            val maxTransceiveLength = nfcF.maxTransceiveLength
            val timeout = nfcF.timeout
            nfcF.close()
            "NFC-F card (Max length: $maxTransceiveLength bytes, Timeout: ${timeout}ms)"
        } catch (e: Exception) {
            "NFC-F compatible card"
        }
    }

    private fun getNfcVDetails(tag: Tag): String {
        val nfcV = NfcV.get(tag) ?: return "NFC-V compatible card"
        return try {
            nfcV.connect()
            val maxTransceiveLength = nfcV.maxTransceiveLength
            nfcV.close()
            "NFC-V card (Max length: $maxTransceiveLength bytes)"
        } catch (e: Exception) {
            "NFC-V compatible card"
        }
    }

    private fun bytesToHexString(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
} 