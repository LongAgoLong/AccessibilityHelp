package com.leo.accessibilityhelplib.util

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.media.AudioManager
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.leo.accessibilityhelplib.AccessibilityHelp
import com.leo.system.context.ContextHelper
import com.leo.system.log.ZLog
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer

/**
 * 封装一些模拟手势操作
 */
object VtsTouchUtils {

    private const val TAG = "AutoTouchUtils"
    private val SPEECH_WINDOW_CONTROLS: MutableList<SpeechWindowControl> = CopyOnWriteArrayList()

    /**
     * 播放系统点击按键音
     */
    private fun playClickSoundEffect() {
        try {
            val audioManager =
                ContextHelper.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 模拟返回键
     *
     * @return
     */
    fun handleBackEvent(mService: AccessibilityService?): Boolean {
        if (null != mService) {
            playClickSoundEffect()
            return mService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        }
        return false
    }

    /**
     * 模拟home键
     *
     * @return
     */
    fun handleHomeEvent(mService: AccessibilityService?): Boolean {
        if (null != mService) {
            playClickSoundEffect()
            return mService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        }
        return false
    }

    /**
     * 执行点击事件
     */
    fun performClick(nodeInfo: AccessibilityNodeInfo?): Boolean {
        nodeInfo?.run {
            val bounds = Rect()
            this.getBoundsInScreen(bounds)
            clickScreen(
                x = bounds.centerX(),
                y = bounds.centerY()
            )
            ZLog.d(TAG, "performClick: ${this.text}")
            return true
        }
        ZLog.i(TAG, "performClick() return false")
        return false
    }

    /**
     * 执行滚动
     *
     * @param nodeInfo
     * @param isScrollForward 是否下一页
     */
    fun performScroll(nodeInfo: AccessibilityNodeInfo, isScrollForward: Boolean) {
        nodeInfo.performAction(if (isScrollForward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }

    /**
     * 输入文本
     *
     * @param nodeInfo
     * @param content
     */
    fun performInput(nodeInfo: AccessibilityNodeInfo, content: String?) {
        val arguments = Bundle()
        arguments.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            content
        )
        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    fun addSpeechWindowControl(speechWindowControl: SpeechWindowControl?) {
        if (speechWindowControl == null || SPEECH_WINDOW_CONTROLS.contains(speechWindowControl)) {
            return
        }
        SPEECH_WINDOW_CONTROLS.add(speechWindowControl)
    }

    fun removeSpeechWindowControl(speechWindowControl: SpeechWindowControl?) {
        if (speechWindowControl == null || !SPEECH_WINDOW_CONTROLS.contains(speechWindowControl)) {
            return
        }
        SPEECH_WINDOW_CONTROLS.remove(speechWindowControl)
    }

    /**
     * 通过坐标点击屏幕
     * 配置文件需添加android:canPerformGestures="true"
     *
     * @param x
     * @param y
     */
    fun clickScreen(
        x: Int,
        y: Int,
        beforeTouchEvent: () -> Unit? = {},
        afterTouchEvent: () -> Unit? = {}
    ) {
        val service = AccessibilityHelp.getInstance().mService ?: return
        HandlerUtils.getMainHandler().post {
            ZLog.i(TAG, "clickScreen: start -> x = $x ; y = $y")
            beforeTouchEvent()
            SPEECH_WINDOW_CONTROLS.forEach(Consumer { speechWindowControl: SpeechWindowControl ->
                speechWindowControl.beforeExecuteTouch()
            })
            val gestureBuilder = GestureDescription.Builder()
            val path = Path()
            path.moveTo(x.toFloat(), y.toFloat())
            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            service.dispatchGesture(
                gestureBuilder.build(),
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription) {
                        super.onCompleted(gestureDescription)
                        ZLog.i(TAG, "clickScreen: Gesture Completed")
                        afterTouchEvent()
                        SPEECH_WINDOW_CONTROLS.forEach(Consumer { speechWindowControl: SpeechWindowControl ->
                            speechWindowControl.afterExecuteTouch(false)
                        })
                    }

                    override fun onCancelled(gestureDescription: GestureDescription) {
                        super.onCancelled(gestureDescription)
                        ZLog.w(TAG, "clickScreen: Gesture Canceled")
                        afterTouchEvent()
                        SPEECH_WINDOW_CONTROLS.forEach(Consumer { speechWindowControl: SpeechWindowControl ->
                            speechWindowControl.afterExecuteTouch(true)
                        })
                    }
                },
                HandlerUtils.getMainHandler()
            )
        }
    }

    /**
     * 通过坐标滑动屏幕
     * 配置文件需添加android:canPerformGestures="true"
     */
    fun swipeScreen(
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
        beforeTouchEvent: () -> Unit = {},
        afterTouchEvent: () -> Unit = {}
    ) {
        val service = AccessibilityHelp.getInstance().mService ?: return
        HandlerUtils.getMainHandler().post {
            ZLog.i(
                TAG,
                "swipeScreen: start -> fromX = $fromX ; fromY = $fromY ; toX = $toX ; toY = $toY"
            )
            beforeTouchEvent()
            SPEECH_WINDOW_CONTROLS.forEach(Consumer { speechWindowControl: SpeechWindowControl ->
                speechWindowControl.beforeExecuteTouch()
            })
            val gestureBuilder = GestureDescription.Builder()
            val path = Path()
            path.moveTo(fromX.toFloat(), fromY.toFloat())
            path.lineTo(toX.toFloat(), toY.toFloat())
            gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            service.dispatchGesture(
                gestureBuilder.build(),
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription) {
                        super.onCompleted(gestureDescription)
                        ZLog.i(TAG, "swipeScreen: Gesture Completed")
                        afterTouchEvent()
                        SPEECH_WINDOW_CONTROLS.forEach(Consumer { speechWindowControl: SpeechWindowControl ->
                            speechWindowControl.afterExecuteTouch(false)
                        })
                    }

                    override fun onCancelled(gestureDescription: GestureDescription) {
                        super.onCancelled(gestureDescription)
                        ZLog.w(TAG, "swipeScreen: Gesture Canceled")
                        afterTouchEvent()
                        SPEECH_WINDOW_CONTROLS.forEach(Consumer { speechWindowControl: SpeechWindowControl ->
                            speechWindowControl.afterExecuteTouch(true)
                        })
                    }
                },
                HandlerUtils.getMainHandler()
            )
        }
    }

