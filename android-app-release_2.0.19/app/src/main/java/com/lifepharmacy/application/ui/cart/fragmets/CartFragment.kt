package com.lifepharmacy.application.ui.cart.fragmets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView.ItemAnimator
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lifepharmacy.application.R
import com.lifepharmacy.application.base.BaseFragment
import com.lifepharmacy.application.databinding.FragmentCartBinding
import com.lifepharmacy.application.enums.AddressChanged
import com.lifepharmacy.application.enums.AddressSelection
import com.lifepharmacy.application.enums.PaymentType
import com.lifepharmacy.application.managers.AlertManager
import com.lifepharmacy.application.managers.analytics.*
import com.lifepharmacy.application.model.cart.CartModel
import com.lifepharmacy.application.model.cart.CartResponseModel
import com.lifepharmacy.application.model.cart.CouponModel
import com.lifepharmacy.application.model.cart.OffersCartModel
import com.lifepharmacy.application.model.general.GeneralResponseModel
import com.lifepharmacy.application.model.home.HomeResponseItem
import com.lifepharmacy.application.model.product.ProductDetails
import com.lifepharmacy.application.network.Result
import com.lifepharmacy.application.ui.address.AddressSelectionActivity
import com.lifepharmacy.application.ui.address.AddressViewModel
import com.lifepharmacy.application.ui.address.ClickSelectedAddress
import com.lifepharmacy.application.ui.cart.adapter.*
import com.lifepharmacy.application.ui.cart.viewmodel.CartViewModel
import com.lifepharmacy.application.ui.coupones.ClickCoupon
import com.lifepharmacy.application.ui.dashboard.adapter.ClickHomeProduct
import com.lifepharmacy.application.ui.dashboard.adapter.ClickLayoutHorizontalProducts
import com.lifepharmacy.application.ui.dashboard.adapter.HomeProductAdapter
import com.lifepharmacy.application.ui.home.viewModel.HomeViewModel
import com.lifepharmacy.application.ui.offers.viewmodel.OffersViewModel
import com.lifepharmacy.application.ui.pages.fragment.PageFragment
import com.lifepharmacy.application.ui.products.viewmodel.ProductViewModel
import com.lifepharmacy.application.ui.profile.viewmodel.ProfileViewModel
import com.lifepharmacy.application.ui.utils.topbar.ClickTool
import dagger.hilt.android.AndroidEntryPoint


/**
 * A simple [Fragment] subclass.
 */
