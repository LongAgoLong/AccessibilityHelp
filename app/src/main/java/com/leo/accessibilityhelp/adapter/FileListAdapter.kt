package com.leo.accessibilityhelp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.leo.accessibilityhelp.R
import com.leo.accessibilityhelp.holder.ItemFileHolder

class FileListAdapter constructor(
    private var context: Context,
    private var list: MutableList<String>
) :
    RecyclerView.Adapter<ItemFileHolder>() {
    var mOnItemClickCallback: OnItemClickCallback? = null
        set(value) {
            field = value
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemFileHolder {
        return ItemFileHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.item_file,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ItemFileHolder, position: Int) {
        val s = list[position]
        holder.mBinding.contentTv.text = s
        holder.mBinding.root.setOnClickListener {
            mOnItemClickCallback?.run {
                onItemClick(holder.adapterPosition, holder.mBinding.root)
            }
        }
    }

    interface OnItemClickCallback {
        fun onItemClick(position: Int, view: View)
    }
}