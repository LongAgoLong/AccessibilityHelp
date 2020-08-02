package com.leo.accessibilityhelp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.OrientationHelper
import com.leo.accessibilityhelp.R
import com.leo.accessibilityhelp.adapter.FileListAdapter
import com.leo.accessibilityhelp.databinding.FragmentChooseFileBinding
import com.leo.accessibilityhelp.lifecyle.ServiceObserver
import com.leo.recyclerview_help.decoration.LineItemDecoration
import com.leo.system.ResHelp
import com.leo.system.WindowUtils

class ChooseFileFragment : Fragment() {
    private lateinit var mBinding: FragmentChooseFileBinding
    private lateinit var list: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        list = mutableListOf(
            ServiceObserver.AD_ACT_WHITE,
            ServiceObserver.AD_IDS,
            ServiceObserver.AD_TEXTS,
            ServiceObserver.PACKAGES
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_choose_file,
            container,
            false
        )
        initView()
        return mBinding.root
    }

    private fun initView() {
        mBinding.fileListView.run {
            val fileListAdapter = FileListAdapter(requireActivity(), list)
            fileListAdapter.mOnItemClickCallback =
                object : FileListAdapter.OnItemClickCallback {
                    override fun onItemClick(position: Int, view: View) {
                        val builder = ChooseFileFragmentArgs.Builder()
                        builder.run {
                            filePath = list[position]
                        }
                        val bundle = builder.build().toBundle()
                        Navigation.findNavController(view)
                            .navigate(
                                R.id.action_chooseFileFragment_to_contentListFragment,
                                bundle
                            )
                    }
                }
            adapter = fileListAdapter
            addItemDecoration(
                LineItemDecoration(
                    requireActivity(),
                    OrientationHelper.VERTICAL,
                    WindowUtils.dp2px(dpValue = 0.7f),
                    false, false,
                    ResHelp.getColor(R.color.c13b5b1)
                )
            )
        }
    }

}