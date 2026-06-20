package fr.aumombelli.dstcg.trade

internal object BluetoothTradeTieBreaker {
    fun shouldKeepOutgoingConnection(
        localSessionId: String,
        remoteSessionId: String,
    ): Boolean = localSessionId <= remoteSessionId
}
