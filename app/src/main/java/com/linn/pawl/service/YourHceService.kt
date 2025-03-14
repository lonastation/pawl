package com.linn.pawl.service

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

class YourHceService : HostApduService() {
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        // Handle APDU commands here
        // This is where you implement the card emulation logic
        return ByteArray(0)
    }

    override fun onDeactivated(reason: Int) {
        // Handle deactivation
    }
} 