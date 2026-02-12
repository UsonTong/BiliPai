package com.android.purebilibili.feature.cast

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.android.AndroidUpnpServiceImpl
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.LocalDevice
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.registry.DefaultRegistryListener
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.support.avtransport.callback.Play
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI
import org.fourthline.cling.support.avtransport.callback.Stop
import org.fourthline.cling.support.model.DIDLContent
import org.fourthline.cling.support.model.ProtocolInfo
import org.fourthline.cling.support.model.Res
import org.fourthline.cling.support.model.item.VideoItem
import java.io.IOException

object DlnaManager {
    private const val TAG = "DlnaManager"
    
    // UPnP Service Interface
    private var upnpService: AndroidUpnpService? = null
    private var isServiceBound = false
    
    // WiFi Multicast Lock (Android 默认禁用多播以省电，需要手动获取锁)
    private var multicastLock: android.net.wifi.WifiManager.MulticastLock? = null
    
    // Observed State
    private val _devices = MutableStateFlow<List<Device<*, *, *>>>(emptyList())
    val devices: StateFlow<List<Device<*, *, *>>> = _devices.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()
    
    // Registry Listener for Device Discovery
    private val registryListener = object : DefaultRegistryListener() {
        override fun remoteDeviceAdded(registry: Registry, device: RemoteDevice) {
            Logger.i(TAG, "📺 [Cling] Remote device added: ${device.details?.friendlyName ?: "Unknown"}")
            refreshDevices()
        }

        override fun remoteDeviceRemoved(registry: Registry, device: RemoteDevice) {
            Logger.i(TAG, "📺 [Cling] Remote device removed: ${device.details?.friendlyName ?: "Unknown"}")
            refreshDevices()
        }

        override fun localDeviceAdded(registry: Registry, device: LocalDevice) {
            refreshDevices()
        }

        override fun localDeviceRemoved(registry: Registry, device: LocalDevice) {
            refreshDevices()
        }
    }
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            upnpService = service as AndroidUpnpService
            
            // Clear cache and registry to ensure fresh search
            upnpService?.registry?.removeAllRemoteDevices()
            upnpService?.registry?.addListener(registryListener)
            
            // Start searching
            Logger.i(TAG, "📺 [Cling] Starting UPnP search...")
            upnpService?.controlPoint?.search()
            
