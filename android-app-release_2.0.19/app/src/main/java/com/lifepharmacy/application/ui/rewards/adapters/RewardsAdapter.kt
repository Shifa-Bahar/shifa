package com.lifepharmacy.application.ui.rewards.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.lifepharmacy.application.R
import com.lifepharmacy.application.databinding.ItemRewardsBinding
import com.lifepharmacy.application.model.general.GeneralResponseModel
import com.lifepharmacy.application.model.home.HomeResponseItem
import com.lifepharmacy.application.model.rewards.RewardItem

class RewardsAdapter(context: Activity?, private val onItemTapped: ClickItemRewards) :
  RecyclerView.Adapter<RewardsAdapter.ItemViewHolder>() {
// var rewardMain: GeneralResponseModel<RewardModel>? = null

  var arrayList: ArrayList<RewardItem>? = ArrayList()
  var activity: Activity? = context
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
    val binding: ItemRewardsBinding = DataBindingUtil.inflate(
      LayoutInflater.from(activity),
      R.layout.item_rewards,
      parent, false
    )
    return ItemViewHolder(binding.root)
  }

  override fun getItemCount(): Int {
    //    return if (null != rewardMain) rewardMain!!.data?.data?.size!!
    return if (null != arrayList) arrayList!!.size else 0

  }

  class ItemViewHolder internal constructor(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    var binding: ItemRewardsBinding? = DataBindingUtil.bind(itemView)
  }

  fun setDataChanged(reward: ArrayList<RewardItem>?) {
//    if (reward != null) {
//      rewardMain = reward
//    }
//    notifyDataSetChanged()
    arrayList?.clear()
    if (reward != null) {
      arrayList?.addAll(reward)
    }
    notifyDataSetChanged()
  }

  override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
    val item = arrayList!![position]
//    val item = rewardMain?.data
    holder.binding?.item = item
    holder.binding?.click = onItemTapped
  }

}