package com.zhenl.crawler.views

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatDialog
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingData
import com.zhenl.crawler.R
import com.zhenl.crawler.databinding.DialogRemoteDeviceBinding
import com.zhenl.crawler.utils.CastHelper
import com.zhenl.violet.base.BasePagedListAdapter
import com.zhenl.violet.base.BaseViewHolder
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.fourthline.cling.android.AndroidUpnpService
import org.fourthline.cling.android.AndroidUpnpServiceImpl
import org.fourthline.cling.model.action.ActionInvocation
import org.fourthline.cling.model.message.UpnpResponse
import org.fourthline.cling.model.meta.LocalDevice
import org.fourthline.cling.model.meta.RemoteDevice
import org.fourthline.cling.model.meta.Service
import org.fourthline.cling.model.types.UDAServiceType
import org.fourthline.cling.registry.Registry
import org.fourthline.cling.registry.RegistryListener
import org.fourthline.cling.support.avtransport.callback.Play
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI
import java.util.concurrent.CopyOnWriteArrayList


/**
 * Created by lin on 2021/8/8.
 */
class RemoteDeviceDialog(context: Context, private val url: String) :
    AppCompatDialog(context, R.style.TransparentDialog), RegistryListener {

    private lateinit var adapter: DeviceAdapter
    private lateinit var serviceConnection: ServiceConnection

    private val deviceList = CopyOnWriteArrayList<RemoteDevice>()

    private var upnpService: AndroidUpnpService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: DialogRemoteDeviceBinding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.dialog_remote_device,
            null,
            false
        )
        setContentView(binding.root)
        initView(binding)

        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                upnpService = service as? AndroidUpnpService
                upnpService?.let {
                    it.registry.addListener(this@RemoteDeviceDialog)
                    it.controlPoint.search()
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                upnpService?.registry?.removeListener(this@RemoteDeviceDialog)
                upnpService = null
            }
        }
        context.bindService(
            Intent(context, AndroidUpnpServiceImpl::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun initView(binding: DialogRemoteDeviceBinding) {
        adapter = DeviceAdapter().apply {
            setOnItemClickListener { adapter, view, position ->
                val device = adapter.getDefItem(position) ?: return@setOnItemClickListener
                play(device as RemoteDevice)
            }
        }
        binding.rv.adapter = adapter
    }

    private fun play(device: RemoteDevice) {
        val metadata: String = CastHelper.createMetadata(url, "id", "name")
        val avtService = device.findService(UDAServiceType("AVTransport")) ?: return
        upnpService?.controlPoint?.execute(object : SetAVTransportURI(avtService, url, metadata) {
            override fun success(invocation: ActionInvocation<out Service<*, *>>?) {
                super.success(invocation)
                upnpService?.controlPoint?.execute(object : Play(avtService) {
                    override fun success(invocation: ActionInvocation<out Service<*, *>>?) {
                        super.success(invocation)
                        dismiss()
                    }

                    override fun failure(
                        invocation: ActionInvocation<out Service<*, *>>?,
                        operation: UpnpResponse?,
                        defaultMsg: String?
                    ) {
                    }
                })
            }

            override fun failure(
                invocation: ActionInvocation<out Service<*, *>>?,
                operation: UpnpResponse?,
                defaultMsg: String?
            ) {
            }
        })
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        context.unbindService(serviceConnection)
    }

    private class DeviceAdapter : BasePagedListAdapter<RemoteDevice>() {
        override fun getLayoutResId(viewType: Int): Int = R.layout.item_remote_device

        override fun convert(holder: BaseViewHolder, item: RemoteDevice) {
            holder.setText(R.id.tv, item.details?.friendlyName ?: item.displayString)
        }
    }

    override fun remoteDeviceDiscoveryStarted(registry: Registry?, device: RemoteDevice?) {
    }

    override fun remoteDeviceDiscoveryFailed(
        registry: Registry?,
        device: RemoteDevice?,
        ex: Exception?
    ) {
    }

    override fun remoteDeviceAdded(registry: Registry?, device: RemoteDevice?) {
        deviceList.add(device)
        MainScope().launch {
            adapter.submitData(PagingData.from(deviceList))
        }
    }

    override fun remoteDeviceUpdated(registry: Registry?, device: RemoteDevice?) {
    }

    override fun remoteDeviceRemoved(registry: Registry?, device: RemoteDevice?) {
        deviceList.remove(device)
        MainScope().launch {
            adapter.submitData(PagingData.from(deviceList))
        }
    }

    override fun localDeviceAdded(registry: Registry?, device: LocalDevice?) {
    }

    override fun localDeviceRemoved(registry: Registry?, device: LocalDevice?) {
    }

    override fun beforeShutdown(registry: Registry?) {
    }

    override fun afterShutdown() {
    }
}