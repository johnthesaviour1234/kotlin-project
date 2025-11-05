package com.grocery.delivery.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grocery.delivery.data.dto.DeliveryAssignment
import com.grocery.delivery.data.dto.OrderStatus
import com.grocery.delivery.data.repository.DeliveryRepository
import com.grocery.delivery.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * ViewModel for Active Delivery screen
 */
@HiltViewModel
class ActiveDeliveryViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository
) : ViewModel() {
    
    private val _deliveryState = MutableLiveData<Resource<DeliveryAssignment>>()
    val deliveryState: LiveData<Resource<DeliveryAssignment>> = _deliveryState
    
    private val _statusUpdateState = MutableLiveData<Resource<String>>()
    val statusUpdateState: LiveData<Resource<String>> = _statusUpdateState
    
    private var currentAssignment: DeliveryAssignment? = null
    
    fun setActiveDelivery(assignment: DeliveryAssignment) {
        currentAssignment = assignment
        _deliveryState.value = Resource.Success(assignment)
    }
    
    fun startDelivery() {
        val assignment = currentAssignment ?: return
        updateStatus(assignment.id, OrderStatus.IN_TRANSIT, "Delivery started")
    }
    
    fun markArrived() {
        val assignment = currentAssignment ?: return
        updateStatus(assignment.id, "arrived", "Driver has arrived at location")
    }
    
    fun markAsDelivered(notes: String? = null) {
        val assignment = currentAssignment ?: return
        updateStatus(assignment.id, "completed", notes ?: "Delivery completed")
    }
    
    private fun updateStatus(assignmentId: String, status: String, notes: String) {
        deliveryRepository.updateOrderStatus(assignmentId, status, notes)
            .onEach { resource ->
                _statusUpdateState.value = resource
                
                // Update current assignment status on success
                if (resource is Resource.Success && currentAssignment != null) {
                    currentAssignment = currentAssignment!!.copy(status = status)
                    _deliveryState.value = Resource.Success(currentAssignment!!)
                }
            }
            .launchIn(viewModelScope)
    }
    
    fun resetStatusUpdateState() {
        _statusUpdateState.value = null
    }
    
    fun getCurrentStatus(): String? {
        return currentAssignment?.status
    }
    
    /**
     * Refresh delivery assignment details silently (for polling).
     * Fetches latest data without showing loading state.
     */
    fun refreshDeliveryDetails(assignmentId: String) {
        deliveryRepository.getAvailableOrders()
            .onEach { resource ->
                when (resource) {
                    is Resource.Success -> {
                        // Find the specific assignment by ID
                        val assignment = resource.data?.firstOrNull { it.id == assignmentId }
                        if (assignment != null) {
                            currentAssignment = assignment
                            _deliveryState.value = Resource.Success(assignment)
                            android.util.Log.d("ActiveDeliveryViewModel", "Delivery details refreshed: status=${assignment.status}")
                        }
                    }
                    is Resource.Error -> {
                        // Silently fail - don't disrupt user experience
                        android.util.Log.e("ActiveDeliveryViewModel", "Failed to refresh delivery: ${resource.message}")
                    }
                    else -> { /* Ignore loading state */ }
                }
            }
            .launchIn(viewModelScope)
    }
}
