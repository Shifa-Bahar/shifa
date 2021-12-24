package com.lifepharmacy.application.ui.cart.fragmets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.lifepharmacy.application.R
import com.lifepharmacy.application.base.BaseFragment
import com.lifepharmacy.application.databinding.FragmentPaymentBinding
import com.lifepharmacy.application.enums.AddressChanged
import com.lifepharmacy.application.enums.AddressSelection
import com.lifepharmacy.application.enums.PaymentType
import com.lifepharmacy.application.managers.AlertManager
import com.lifepharmacy.application.managers.analytics.initiateCheckOutForListProducts
import com.lifepharmacy.application.managers.analytics.paymentScreenOpen
import com.lifepharmacy.application.model.config.DeliverySlot
import com.lifepharmacy.application.model.payment.CardMainModel
import com.lifepharmacy.application.network.Result
import com.lifepharmacy.application.ui.address.AddressSelectionActivity
import com.lifepharmacy.application.ui.address.AddressViewModel
import com.lifepharmacy.application.ui.address.dailog.ClickLayoutAddress
import com.lifepharmacy.application.ui.card.CardsAdapter
import com.lifepharmacy.application.ui.card.ClickCard
import com.lifepharmacy.application.ui.cart.adapter.*
import com.lifepharmacy.application.ui.cart.viewmodel.CartViewModel
import com.lifepharmacy.application.ui.profile.viewmodel.ProfileViewModel
import com.lifepharmacy.application.ui.utils.topbar.ClickTool
import com.lifepharmacy.application.ui.wallet.viewmodels.TopViewModel
import com.lifepharmacy.application.ui.wallet.viewmodels.WalletViewModel
import com.lifepharmacy.application.utils.universal.ConstantsUtil
import com.lifepharmacy.application.utils.universal.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.*


/**
 * A simple [Fragment] subclass.
 */