@AndroidEntryPoint
class CartFragment : BaseFragment<FragmentCartBinding>(),
  ClickCartProduct, ClickCartFragment, ClickCoupon, ClickTool, ClickHomeProduct,
  ClickLayoutHorizontalProducts, ClickOfferCartProduct, ClickPaymentDetails, ClickSelectedAddress,
  ClickOutOfStock {
  private val viewModel: CartViewModel by activityViewModels()
  private val addressViewModel: AddressViewModel by activityViewModels()
  private val vieModelProfile: ProfileViewModel by activityViewModels()
  private val homeViewModel: HomeViewModel by activityViewModels()
  private val offerVieModel: OffersViewModel by activityViewModels()
  private val productViewModel: ProductViewModel by activityViewModels()

  //  private lateinit var cartProductsAdapter: CartProductsAdapter
  private lateinit var cartOfferAdapter: CartOfferAdapter
  private lateinit var outOfStockProductAdapter: OutOfStockProductAdapter
  private lateinit var freeGiftProductAdapter: FreeGiftProductAdapter

  //  private lateinit var couponAdapter: CouponAdapter
  private lateinit var homeProductAdapter: HomeProductAdapter
  var isProceedToPayment = false
  lateinit var onBackPressedCallback: OnBackPressedCallback
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Override this method to customize back press

    onBackPressedCallback = object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        //some logic that needs to be run before fragment is destroyed
        try {
          findNavController().navigateUp()
        } catch (e: Exception) {

        }

      }
    }

    requireActivity().onBackPressedDispatcher.addCallback(

      onBackPressedCallback
    )


  }

  private val addressContract =
    registerForActivityResult(AddressSelectionActivity.Contract()) { result ->
      result?.address?.let {
        addressViewModel.deliveredAddressMut.value = it
        addressViewModel.addressSelection = AddressSelection.NON
      }
    }

  override fun onDestroyView() {
    super.onDestroyView()
    //unregister listener here
    try {
      onBackPressedCallback.isEnabled = false
      onBackPressedCallback.remove()
    } catch (e: java.lang.Exception) {
      e.printStackTrace()
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    viewModel.appManager.analyticsManagers.cartScreenOpen()
    mView = super.onCreateView(inflater, container, savedInstanceState)
    initUI()

//    viewModel.setExpressDeliveryTime()
    addressViewModel.addressChanged.value = AddressChanged.ADDRESS_UNCHANGED
    return mView
  }

  override fun onResume() {
    super.onResume()
    observers()
    viewModel.appManager.storageManagers.getSettings()
    viewModel.appManager.storageManagers.getNewAvailableSlots()
  }

  private fun initUI() {

    binding.tollBar.tvToolbarTitle.text = getString(R.string.cart)
    binding.lyPaymentDetail.click = this

    binding.layoutCoupone.click = this
    binding.layoutCoupone.isLoggedIn = vieModelProfile.isLoggedIn
    binding.click = this
    binding.viewModel = viewModel
    binding.lifecycleOwner = this
    binding.layoutLocation.click = this
    binding.tollBar.click = this
    binding.tollBar.isHeartVisible = true
    binding.layoutLocation.lifecycleOwner = this
    binding.lyRecommendations.title = getString(R.string.recommended_products)
    binding.layoutLocation.mViewModel = addressViewModel
    binding.layoutLocation.lifecycleOwner = this
    binding.layoutFreeship.mViewModel = viewModel
    binding.layoutFreeship.lifecycleOwner = this
    binding.llFreeGift.mViewModel = viewModel
    binding.llFreeGift.lifecycleOwner = this
    bindPayment()

    binding.layoutCoupone.click = this
    binding.layoutCoupone.couponText = viewModel.couponText
    viewModel.couponText.setEditText(binding.layoutCoupone.edCoupon)
    binding.layoutCoupone.viewModel = viewModel
    binding.layoutCoupone.lifecycleOwner = this
    viewModel.isCouponApplied.value = false
    binding.isLoggedIn = vieModelProfile.isLoggedIn
    addressViewModel.isSelecting.set(true)

    binding.tollBar.isBackVisible = false
    viewModel.selectedPaymentType.value = PaymentType.CARD
    binding.isLoggedIn = vieModelProfile.isLoggedIn
    initRecommendedRV()
    initCartOfferRV()
    initOutOfStockRv()
    initFreeGiftRV()
  }

  private fun bindPayment() {
    binding.lyPaymentDetail.mViewModel = viewModel
    binding.lyPaymentDetail.lifecycleOwner = this
    binding.lyPaymentDetail.mViewModel = viewModel
    binding.lyPaymentDetail.lifecycleOwner = this
    viewModel.paymentDetailsTitle.value = getString(R.string.order_summary)
    viewModel.paymentDetailsSubTotalTitle.value = getString(R.string.order_total)
  }

  private fun initCartOfferRV() {
    cartOfferAdapter = CartOfferAdapter(requireActivity(), this, this)
    binding.rvCart.adapter = cartOfferAdapter
    val animator: ItemAnimator? = binding.rvCart.itemAnimator
    if (animator is SimpleItemAnimator) {
      animator.supportsChangeAnimations = false
    }
    binding.rvCart.itemAnimator?.changeDuration = 0
  }

  private fun initRecommendedRV() {
    binding.lyRecommendations.click = this
    binding.lyRecommendations.title = getString(R.string.you_may_also_like)
    binding.lyRecommendations.txtViewViewAll.visibility = View.GONE
    homeProductAdapter =
      HomeProductAdapter(requireActivity(), this, appManager = viewModel.appManager,
        viewModel.appManager.storageManagers.config.backOrder ?: "Pre-Order")
    binding.lyRecommendations.recyclerViewProducts.adapter = homeProductAdapter
    val animator: ItemAnimator? = binding.lyRecommendations.recyclerViewProducts.itemAnimator
    if (animator is SimpleItemAnimator) {
      animator.supportsChangeAnimations = false
    }
    binding.lyRecommendations.recyclerViewProducts.itemAnimator?.changeDuration = 0
  }

  private fun initOutOfStockRv() {
    outOfStockProductAdapter = OutOfStockProductAdapter(requireActivity(), this)
    binding.llOutOfStock.recyclerViewProducts.adapter = outOfStockProductAdapter
  }

  private fun initFreeGiftRV() {
    freeGiftProductAdapter = FreeGiftProductAdapter(requireActivity(), this)
    binding.rvGifts.adapter = freeGiftProductAdapter
  }

  private fun observers() {
    homeViewModel.recommendedMut.observe(viewLifecycleOwner, Observer {
      it?.let {
        when (it.status) {
          Result.Status.SUCCESS -> {
            homeProductAdapter.setDataChanged(it.data?.products)
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
    viewModel.offersManagers.offersArrayMut.observe(viewLifecycleOwner, Observer {
      it?.let {
        if (it.size > 0) {
          viewModel.isThereCart.value = (true)
          viewModel.doAmountCalculations()
        } else {
          viewModel.isThereCart.value = (false)
        }
        if (!viewModel.offersManagers.isUpdatingFromServer) {
          initCartOfferRV()
          cartOfferAdapter.setDataChanged(it)
          refreshItemProducts()
        }
        viewModel.isCartChanged.value = !viewModel.offersManagers.cartIsNotChangedFromServer
      }
    })
    viewModel.offersManagers.cartQtyCountMut.observe(viewLifecycleOwner, Observer {
      if (!viewModel.offersManagers.isUpdatingFromServer && viewModel.offersManagers.cartIsNotChangedFromServer) {
        updateServerCart()
      }
    })
    viewModel.offersManagers.outOfProductsArrayMut.observe(viewLifecycleOwner, Observer {
      if (it.isNullOrEmpty()) {
        viewModel.isAnyOutOfStock.value = false
      } else {
        viewModel.isAnyOutOfStock.value = true
        outOfStockProductAdapter.setDataChanged(it)
      }
    })
    viewModel.offersManagers.freeGiftProductArrayMut.observe(viewLifecycleOwner, {
      if ((it.isNullOrEmpty())) {
        viewModel.isAnyFreeGift.value = false
      } else {
        viewModel.isAnyFreeGift.value = true
        freeGiftProductAdapter.setDataChanged(it)
      }
    })

  }

  private fun updateServerCart() {
    if (viewModel.appManager.persistenceManager.isThereCart()) {
      updateCartObserver()
    } else {
      createCartObserver()
    }
  }

  private fun createCartObserver() {
    viewModel.createCart().observe(viewLifecycleOwner, Observer {
      it?.let {
        when (it.status) {
          Result.Status.SUCCESS -> {
            it.data?.let { it1 -> handleCartFromServerResponse(it1) }

          }
          Result.Status.ERROR -> {
            viewModel.isUpdatingCart.value = (false)
//            Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT)
//              .show()
          }
          Result.Status.LOADING -> {
            viewModel.isUpdatingCart.value = (true)
          }
        }
      }
    })
  }

  private fun updateCartObserver() {
    viewModel.updateCart().observe(viewLifecycleOwner, Observer {
      it?.let {
        when (it.status) {
          Result.Status.SUCCESS -> {
            it.data?.let { it1 -> handleCartFromServerResponse(it1) }
          }
          Result.Status.ERROR -> {
            viewModel.isUpdatingCart.value = (true)
            createCartObserver()
          }
          Result.Status.LOADING -> {
            viewModel.isUpdatingCart.value = (true)
          }
        }
      }
    })
  }

  override fun getLayoutRes(): Int {
    return R.layout.fragment_cart
  }

  override fun permissionGranted(requestCode: Int) {

  }

  override fun onClickProceed() {
    viewModel.appManager.analyticsManagers.cartCheckOutStartedButtonClicked()
    checkOfferComplete()
  }


  override fun onClickAllSelected() {
//    viewModel.isAllSelected.get()?.let {
//      viewModel.cartManager.selectedAll(!it)
//      refreshItemProducts()
//    }
  }

  override fun onClickDeleteCart() {
    if (viewModel.offersManagers.cartQtyCountMut.value?.toInt()!! > 0) {

      MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog)
        .setTitle(resources.getString(R.string.clear_cart_title))
        .setMessage(resources.getString(R.string.clear_cart_descr))
        .setNegativeButton(resources.getString(R.string.cancel)) { _, _ ->
        }
        .setPositiveButton(resources.getString(R.string.clear_cart)) { _, _ ->
          viewModel.offersManagers.clear()
          clearCart()
        }
        .show()
    }
  }

  override fun onClickVieDetails() {
    binding.scrollviewMain.post {
      binding.scrollviewMain.fullScroll(View.FOCUS_DOWN)
    }
  }

  override fun onClickChangeAddress() {
    addressContract.launch(true)
  }

  override fun onClickApplyCoupon() {
    validateCoupon()
  }

  override fun onClickRemoveCoupon() {
    viewModel.couponModel.value = CouponModel(couponCode = "", couponExpiry = "", couponValue = 0.0)
    viewModel.isCouponApplied.value = false
    viewModel.doAmountCalculations()
  }

  override fun onClickLogin() {
    findNavController().navigate(R.id.nav_login_sheet)
  }

  override fun onClickGoShopping() {
//    findNavController().navigate(R.id.toHome)
    requireActivity().findNavController(R.id.fragment).navigate(R.id.nav_home)
  }

  override fun onCouponSelected() {
//    viewModel.discount.set("-10 AED")
  }

  override fun onClickBack() {
    findNavController().navigateUp()
  }

  override fun onProductClicked(productDetails: ProductDetails, position: Int) {
    try {
      productViewModel.position = position
      productViewModel.previewProductMut.value = productDetails
      findNavController().navigate(R.id.nav_product_preview)
    } catch (e: Exception) {

    }

//    findNavController().navigate(
//      R.id.productFragment,
//      ProductFragment.getBundle(productID = productDetails.id)
//    )
  }

  override fun onClickAddProduct(productDetails: ProductDetails, position: Int) {
    viewModel.offersManagers.addProduct(requireActivity(), productDetails, position)
//    refreshItemProducts()
  }

  override fun onClickMinus(productDetails: ProductDetails, position: Int) {
    viewModel.offersManagers.removeProduct(requireActivity(), productDetails, position)
//    changeCartAdapterViewType(productDetails)
//    cartOfferAdapter.notifyDataSetChanged()
//    refreshItemProducts()
  }

  override fun onClickRemove(productDetails: ProductDetails, position: Int) {
    viewModel.offersManagers.removeProduct(requireActivity(), productDetails, position = position)
//    changeCartAdapterViewType(productDetails)
//    cartOfferAdapter.notifyDataSetChanged()
//    refreshItemProducts()
  }

  override fun onClickProduct(productDetails: ProductDetails, position: Int) {
    try {
      productViewModel.position = position
      productViewModel.previewProductMut.value = productDetails
      findNavController().navigate(R.id.nav_product_preview)
    } catch (e: Exception) {

    }

  }

  override fun onClickChecked(offerModel: OffersCartModel, position: Int) {

  }

  override fun onClickAddMore(offerModel: OffersCartModel, position: Int) {
    offerVieModel.offersManagers.selectedOffer.value = offerModel
    findNavController().navigate(R.id.nav_offers)
  }

  override fun onClickChecked(cartModel: CartModel, position: Int) {
//    viewModel.cartManager.selectUnselected(cartModel)
  }

  override fun onClickEmptyClicked(offerModel: OffersCartModel, position: Int) {
    offerVieModel.offersManagers.selectedOffer.value = offerModel
    findNavController().navigate(R.id.nav_offers)
  }

  override fun onClickPlus(productDetails: ProductDetails, position: Int) {
    viewModel.offersManagers.addProduct(requireActivity(), productDetails)
//    changeCartAdapterViewType(productDetails)
//    cartOfferAdapter.notifyDataSetChanged()
  }

  override fun onClickWishList(productDetails: ProductDetails, position: Int) {
    viewModel.appManager.wishListManager.selectUnselected(productDetails)
  }

  override fun onClickNotify(productDetails: ProductDetails, position: Int) {
    notifyAboutProduct(productDetails)
  }

  override fun onClickOrderOutOfStock(productDetails: ProductDetails, position: Int) {
  }

  private fun notifyAboutProduct(productDetails: ProductDetails) {
    productViewModel.notifyProduct(productDetails).observe(viewLifecycleOwner, Observer {
      it?.let {
        when (it.status) {
          Result.Status.SUCCESS -> {
            hideLoading()
            it.data?.message?.let { it1 -> AlertManager.showSuccessMessage(requireActivity(), it1) }
          }
          Result.Status.ERROR -> {
            it.message?.let { it1 -> AlertManager.showErrorMessage(requireActivity(), it1) }
            hideLoading()
          }
          Result.Status.LOADING -> {
            showLoading()
          }
        }
      }
    })
  }

  private fun refreshItemProducts() {
//    cartOfferAdapter.setDataChanged(viewModel.offersManagers.offersArrayMut.value)
    homeProductAdapter.notifyDataSetChanged()
//    cartProductsAdapter.setDataChanged(viewModel.cartManager.cartItems)
  }

  private fun clearCart() {
    cartOfferAdapter.clear()
    homeProductAdapter.notifyDataSetChanged()
  }

  override fun onClickViewAll(id: String, title: String, type: String) {

  }

  override fun onLoadSectionItems(homeResponseItem: HomeResponseItem) {

  }

  override fun onClickRemove(cartModel: CartModel, position: Int) {
    viewModel.offersManagers.removeOutOfStockItem(position)
  }

  override fun onClickNotifyMe(cartModel: CartModel, position: Int) {
    notifyAboutProduct(cartModel.productDetails)
  }

  private fun validateCoupon() {
    if (viewModel.couponText.getValue()
        .isNotBlank()
    ) {
      viewModel.appManager.analyticsManagers.couponEntered(viewModel.couponText.getValue())
      viewModel.validateCoupon().observe(viewLifecycleOwner, Observer {
        it?.let {
          when (it.status) {
            Result.Status.SUCCESS -> {
              applyCoupon(it.data?.data)
              viewModel.appManager.analyticsManagers.couponApplied(viewModel.couponText.getValue())
            }
            Result.Status.ERROR -> {
              viewModel.appManager.analyticsManagers.couponDenied(viewModel.couponText.getValue())
              Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT)
                .show()
              viewModel.isCouponApplied.value = false
              viewModel.isUpdatingCart.value = (false)
            }
            Result.Status.LOADING -> {
              viewModel.isUpdatingCart.value = (true)
            }
          }
        }
      })
    }
  }

  private fun applyCoupon(couponModel: CouponModel?) {
    homeProductAdapter.notifyDataSetChanged()
    if (couponModel != null && couponModel.couponValue != 0.0 && !couponModel.couponCode.isNullOrEmpty()) {
      viewModel.isCouponApplied.value = true
      viewModel.couponModel.value = couponModel
      viewModel.isUpdatingCart.value = (false)
      viewModel.doAmountCalculations()
    }
  }

  override fun onClickKnowMore() {
    findNavController().navigate(
      R.id.pageFragment,
      PageFragment.getPageFragmentBundle("terms-and-conditions")
    )
  }

  private fun handleCartFromServerResponse(result: GeneralResponseModel<CartResponseModel>) {
    viewModel.isUpdatingCart.value = (false)
    viewModel.freeGiftMessage.value = result.data?.giftMessage
//    result.data?.activeSlots?.let {
//      viewModel.appManager.storageManagers.deliverySlots = it
//    }
    result.data?.items?.let { it1 ->
      viewModel.offersManagers.updateCartFromServer(
        requireActivity(),
        it1,
        result.data
      )
    }
    appManager.persistenceManager.saveCartID(result.data?.id.toString())
    applyCoupon(result.data?.couponModel)
    if (isProceedToPayment) {
      isProceedToPayment = false
      proceedToPayment()
    }
  }

  private fun proceedToPayment() {
    viewModel.calculateCompleteShipmentCharges(vieModelProfile.userObjectMut.value)
    try {
      findNavController().navigate(R.id.paymentFragment)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun checkOfferComplete() {
    if (!viewModel.offersManagers.isAllGroupValid()) {
      inCompleteOfferPrompt()
    } else {
      isProceedToPayment = true
      updateServerCart()
    }

  }

  private fun inCompleteOfferPrompt() {
    findNavController().navigate(R.id.offerIncompleteDialog)
//    val offer = viewModel.offersManagers.getFirstInvalidGroupOffer()
//    val title =
//      "${getString(R.string.offer_incomplete_title_first)} ${getString(R.string.buy)}${offer?.xValue}${
//        getString(R.string.get)
//      }${offer?.yValue} ${getString(R.string.offer)}"
//    MaterialAlertDialogBuilder(
//      requireContext(),
//      R.style.ThemeOverlay_App_MaterialInfoDialog
//    )
//      .setTitle(getString(R.string.offer_incomplete_title_first))
//      .setMessage(getString(R.string.offer_incomplete_dec))
//      .setPositiveButton(getString(R.string.claim_offer)) { dialog, which ->
//        viewModel.offersManagers.getFirstInvalidGroupOfferLocation()?.let {
//          binding.scrollviewMain.post { // Call smooth scroll
//            binding.scrollviewMain.scrollTo(0, binding.scrollviewMain.top * it)
//          }
//        }
//
//      }
//      .setNegativeButton(getString(R.string.proceed_without_offer)) { dialog, which ->
//        isProceedToPayment = true
//        updateServerCart()
//      }
//      .show()
  }


}



