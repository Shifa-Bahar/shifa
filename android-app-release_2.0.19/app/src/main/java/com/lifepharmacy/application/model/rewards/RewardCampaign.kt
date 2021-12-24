package com.lifepharmacy.application.model.rewards

data class RewardCampaign(
  val amount: String,
  val banner: String,
  val compensated_by: String,
  val created_at: String,
  val created_by: Int,
  val description: String,
  val end_at: String,
  val icon: String,
  val id: Int,
  val start_at: String,
  val status: Int,
  val terms: String,
  val title: String,
  val type: Int,
  val updated_at: String
)