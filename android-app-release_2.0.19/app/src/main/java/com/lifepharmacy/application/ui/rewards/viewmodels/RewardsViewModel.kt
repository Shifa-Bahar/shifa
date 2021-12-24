package com.lifepharmacy.application.ui.rewards.viewmodels

import android.app.Application
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import com.lifepharmacy.application.base.BaseViewModel
import com.lifepharmacy.application.managers.AppManager
import com.lifepharmacy.application.model.rewards.RewardItem
import com.lifepharmacy.application.model.vouchers.VoucherMainResponse
import com.lifepharmacy.application.network.performNwOperation
import com.lifepharmacy.application.repository.RewardsRepository

class RewardsViewModel
@ViewModelInject
constructor(
  val appManager: AppManager,
  private val repository: RewardsRepository,
  application: Application
) : BaseViewModel(application) {

  var selectedVoucher = MutableLiveData<RewardItem>()
  var termsAndConditions = MutableLiveData<String>()
  var voucherMainResponseMut = MutableLiveData<VoucherMainResponse>()

  fun getRewards() =
    performNwOperation { repository.getRewards() }


//  fun getUserById() = performNwAndSaveOperation(
//  databaseQuery = { userDao.getUser() }, networkCall = {
//    userRepo.fetchUserDetail()
//  }, saveCallResult = { userDao.insertUser(it) })
}