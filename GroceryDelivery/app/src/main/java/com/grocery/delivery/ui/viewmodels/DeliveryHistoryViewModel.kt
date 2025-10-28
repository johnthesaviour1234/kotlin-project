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
 * ViewModel for Delivery History screen
 */
@HiltViewModel
class DeliveryHistoryViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository
) : ViewModel() {
    
    private val _historyState = MutableLiveData<Resource<List<DeliveryAssignment>>>()
    val historyState: LiveData<Resource<List<DeliveryAssignment>>> = _historyState
    
    private val _statsState = MutableLiveData<DeliveryStats>()
    val statsState: LiveData<DeliveryStats> = _statsState
    
    init {
        loadHistory()
    }
    
    fun loadHistory() {
        deliveryRepository.getOrderHistory(limit = 50)
            .onEach { resource ->
                _historyState.value = resource
                
                // Calculate stats when data is loaded
                if (resource is Resource.Success && resource.data != null) {
                    calculateStats(resource.data)
                }
            }
            .launchIn(viewModelScope)
    }
    
    fun refreshHistory() {
        loadHistory()
    }
    
    private fun calculateStats(deliveries: List<DeliveryAssignment>) {
        val totalDeliveries = deliveries.size
        val totalEarnings = deliveries.sumOf { 
            it.orders?.totalAmount ?: 0.0 
        }
        
        // Calculate average delivery time (if available)
        val deliveriesWithTime = deliveries.filter { 
            it.acceptedAt != null && it.updatedAt != null 
        }
        
        val avgDeliveryMinutes = if (deliveriesWithTime.isNotEmpty()) {
            deliveriesWithTime.map { assignment ->
                try {
                    val acceptedTime = java.time.Instant.parse(assignment.acceptedAt)
                    val completedTime = java.time.Instant.parse(assignment.updatedAt)
                    java.time.Duration.between(acceptedTime, completedTime).toMinutes()
                } catch (e: Exception) {
                    0L
                }
            }.average().toLong()
        } else {
            0L
        }
        
        _statsState.value = DeliveryStats(
            totalDeliveries = totalDeliveries,
            totalEarnings = totalEarnings,
            averageDeliveryTime = avgDeliveryMinutes
        )
    }
    
    data class DeliveryStats(
        val totalDeliveries: Int,
        val totalEarnings: Double,
        val averageDeliveryTime: Long
    )
}