@AndroidEntryPoint
class PaymentFragment : BaseFragment<FragmentPaymentBinding>(),
  ClickCard, ClickLayoutAddress, ClickTool, ClickPaymentFragment {
  private val viewModel: CartViewModel by activityViewModels()
  private val addressViewModel: AddressViewModel by activityViewModels()
  private val walletVieModel: WalletViewModel by activityViewModels()
  private val profileViewModel: ProfileViewModel by activityViewModels()
  private val topViewModel: TopViewModel by activityViewModels()
  private lateinit var cardAdapter: CardsAdapter

  //  private lateinit var deliveryOptionAdapter: DeliveryOptionsAdapter
  private lateinit var instantShipmentProducts: CartShipmentProductAdapter
  private lateinit var expressShipmentProducts: CartShipmentProductAdapter
  private lateinit var freeGiftsShipmentProducts: CartShipmentProductAdapter
  private lateinit var instantSlots: DeliverSlotsAdapter
  private lateinit var standardSlots: StandardDeliverSlotsAdapter
  private val requestLocationPermissions =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
      var result = true
      granted.entries.forEach {
        if (!it.value) {
          result = false
          return@forEach
        }
      }
      if (result) {
        locationPermissionGranted()
      } else {
        AlertManager.permissionRequestPopup(requireActivity())
      }

    }
  private val addressContract =
    registerForActivityResult(AddressSelectionActivity.Contract()) { result ->
      result?.address?.let {
        addressViewModel.deliveredAddressMut.value = it
        addressViewModel.addressSelection = AddressSelection.NON
      }
    }

  private var isSetManualInstantSwitchListen = false

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    viewModel.appManager.analyticsManagers.paymentScreenOpen()
    mView = super.onCreateView(inflater, container, savedInstanceState)
    viewModel.offersManagers.makeProductsArray()
    initUI()
    observers()
    if (viewModel.offersManagers.cartQtyCountMut.value == null || viewModel.offersManagers.cartQtyCountMut.value == 0) {
      findNavController().navigateUp()
    }
    CoroutineScope(Dispatchers.Main.immediate).launch {
      delay(1000)
      isSetManualInstantSwitchListen = true
    }
    return mView
  }

  private fun initUI() {
    binding.lyDeliveryOptions.viewModel = viewModel
    binding.addressViewModel = addressViewModel
    binding.viewModel = viewModel
    binding.lifecycleOwner = this
    binding.lyDeliveryOptions.lifecycleOwner = this
    binding.click = this
    binding.lyAddress.clicklayout = this
    binding.lyAddress.isShipment = true
    binding.llOrderPlaced.click = this
    binding.tollBar.click = this
    binding.llReturnPolicy.viewModel = viewModel
    binding.llReturnPolicy.lifecycleOwner = this
    binding.tollBar.tvToolbarTitle.text = getString(R.string.payment)
    bindPayment()
    bindDeliveryOptions()
    bindOrderSummery()
    viewModel.instantCharge.value =
      viewModel.appManager.storageManagers.config.iNSTANTDELIVERYFEE
        ?: ConstantsUtil.INSTANT_DELIVERY_FEE
    viewModel.standardCharge.value =
      viewModel.appManager.storageManagers.config.eXPRESSDELIVERYFEE
        ?: ConstantsUtil.EXPRESS_DELIVERY_FEE
    viewModel.isInstantChecked.value = (false)
    initCardRV()
    handlePaymentMethodSelection()
    handleGetTogetherListener()
    initRVInstantShipment()
    initRVExpressShipment()
    initRVGiftShipment()
    initRVDeliverySlots()
    initRVStandardDeliverySlots()
    viewModel.codCharges.value =
      viewModel.appManager.storageManagers.config.cODCHARGES ?: ConstantsUtil.COD_CHARGES
    walletVieModel.requestCards()
    binding.isLoggedIn = profileViewModel.isLoggedIn
    if (viewModel.appManager.persistenceManager.isLoggedIn()) {
      profileViewModel.isLoggedIn.set(true)
    } else {
      profileViewModel.isLoggedIn.set(false)
    }
    viewModel.orderId = 0
  }

  private fun bindPayment() {
    viewModel.paymentDetailsTitle.value = getString(R.string.price_details)
    viewModel.paymentDetailsSubTotalTitle.value = getString(R.string.subtotal)
    binding.lyPaymentMethods.profileViewModel = profileViewModel
    binding.lyPaymentMethods.mViewModel = viewModel
    binding.lyPaymentMethods.topViewModel = topViewModel
    binding.lyPaymentMethods.lifecycleOwner = this
    viewModel.bindDefaultPaymentMethod()
    viewModel.setLastPaymentType()
  }

  override fun onResume() {
    super.onResume()
    viewModel.appManager.storageManagers.getSettings()
  }

  private fun bindDeliveryOptions() {
    binding.lyDeliveryOptions.click = this
    binding.lyDeliveryOptions.note = viewModel.note
    viewModel.note.setEditText(binding.lyDeliveryOptions.edNote)
    viewModel.isLeaveAtMyDoor.value = binding.lyDeliveryOptions.swLeaveAtDoor.isChecked
    binding.lyDeliveryOptions.swLeaveAtDoor.setOnCheckedChangeListener { _, b ->
      viewModel.isLeaveAtMyDoor.value = b
    }
  }

  private fun bindOrderSummery() {
    binding.lyOrderSummary.isPayment = true
    binding.lyOrderSummary.mViewModel = viewModel
    binding.lyOrderSummary.lifecycleOwner = this
    viewModel.calculateCompleteShipmentCharges(profileViewModel.userObjectMut.value)

  }

  private fun initRVInstantShipment() {
    instantShipmentProducts = CartShipmentProductAdapter(requireActivity())
    binding.lyDeliveryOptions.rvInstantProducts.adapter = instantShipmentProducts
  }

  private fun initRVExpressShipment() {
    expressShipmentProducts = CartShipmentProductAdapter(requireActivity())
    binding.lyDeliveryOptions.rvExpressProducts.adapter = expressShipmentProducts
  }

  private fun initRVGiftShipment() {
    freeGiftsShipmentProducts = CartShipmentProductAdapter(requireActivity())
    binding.lyDeliveryOptions.rvGiftProducts.adapter = freeGiftsShipmentProducts
  }

  private fun initRVDeliverySlots() {
    instantSlots = DeliverSlotsAdapter(requireActivity(), object : ClickDeliverySlot {
      override fun onClickSlot(slot: DeliverySlot, position: Int) {
        viewModel.setInstantSlot(slot)
        instantSlots.setItemSelected(position)
      }

    })
    binding.lyDeliveryOptions.rvSlots.adapter = instantSlots
  }

  private fun initRVStandardDeliverySlots() {
    standardSlots =
      StandardDeliverSlotsAdapter(requireActivity(), object : ClickStandardDeliverySlot {
        override fun onClickSlot(slot: DeliverySlot, position: Int) {
          viewModel.setStandardSlot(slot)
          standardSlots.setItemSelected(position)
        }

      })
    binding.lyDeliveryOptions.rvStandardSlots.adapter = standardSlots
  }

  private fun handleGetTogetherListener() {
    binding.lyDeliveryOptions.swTogether.setOnCheckedChangeListener { _, b ->
      viewModel.isInstantChecked.value = (b)
      viewModel.offersManagers.changeDeliveryOption(b)
      viewModel.doAmountCalculations(profileViewModel.userObjectMut.value)
      if (isSetManualInstantSwitchListen) {
        viewModel.isManualInstantChecked.value = !b
      }
    }
    if (viewModel.appManager.storageManagers.config.inInstantIsDefault == true) {
      binding.lyDeliveryOptions.swTogether.isChecked = true
      viewModel.isInstantChecked.value = (true)
      viewModel.offersManagers.changeDeliveryOption(true)
      viewModel.doAmountCalculations(profileViewModel.userObjectMut.value)
    } else {
      viewModel.offersManagers.changeDeliveryOption(false)
    }
  }

  private fun handlePaymentMethodSelection() {
    binding.lyPaymentMethods.rgCash.setOnCheckedChangeListener { _, b ->
      if (b) {
        viewModel.selectedPaymentType.value = PaymentType.CASH
        cardAdapter.selectedItem(-1)
        viewModel.calculateCompleteShipmentCharges(profileViewModel.userObjectMut.value)
        binding.lyDeliveryOptions.swLeaveAtDoor.isChecked = false

      }
    }
    binding.lyPaymentMethods.rgNewCard.setOnCheckedChangeListener { _, b ->
      if (b) {
        viewModel.selectedPaymentType.value = PaymentType.NEW
        cardAdapter.selectedItem(-1)
        viewModel.calculateCompleteShipmentCharges(profileViewModel.userObjectMut.value)

      }
    }
    binding.lyPaymentMethods.rgWallet.setOnCheckedChangeListener { _, b ->
      if (b) {
        viewModel.selectedPaymentType.value = PaymentType.WALLET
        cardAdapter.selectedItem(-1)
        viewModel.calculateCompleteShipmentCharges(profileViewModel.userObjectMut.value)
      }
    }
  }

  private fun initCardRV() {
    cardAdapter = CardsAdapter(requireActivity(), this)
    binding.lyPaymentMethods.rvCards.adapter = cardAdapter

  }

  private fun observerCartSelectedItems() {

    viewModel.offersManagers.instantProductsMut.observe(viewLifecycleOwner, {
      it?.let {
        instantShipmentProducts.setDataChanged(it)

      }
    })
    viewModel.offersManagers.expressProductsMut.observe(viewLifecycleOwner, {
      it?.let {
        expressShipmentProducts.setDataChanged(it)

      }
    })
    viewModel.offersManagers.freeGiftProductArrayMut.observe(viewLifecycleOwner, {
      it?.let {
        freeGiftsShipmentProducts.setDataChanged(it)

      }
    })
    viewModel.offersManagers.instantCount.observe(viewLifecycleOwner, {
      it?.let {
        if (it == 0) {
          binding.lyDeliveryOptions.swTogether.isChecked = false
          viewModel.isInstantChecked.value = (false)
          instantSlots.clearSlots()
          viewModel.appManager.storageManagers.instantDeliverySlots.clear()
        } else {
          instantSlots.setDataChanged(viewModel.appManager.storageManagers.instantDeliverySlots)
          viewModel.setInstantDefaultSelectedSlot()
        }
      }
    })
    viewModel.offersManagers.expressCount.observe(viewLifecycleOwner, {
      it?.let {
        if (it == 0) {
          standardSlots.clearSlots()
          viewModel.appManager.storageManagers.standardDeliverySlots.clear()
        } else {
          standardSlots.setDataChanged(viewModel.appManager.storageManagers.standardDeliverySlots)
          viewModel.setStandardDefaultSelectedSlot()
        }
      }
    })
    viewModel.offersManagers.cartQtyCountMut.observe(viewLifecycleOwner, {
      it?.let {
        if (it > 0) {
          viewModel.isThereCart.value = (true)
        } else {
          viewModel.isThereCart.value = (false)
        }
      }
    })
  }

  private fun observers() {
    observerCartSelectedItems()
    walletVieModel.cardsDataMut
      .observe(viewLifecycleOwner, {
        it?.let {
          when (it.status) {
            Result.Status.SUCCESS -> {
              cardAdapter.setDataChanged(it.data)
              when {
                it.data == null -> {
//                viewModel.selectedPaymentType.value = PaymentType.NEW
                }
                it.data.isEmpty() -> {
//                viewModel.selectedPaymentType.value = PaymentType.NEW
                }
                else -> {
                  if (viewModel.selectedPaymentType.value?.name?.toLowerCase(Locale.ROOT) == "card" ||
                    viewModel.selectedPaymentType.value?.name?.toLowerCase(Locale.ROOT) == "new"
                  ) {
                    cardAdapter.selectedItem(0)
                    walletVieModel.selectedCard = it.data[0]
                    topViewModel.cardMainModel = it.data[0]

                  }
                  viewModel.checkAndUpdateAccordingTopPreviousPaymentTyp()
                }
              }
            }
            Result.Status.ERROR -> {
              Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT)
                .show()
            }
            Result.Status.LOADING -> {

            }
          }
        }

      })

    addressViewModel.deliveredAddressMut.observe(viewLifecycleOwner, {
      it?.let {
        if (it.id == null) {
          addressViewModel.isThereAnyAddress.value = (false)
        } else {
          addressViewModel.isThereAnyAddress.value = (true)
          binding.lyAddress.item = it
        }
      }

    })
    viewModel.selectedPaymentType.observe(viewLifecycleOwner, {
      it?.let {
        Logger.d(
          "SelectedPaymentType",
          "${viewModel.selectedPaymentType.value?.name?.toLowerCase(Locale.ROOT)}"
        )
        when (it) {
          PaymentType.WALLET_WITH_LESS -> {
            if (!OrderWalletTopUpBottomSheet.inView && !OrderSummaryBottomSheet.inView) {
              findNavController().navigate(R.id.orderWalletTopUpBottomSheet)
            }

          }
          PaymentType.CARD -> {
            if (cardAdapter.arrayList.isNullOrEmpty()) {
              viewModel.selectedPaymentType.value = PaymentType.NEW
            } else {
              if (cardAdapter.rowIndex < 0) {
                cardAdapter.selectedItem(0)
              }
            }
          }
          PaymentType.NEW -> {
//            binding.lyPaymentMethods.lyCards.isNewCardSelected = true
          }

          PaymentType.NEW_FOR_WALLET -> {
            topViewModel.viaNewCard = true
//            binding.lyPaymentMethods.lyCardsForTopUp.isNewCardSelected = true
          }
          PaymentType.WALLET -> {
            if (!OrderSummaryBottomSheet.inView) {
              viewModel.calculateCompleteShipmentCharges(profileViewModel.userObjectMut.value)
            }

          }
          else -> {
            topViewModel.viaNewCard = false
          }
        }
      }

    })
    addressViewModel.addressChanged.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
      it?.let {
        it?.let {
          when (it) {
            AddressChanged.ADDRESS_CHANGED -> {
              findNavController().navigateUp()
            }
            AddressChanged.ADDRESS_UNCHANGED -> {

            }
          }
        }
      }
    })
  }


  override fun getLayoutRes(): Int {
    return R.layout.fragment_payment
  }

  override fun permissionGranted(requestCode: Int) {
  }

  override fun changeAddress() {
    addressContract.launch(true)
  }

  private fun locationPermissionGranted() {
    addressContract.launch(true)
  }

  override fun onCardSelect(position: Int?, cardMainModel: CardMainModel) {
    viewModel.selectedPaymentType.value = PaymentType.CARD
    topViewModel.cardMainModel = cardMainModel
    walletVieModel.selectedCard = cardMainModel
    cardAdapter.selectedItem(position)
//    Logger.d("SelectedPaymentTyp","${viewModel.selectedPaymentType.value?.name?.toLowerCase(Locale.ROOT)}")\\

    viewModel.checkAndUpdateAccordingTopPreviousPaymentTyp()

  }

  override fun onDeleteCard(position: Int?, cardMainModel: CardMainModel) {

  }

  override fun onClickBack() {
    findNavController().navigateUp()
  }

  override fun onClickProceed() {
    if (viewModel.appManager.persistenceManager.isLoggedIn()) {
      triggerMultipleEvent()
      if ((!viewModel.appManager.persistenceManager.getLoggedInUser()?.name.isNullOrBlank()
            && !viewModel.appManager.persistenceManager.getLoggedInUser()?.email.isNullOrBlank())
      ) {
        if (addressViewModel.deliveredAddressMut.value != null && addressViewModel.deliveredAddressMut.value?.id != null) {
          findNavController().navigate(R.id.orderSummaryBottomSheet)
//          placeOrderRequest()

        } else {
          requestLocationPermissions.launch(ConstantsUtil.getLocationListNormal())
        }
      } else {
        findNavController().navigate(R.id.nav_login_sheet)
      }
    } else {
      findNavController().navigate(R.id.nav_login_sheet)
    }
  }

  private fun triggerMultipleEvent() {
    viewModel.offersManagers.getAllProductsWithQTY().let {
      viewModel.appManager.analyticsManagers.initiateCheckOutForListProducts(it)
    }
//    CoroutineScope(Dispatchers.IO).launch {
//      for (item in viewModel.offersManagers.getAllProductsWithQTY()) {
//        viewModel.appManager.analyticsManagers.initiateCheckout(item.productDetails)
//      }
//    }
  }


  override fun onClickGotoOrders() {
    findNavController().navigate(R.id.nav_orders)
  }

  override fun onClickContinueShopping() {
    requireActivity().findNavController(R.id.fragment).navigate(R.id.nav_home)
  }

  override fun onClickInstantDetails() {
    viewModel.shipmentSelected = 1
    findNavController().navigate(R.id.shipmentProductList)
  }

  override fun onClickExpressDetails() {
    viewModel.shipmentSelected = 2
    findNavController().navigate(R.id.shipmentProductList)
  }

  override fun onClickGiftDetails() {
    viewModel.shipmentSelected = 3
    findNavController().navigate(R.id.shipmentProductList)
  }

  override fun onClickDeliveryInstant() {
    var title = ""
    var description = ""
    title = getString(R.string.instant_delivery)
    description = viewModel.appManager.storageManagers.config.instantInfo ?: ""
    AlertManager.showInfoAlertDialog(requireActivity(), title, description)
  }

  override fun onClickDeliveryExpress() {
    var title = ""
    var description = ""
    title = getString(R.string.express_delivery)
    description = viewModel.appManager.storageManagers.config.expressInfo ?: ""
    AlertManager.showInfoAlertDialog(requireActivity(), title, description)
  }
}

