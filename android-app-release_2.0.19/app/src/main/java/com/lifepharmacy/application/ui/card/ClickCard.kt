package com.lifepharmacy.application.ui.card

import com.lifepharmacy.application.model.payment.CardMainModel

/**
 * Created by Zahid Ali
 */
interface ClickCard {
    fun onCardSelect(position:Int?,cardMainModel: CardMainModel)
    fun onDeleteCard(position:Int?,cardMainModel: CardMainModel)
}