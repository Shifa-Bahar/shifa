import com.google.gson.annotations.SerializedName


data class ValidateReward(

  @SerializedName("reward_campaign_id")
  val reward_campaign_id: Int,
  @SerializedName("reward_code")
  val reward_code: Int,
  @SerializedName("user_id")
  val user_id: Int,
  @SerializedName("phone")
  val phone: Int,
  @SerializedName("amount")
  val amount: Int,
  @SerializedName("type")
  val type: Int,
  @SerializedName("redeemed_at_store")
  val redeemed_at_store: Int,
  @SerializedName("updated_at")
  val updated_at: String,
  @SerializedName("created_at")
  val created_at: String,
  @SerializedName("id")
  val id: Int,
  @SerializedName("reward_campaign")
  val reward_campaign: Reward_campaign
)