            _isConnected.value = true
            Logger.i(TAG, "📺 [Cling] Service connected, multicast enabled")
            refreshDevices() // In case there are already devices
        }

        override fun onServiceDisconnected(className: ComponentName) {
            upnpService = null
            _isConnected.value = false
            isServiceBound = false
            Logger.d(TAG, "Cling Service Disconnected")
        }
    }

    fun bindService(context: Context) {
        if (isServiceBound) {
            Logger.d(TAG, "📺 [Cling] bindService ignored: already bound")
            return
        }
        // 获取 WiFi 多播锁，这是 UPnP 发现的关键！
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            multicastLock = wifiManager.createMulticastLock("DlnaManager")
            multicastLock?.setReferenceCounted(true)
            multicastLock?.acquire()
            Logger.i(TAG, "📺 [Cling] MulticastLock acquired")
        } catch (e: Exception) {
            Logger.e(TAG, "📺 [Cling] Failed to acquire multicast lock: ${e.message}")
        }
        
        val intent = Intent(context, AndroidUpnpServiceImpl::class.java)
        isServiceBound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        if (!isServiceBound) {
            Logger.e(TAG, "📺 [Cling] Failed to bind AndroidUpnpService")
            releaseMulticastLock()
        }
    }

    fun unbindService(context: Context) {
        if (upnpService != null) {
            upnpService?.registry?.removeListener(registryListener)
        }
        if (isServiceBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                Logger.w(TAG, "📺 [Cling] unbindService ignored: ${e.message}")
            }
            isServiceBound = false
        }
        upnpService = null
        _isConnected.value = false
        _devices.value = emptyList()
        releaseMulticastLock()
    }
    
    fun refresh() {
        Logger.i(TAG, "📺 [Cling] Manual refresh triggered")
        upnpService?.registry?.removeAllRemoteDevices()
        upnpService?.controlPoint?.search()
        // 延迟刷新设备列表，给设备发现一些时间
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            refreshDevices()
        }, 2000)
    }
    
    private fun refreshDevices() {
        val allDevices = upnpService?.registry?.devices?.toList() ?: emptyList()
        Logger.i(TAG, "📺 [Cling] Registry contains ${allDevices.size} devices")
        
        // 更宽松的过滤 - 只要是 MediaRenderer 或包含 AVTransport 服务
        val renderers = allDevices.filter { device ->
            val isMediaRenderer = device.type?.type?.contains("MediaRenderer", ignoreCase = true) == true
            val hasAVTransport = device.findService(org.fourthline.cling.model.types.UDAServiceId("AVTransport")) != null
            isMediaRenderer || hasAVTransport
        }
        _devices.value = renderers
        
        if (renderers.isNotEmpty()) {
            Logger.i(TAG, "📺 [Cling] Found ${renderers.size} MediaRenderer(s): ${renderers.map { it.details?.friendlyName }.joinToString()}")
        } else {
            Logger.i(TAG, "📺 [Cling] No MediaRenderer devices found (total devices: ${allDevices.size})")
        }
    }

    /**
     * Cast a URL to the selected device
     * @param device The target UPnP Device
     * @param url The actual video URL (or proxy URL)
     * @param title Title of the video
     * @param creator Author/Uploader
     */
    fun cast(device: Device<*, *, *>, url: String, title: String, creator: String) {
        val service = device.findService(org.fourthline.cling.model.types.UDAServiceId("AVTransport"))
        if (service == null) {
             Logger.e(TAG, "📺 [Cling] Device does not support AVTransport")
             return
        }

        val controlPoint = upnpService?.controlPoint ?: return
        
        // Build Metadata (DIDL-Lite)
        // This is usually required by TVs to display title properly
        val metadata = createMetadata(url, title, creator)

        // 1. Set URI
        controlPoint.execute(object : SetAVTransportURI(service, url, metadata) {
            override fun success(invocation: org.fourthline.cling.model.action.ActionInvocation<*>) {
                Logger.d(TAG, "Set URI Success, starting play...")
                // 2. Play
                controlPoint.execute(object : Play(service) {
                    override fun success(invocation: org.fourthline.cling.model.action.ActionInvocation<*>) {
                        Logger.d(TAG, "Play Command Success")
                    }

                    override fun failure(invocation: org.fourthline.cling.model.action.ActionInvocation<*>, operation: org.fourthline.cling.model.message.UpnpResponse, defaultMsg: String) {
                        Logger.e(TAG, "Play Command Failed: $defaultMsg")
                    }
                })
            }

            override fun failure(invocation: org.fourthline.cling.model.action.ActionInvocation<*>, operation: org.fourthline.cling.model.message.UpnpResponse, defaultMsg: String) {
                Logger.e(TAG, "Set URI Failed: $defaultMsg")
            }
        })
    }
    
    fun stop(device: Device<*, *, *>) {
        val service = device.findService(org.fourthline.cling.model.types.UDAServiceId("AVTransport")) ?: return
        upnpService?.controlPoint?.execute(object : Stop(service) {
            override fun success(invocation: org.fourthline.cling.model.action.ActionInvocation<*>) {
                Logger.d(TAG, "Stop command successful")
            }
            override fun failure(invocation: org.fourthline.cling.model.action.ActionInvocation<*>, operation: org.fourthline.cling.model.message.UpnpResponse, defaultMsg: String) {
                Logger.e(TAG, "Stop command failed: $defaultMsg")
            }
        })
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
                Logger.d(TAG, "MulticastLock released")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to release multicast lock", e)
        }
        multicastLock = null
    }

    private fun createMetadata(url: String, title: String, creator: String): String {
        try {
            val didl = DIDLContent()
            val res = Res(ProtocolInfo("http-get:*:video/mp4:*"), null, url)
            
            val item = VideoItem("1", "0", title, creator, res)
            didl.addItem(item)
            
            val parser = org.fourthline.cling.support.contentdirectory.DIDLParser()
            return parser.generate(didl)
        } catch (e: Exception) {
            Logger.e(TAG, "Error generating metadata", e)
            return ""
        }
    }
}
