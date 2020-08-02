package com.leo.accessibilityhelp.holder

import androidx.recyclerview.widget.RecyclerView
import com.leo.accessibilityhelp.databinding.ItemFileBinding

class ItemFileHolder : RecyclerView.ViewHolder {
    var mBinding: ItemFileBinding
        get() = field

    constructor(mBinding: ItemFileBinding) : super(mBinding.root) {
        this.mBinding = mBinding
    }
}