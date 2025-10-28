package com.grocery.delivery.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grocery.delivery.data.dto.DeliveryAssignment
import com.grocery.delivery.data.repository.DeliveryRepository
import com.grocery.delivery.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * ViewModel for Available Orders screen
 */
@HiltViewModel
class AvailableOrdersViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository
) : ViewModel() {
    
    private val _ordersState = MutableLiveData<Resource<List<DeliveryAssignment>>>()
    val ordersState: LiveData<Resource<List<DeliveryAssignment>>> = _ordersState
    
    private val _actionState = MutableLiveData<Resource<String>>()
    val actionState: LiveData<Resource<String>> = _actionState
    
    init {
        loadAvailableOrders()
    }
    
    fun loadAvailableOrders() {
        deliveryRepository.getAvailableOrders()
            .onEach { resource ->
                _ordersState.value = resource
            }
            .launchIn(viewModelScope)
    }
    
    fun refreshOrders() {
        loadAvailableOrders()
    }
    
    fun acceptOrder(assignmentId: String, notes: String? = null) {
        deliveryRepository.acceptOrder(assignmentId, notes)
            .onEach { resource ->
                _actionState.value = resource
                if (resource is Resource.Success) {
                    // Refresh orders list after successful acceptance
                    loadAvailableOrders()
                }
            }
            .launchIn(viewModelScope)
    }
    
    fun declineOrder(assignmentId: String, reason: String? = null) {
        deliveryRepository.declineOrder(assignmentId, reason)
            .onEach { resource ->
                _actionState.value = resource
                if (resource is Resource.Success) {
                    // Refresh orders list after successful decline
                    loadAvailableOrders()
                }
            }
            .launchIn(viewModelScope)
    }
    
    fun resetActionState() {
        _actionState.value = null
    }
}
