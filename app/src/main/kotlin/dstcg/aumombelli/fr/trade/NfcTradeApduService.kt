package fr.aumombelli.dstcg.trade

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NfcTradeApduService : HostApduService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray? {
        val command = commandApdu ?: return NfcTradeApdu.failedResponse()
        if (NfcTradeApdu.isSelectAid(command)) {
            return NfcTradeApdu.successResponse()
        }

        val payload = NfcTradeApdu.payloadFromCommand(command) ?: return NfcTradeApdu.notFoundResponse()
        scope.launch {
            val response = runCatching {
                val incoming = NfcTradeCodec.decode(payload)
                val outgoing = NfcTradeSessionRegistry.dispatch(incoming)
                NfcTradeApdu.successResponse(NfcTradeCodec.encode(outgoing))
            }.getOrElse {
                NfcTradeApdu.failedResponse()
            }
            sendResponseApdu(response)
        }
        return null
    }

    override fun onDeactivated(reason: Int) {
        NfcTradeSessionRegistry.notifyDeactivated(reason)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
