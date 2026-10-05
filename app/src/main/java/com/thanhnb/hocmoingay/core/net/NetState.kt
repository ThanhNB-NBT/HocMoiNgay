package com.thanhnb.hocmoingay.core.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

/** Có mạng hay không (để tắt nút Chạy/Nộp). Dùng INTERNET thay VALIDATED vì server dev chạy http nội bộ trên máy ảo. */
class NetState(ctx: Context, scope: CoroutineScope) {
    private val cm = ctx.getSystemService(ConnectivityManager::class.java)
    private fun now() = cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    val online: StateFlow<Boolean> = callbackFlow {
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(n: Network, c: NetworkCapabilities) {
                trySend(c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
            override fun onLost(n: Network) { trySend(now()) }
        }
        cm.registerDefaultNetworkCallback(cb)
        trySend(now())
        awaitClose { cm.unregisterNetworkCallback(cb) }
    }.stateIn(scope, SharingStarted.Eagerly, now())
}
