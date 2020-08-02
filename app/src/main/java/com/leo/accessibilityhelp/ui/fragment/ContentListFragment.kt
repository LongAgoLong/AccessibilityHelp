package com.leo.accessibilityhelp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.leo.accessibilityhelp.R
import com.leo.accessibilityhelp.databinding.FragmentContentListBinding
import com.leo.accessibilityhelp.lifecyle.ServiceObserver
import com.leo.commonutil.storage.IOUtil

class ContentListFragment : Fragment() {
    private lateinit var mBinding: FragmentContentListBinding
    private var filePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.run {
            val bundle = ChooseFileFragmentArgs.fromBundle(this)
            filePath = bundle.filePath
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_content_list,
            container,
            false
        )
        initView()
        return mBinding.root
    }

    private fun initView() {
        filePath?.let {
            val idsStr = IOUtil.getDiskText(fileName = ServiceObserver.AD_IDS)
            mBinding.resultTv.text = idsStr?.replace("#", "#\n")
        }
    }
}