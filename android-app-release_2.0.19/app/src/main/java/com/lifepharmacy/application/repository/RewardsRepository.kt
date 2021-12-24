package com.lifepharmacy.application.repository

import com.lifepharmacy.application.base.BaseRepository
import com.lifepharmacy.application.network.endpoints.RewardsApi
import com.lifepharmacy.application.network.endpoints.VoucherApi
import com.lifepharmacy.application.utils.NetworkUtils
import javax.inject.Inject


class RewardsRepository
@Inject constructor(
  private val networkUtils: NetworkUtils,
  private val rewardsApi: RewardsApi
) :
  BaseRepository() {

  suspend fun getRewards() =
    getResult({ rewardsApi.getRewardsList() }, networkUtils)

//  suspend fun getRewardsValidation(coupon_code: Int) =
//    getResult({ rewardsApi.validateReward(coupon_code) }, networkUtils)

}