    /**
     * 串行分发手势事件
     * @param gestureList List<StrokeDescription>
     * @param beforeTouchEvent Function0<Unit>
     * @param afterTouchEvent Function1<[@kotlin.ParameterName] Boolean, Unit>
     * @param sleepTime Long 事件间隔
     */
    fun dispatchGestureListSerial(
        gestureList: List<GestureDescription.StrokeDescription>,
        beforeTouchEvent: () -> Unit = {},
        afterTouchEvent: (isCancel: Boolean) -> Unit = {},
        sleepTime: Long = 0
    ) {
        val service = AccessibilityHelp.getInstance().mService ?: return
        HandlerUtils.getVtsHandler().post {
            var mIsInterrupt = false
            val mLock = ReentrantLock()
            beforeTouchEvent()
            gestureList.forEach {
                if (mIsInterrupt) {
                    return@forEach
                }
                val gestureBuilder = GestureDescription.Builder()
                gestureBuilder.addStroke(it)
                service.dispatchGesture(
                    gestureBuilder.build(),
                    object : AccessibilityService.GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription) {
                            super.onCompleted(gestureDescription)
                            ZLog.i(TAG, "swipeScreen: Gesture Completed")
                            mLock.unlock()
                        }

                        override fun onCancelled(gestureDescription: GestureDescription) {
                            super.onCancelled(gestureDescription)
                            ZLog.w(TAG, "swipeScreen: Gesture Canceled")
                            mIsInterrupt = true
                            mLock.unlock()
                        }
                    },
                    HandlerUtils.getMainHandler()
                )
                mLock.lock()
                safeSleepThread(sleepTime)
            }
            afterTouchEvent(mIsInterrupt)
        }
    }

    /**
     * 并行分发手势事件
     * @param gestureList List<StrokeDescription>
     * @param beforeTouchEvent Function0<Unit>
     * @param afterTouchEvent Function1<[@kotlin.ParameterName] Boolean, Unit>
     */
    fun dispatchGestureListParallel(
        gestureList: List<GestureDescription.StrokeDescription>,
        beforeTouchEvent: () -> Unit = {},
        afterTouchEvent: (isCancel: Boolean) -> Unit = {}
    ) {
        val service = AccessibilityHelp.getInstance().mService ?: return
        HandlerUtils.getVtsHandler().post {
            beforeTouchEvent()
            val gestureBuilder = GestureDescription.Builder()
            gestureList.forEach {
                gestureBuilder.addStroke(it)
            }
            service.dispatchGesture(
                gestureBuilder.build(),
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription) {
                        super.onCompleted(gestureDescription)
                        ZLog.i(TAG, "swipeScreen: Gesture Completed")
                        afterTouchEvent(false)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription) {
                        super.onCancelled(gestureDescription)
                        ZLog.w(TAG, "swipeScreen: Gesture Canceled")
                        afterTouchEvent(true)
                    }
                },
                HandlerUtils.getMainHandler()
            )
        }
    }

    private fun safeSleepThread(mills: Long) {
        try {
            Thread.sleep(mills)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @FunctionalInterface
    interface SpeechWindowControl {
        /**
         * 执行触摸事件之前回调
         */
        fun beforeExecuteTouch()

        /**
         * 执行触摸事件结束之后回调
         */
        fun afterExecuteTouch(isCancel: Boolean)
    }
}
