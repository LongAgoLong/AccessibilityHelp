package com.leo.accessibilityhelp.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.leo.accessibilityhelp.R
import com.leo.accessibilityhelp.databinding.ActivityEditConfigBinding

class EditConfigActivity : BaseActivity() {
    private lateinit var mBinding: ActivityEditConfigBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_edit_config
        )
        initActionBar()
    }

    override fun initActionBar() {
        super.initActionBar()
        val actionBar = supportActionBar ?: return
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.title = "过滤规则"
    }


}