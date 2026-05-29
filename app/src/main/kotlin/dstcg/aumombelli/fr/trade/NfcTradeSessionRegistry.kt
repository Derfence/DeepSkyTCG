package fr.aumombelli.dstcg.trade

internal interface NfcTradeSessionHandler {
    suspend fun handleIncoming(packet: NfcTradePacket): NfcTradePacket
    fun onNfcDeactivated(reason: Int) = Unit
}

internal object NfcTradeSessionRegistry {
    @Volatile
    private var handler: NfcTradeSessionHandler? = null

    fun install(handler: NfcTradeSessionHandler) {
        this.handler = handler
    }

    fun clear(handler: NfcTradeSessionHandler) {
        if (this.handler === handler) {
            this.handler = null
        }
    }

    suspend fun dispatch(packet: NfcTradePacket): NfcTradePacket =
        handler?.handleIncoming(packet)
            ?: packet.failureResponse("Aucune session d'echange active.")

    fun notifyDeactivated(reason: Int) {
        handler?.onNfcDeactivated(reason)
    }
}
