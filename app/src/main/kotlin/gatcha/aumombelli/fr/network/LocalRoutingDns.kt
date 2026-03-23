package fr.aumombelli.gatcha.network

import fr.aumombelli.gatcha.BuildConfig
import okhttp3.Dns
import java.net.InetAddress

class LocalRoutingDns(
    private val defaultDns: Dns = Dns.SYSTEM,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val routedHost = BuildConfig.LOCAL_ROUTED_HOST
        val routedIp = BuildConfig.LOCAL_ROUTED_IP

        if (
            routedHost.isNotBlank() &&
            routedIp.isNotBlank() &&
            hostname.equals(routedHost, ignoreCase = true)
        ) {
            return InetAddress.getAllByName(routedIp).toList()
        }

        return defaultDns.lookup(hostname)
    }
}
