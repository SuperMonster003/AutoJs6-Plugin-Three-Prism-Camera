package io.github.supermonster003.autojs6.plugin.three.prism.camera.session

import org.autojs.plugin.camera.api.CameraContract

/**
 * The one camera holder of the plugin (protocol "Limits": one session in the whole plugin; roadmap D17, appendix D
 * Q7 (a)): a script session or the viewfinder. A second `openSession` gets `CAMERA_BUSY` with [busyLabel] of the
 * holder; the viewfinder takes the camera from a script session through [preemptForViewfinder], which tells the old
 * holder ([Holder.onPreempted]) to close with reason `pluginUi`.
 * zh-CN: 插件唯一的相机持有者 (协议 "Limits" 一节: 整个插件只有一个会话; 路线图 D17, 附录 D Q7 (a)): 脚本会话或取景页. 第二次 `openSession` 得到
 * `CAMERA_BUSY` 与持有者的 [busyLabel]; 取景页经 [preemptForViewfinder] 从脚本会话手中接过相机, 并通知原持有者 ([Holder.onPreempted]) 以原因
 * `pluginUi` 关闭.
 */
internal class CameraExclusivity {

    /**
     * [label] is the `holderLabel` of a script session (informational, protocol "The viewfinder and `CAMERA_BUSY`").
     * zh-CN: [label] 是脚本会话的 `holderLabel` (仅供参考, 协议 "The viewfinder and `CAMERA_BUSY`" 一节).
     */
    class Holder(val label: String, val viewfinder: Boolean) {
        @Volatile
        var onPreempted: () -> Unit = {}

        /** The `holder` of `CAMERA_BUSY`. zh-CN: `CAMERA_BUSY` 的 `holder`. */
        val busyLabel: String get() = if (viewfinder) CameraContract.HOLDER_PLUGIN_UI else label
    }

    private var current: Holder? = null

    /** Takes the camera; answers the current holder when another one has it. zh-CN: 获取相机; 已被他人持有时返回当前持有者. */
    @Synchronized
    fun acquire(holder: Holder): Holder? {
        val busy = current
        if (busy != null && busy !== holder) return busy
        current = holder
        return null
    }

    /** Gives the camera back; only the current holder can. zh-CN: 归还相机; 只有当前持有者可以. */
    @Synchronized
    fun release(holder: Holder): Boolean {
        if (current !== holder) return false
        current = null
        return true
    }

    /**
     * The viewfinder takes the camera at once; the previous holder, if any, is told to close and is answered.
     * zh-CN: 取景页立即接过相机; 如有原持有者, 通知其关闭并将其返回.
     */
    fun preemptForViewfinder(viewfinder: Holder): Holder? {
        require(viewfinder.viewfinder) { "only the viewfinder preempts" }
        val previous = synchronized(this) { current.also { current = viewfinder } }
        if (previous != null && previous !== viewfinder) {
            runCatching { previous.onPreempted() }
            return previous
        }
        return null
    }

    @Synchronized
    fun holder(): Holder? = current

    companion object {
        /** The plugin process' instance. zh-CN: 插件进程的实例. */
        val shared = CameraExclusivity()
    }

}
