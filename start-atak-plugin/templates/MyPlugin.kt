/*
 * Minimal IPlugin skeleton for ATAK SDK 5.5+.
 *
 * REPLACE:
 *   • package declaration  (line 1)
 *   • class name           (must also match plugin.xml `impl` attribute)
 *   • R.string.app_name    (defined in res/values/strings.xml)
 *   • R.drawable.ic_tray   (place a 96x96 white-on-transparent PNG in res/drawable/)
 *
 * Place at: app/src/main/java/<package-path>/MyPlugin.kt
 *
 * This skeleton creates a single toolbar button that opens a pane.
 * Extend by adding map overlays, CoT detail handlers, settings, etc. in onStart().
 * Tear everything down symmetrically in onStop().
 */
package com.atakmap.android.MYPLUGIN.plugin

import android.content.Context
import android.graphics.drawable.Drawable
import com.atak.plugins.impl.PluginContextProvider
import com.atakmap.android.maps.MapView
import gov.tak.api.commons.graphics.Bitmap
import gov.tak.api.plugin.IPlugin
import gov.tak.api.plugin.IServiceController
import gov.tak.api.ui.IHostUIService
import gov.tak.api.ui.ToolbarItem
import gov.tak.api.ui.ToolbarItemAdapter
import gov.tak.platform.marshal.MarshalManager

class MyPlugin(serviceController: IServiceController) : IPlugin {

    private val serviceController: IServiceController = serviceController
    private var pluginContext: Context? = null
    private var uiService: IHostUIService? = null
    private var toolbarItem: ToolbarItem? = null

    init {
        // Plugin context (durable across plugin reloads).
        val ctxProvider = serviceController
            .getService<PluginContextProvider?>(PluginContextProvider::class.java)
        if (ctxProvider != null) {
            pluginContext = ctxProvider.getPluginContext()
            // Optional theme — only if you have a custom AppTheme defined.
            // pluginContext!!.setTheme(R.style.ATAKPluginTheme)
        }

        uiService = serviceController.getService<IHostUIService?>(IHostUIService::class.java)

        toolbarItem = ToolbarItem.Builder(
            pluginContext!!.getString(R.string.app_name),
            MarshalManager.marshal<Bitmap?, Drawable?>(
                pluginContext!!.resources.getDrawable(R.drawable.ic_tray),
                Drawable::class.java,
                Bitmap::class.java
            )
        )
            .setListener(object : ToolbarItemAdapter() {
                override fun onClick(item: ToolbarItem?) {
                    // TODO: open your plugin's main pane here.
                    // Typical pattern: build a Pane via PaneBuilder, then call
                    // uiService.showPane(pane, null). See the ATAK_Plugin_Development_Guide.pdf
                    // (in the SDK root) and the samples/ directory for examples.
                }
            })
            .setIdentifier(pluginContext!!.packageName)
            .build()
    }

    override fun onStart() {
        if (uiService == null) return
        uiService!!.addToolbarItem(toolbarItem)

        val mapView = MapView.getMapView() ?: return
        // TODO: register CoT detail handlers, create overlays, etc.
        //       Hold references to anything that needs torn down in onStop().
    }

    override fun onStop() {
        uiService?.removeToolbarItem(toolbarItem)
        // TODO: unregister CoT detail handlers, dispose overlays, clear singletons.
        //       Every onStart() resource needs a matching onStop() teardown.
    }
}
