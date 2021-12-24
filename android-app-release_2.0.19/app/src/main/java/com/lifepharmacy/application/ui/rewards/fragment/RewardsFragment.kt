package com.lifepharmacy.application.ui.rewards.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.lifepharmacy.application.R
import com.lifepharmacy.application.base.BaseFragment
import com.lifepharmacy.application.databinding.FragmentRewardsBinding
import com.lifepharmacy.application.managers.analytics.RewardsListScreenOpen
import com.lifepharmacy.application.model.rewards.RewardItem
//import com.lifepharmacy.application.managers.analytics.RewardsListScreenOpen
import com.lifepharmacy.application.network.Result
import com.lifepharmacy.application.ui.documents.dialog.DocTypSelectionBottomSheet.Companion.TAG
import com.lifepharmacy.application.ui.profile.viewmodel.ProfileViewModel
import com.lifepharmacy.application.ui.rewards.adapters.ClickItemRewards
import com.lifepharmacy.application.ui.rewards.adapters.RewardsAdapter
import com.lifepharmacy.application.ui.rewards.viewmodels.RewardsViewModel
import com.lifepharmacy.application.ui.utils.topbar.ClickTool
import com.lifepharmacy.application.utils.universal.RecyclerPagingListener
import com.lifepharmacy.application.utils.universal.RecyclerViewPagingUtil
import dagger.hilt.android.AndroidEntryPoint


/**
 * A simple [Fragment] subclass.
 */
@AndroidEntryPoint
class RewardsFragment : BaseFragment<FragmentRewardsBinding>(), ClickItemRewards,
  ClickRewardsFragment, ClickTool,
  RecyclerPagingListener {
  private val viewModel: RewardsViewModel by activityViewModels()
  private val profileViewModel: ProfileViewModel by activityViewModels()
  lateinit var RewardsAdapter: RewardsAdapter
  private var layoutManager: GridLayoutManager? = null
  private lateinit var recyclerViewPagingUtil: RecyclerViewPagingUtil

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    viewModel.appManager.analyticsManagers.RewardsListScreenOpen()
    if (view == null) {
      mView = super.onCreateView(inflater, container, savedInstanceState)
      initUI()
      observers()
    }

    return mView
  }

  private fun initUI() {

    binding.toolbarTitle.click = this
    binding.toolbarTitle.tvToolbarTitle.text = "Rewards"
//    binding.rewbtn.setOnClickListener(onClickApplyRewards());
    initPagination()
  }

  private fun initPagination() {

    RewardsAdapter = RewardsAdapter(requireActivity(), this)
    layoutManager = GridLayoutManager(requireContext(), 1)
    binding.rvItems.layoutManager = layoutManager
    recyclerViewPagingUtil = RecyclerViewPagingUtil(
      binding.rvItems,
      layoutManager!!, this
    )
    binding.rvItems.adapter = RewardsAdapter
    binding.rvItems.addOnScrollListener(recyclerViewPagingUtil)
    binding.rvItems.post { // Call smooth scroll
      binding.rvItems.scrollToPosition(0)
    }
//    resetSkip()
  }


  private fun observers() {
    if (profileViewModel.isLoggedIn.get() != true) {
      findNavController().navigate(R.id.nav_login_sheet)
    }

    viewModel.getRewards().observe(viewLifecycleOwner, Observer {
      it.let {
        when (it.status) {
          Result.Status.SUCCESS -> {
            it.let {
              RewardsAdapter.setDataChanged(it.data?.data)
            }
          }
          Result.Status.ERROR -> {
//            viewModel.loading.value = false
//            viewModel.text.value = "Error"
          }

        }
      }
    })


  }

  override fun getLayoutRes(): Int {
    return R.layout.fragment_rewards
  }

  override fun permissionGranted(requestCode: Int) {

  }

  override fun onClickBack() {
    findNavController().navigateUp()
  }


  override fun onClickRewards(RewardModel: RewardItem) {
    viewModel.selectedVoucher.value = RewardModel
    findNavController().navigate(R.id.rewardsDetailFragment)
  }

  override fun onNextPage(skip: Int, take: Int) {
//
  }


  override fun onClickApplyRewards() {
    Log.e(TAG, "onClickApplyRewards:shifffaaaa ")
    Toast.makeText(context, "REWARDS", Toast.LENGTH_LONG).show()

  }


}