package com.leo.accessibilityhelp.view

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.leo.accessibilityhelp.CstService
import com.leo.accessibilityhelp.R

/**
 * Created by jinliangshan on 16/12/26.
 */
class FloatingView(private val mContext: Context) : LinearLayout(mContext) {
    private val mWindowManager: WindowManager =
        mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var mTvPackageName: TextView? = null
    private var mTvClassName: TextView? = null
    private var mIvClose: ImageView? = null

    lateinit var preP: Point
    lateinit var curP: Point

    init {
        initView()
    }

    private fun initView() {
        View.inflate(mContext, R.layout.layout_floating, this)
        mTvPackageName = findViewById(R.id.tv_package_name)
        mTvClassName = findViewById(R.id.tv_class_name)
        mIvClose = findViewById(R.id.iv_close)

        mIvClose!!.setOnClickListener {
            Toast.makeText(mContext, "关闭悬浮框", Toast.LENGTH_SHORT).show()
            val intent = Intent(mContext, CstService::class.java)
            intent.putExtra(CstService.TYPE, CstService.TYPE_COMMAND)
            intent.putExtra(
                CstService.KEY_COMMAND, CstService.COMMAND_CLOSE
            )
            mContext.startService(intent)
        }
    }

    fun updateInfo(packageName: String = "", className: String = "") {
        Log.d(TAG, "event:$packageName: $className")
        mTvPackageName!!.text = packageName
        mTvClassName!!.text =
            if (className.startsWith(packageName)) className.substring(packageName.length) else className
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> preP = Point(event.rawX.toInt(), event.rawY.toInt())

            MotionEvent.ACTION_MOVE -> {
                curP = Point(event.rawX.toInt(), event.rawY.toInt())
                curP.let {
                    val dx = it.x - preP.x
                    val dy = it.y - preP.y
                    val layoutParams = this.layoutParams as WindowManager.LayoutParams
                    layoutParams.x += dx
                    layoutParams.y += dy
                    mWindowManager.updateViewLayout(this, layoutParams)
                    preP = it
                }
            }
        }

        return false
    }

    companion object {
        const val TAG = "FloatingView"
    }
}
