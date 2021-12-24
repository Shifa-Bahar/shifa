package com.lifepharmacy.application.network.endpoints

import ValidateReward
import com.google.android.gms.common.internal.safeparcel.SafeParcelable
import com.lifepharmacy.application.model.cart.CouponModel
import com.lifepharmacy.application.model.cart.ValidateCouponRequestBody
import com.lifepharmacy.application.model.general.GeneralResponseModel
import com.lifepharmacy.application.model.home.HomeResponseItem
import com.lifepharmacy.application.model.product.ProductDetails
import com.lifepharmacy.application.model.rewards.RewardItem
import com.lifepharmacy.application.model.rewards.RewardModel
import com.lifepharmacy.application.utils.URLs
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface RewardsApi {

//  @GET(URLs.REWARD_LIST)
//  suspend fun getReswardsList(): Response<RewardModel>

//  @GET(URLs.REWARD_LIST)
//  fun getRewardsList(): Call<GeneralResponseModel<ArrayList<RewardModel>>>


  @GET(URLs.REWARD_LIST)
  suspend fun getRewardsList(): Response<GeneralResponseModel<ArrayList<RewardItem>>>

//  @POST(URLs.REWARDS_VALIDATE)
//  suspend fun validateReward(@Query("coupon_code") coupon_code: Int): Response<GeneralResponseModel<ValidateReward>>


}
