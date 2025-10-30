package com.grocery.admin.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.grocery.admin.data.remote.dto.DashboardMetrics
import com.grocery.admin.domain.repository.DashboardRepository
import com.grocery.admin.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : BaseViewModel() {
    
    private val _dashboardMetrics = MutableLiveData<Resource<DashboardMetrics>>()
    val dashboardMetrics: LiveData<Resource<DashboardMetrics>> = _dashboardMetrics
    
    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing
    
    init {
        loadDashboardMetrics()
    }
    
    fun loadDashboardMetrics(range: String = "7d") {
        viewModelScope.launch {
            dashboardRepository.getDashboardMetrics(range)
                .onEach { result ->
                    _dashboardMetrics.value = result
                    if (result is Resource.Success || result is Resource.Error) {
                        _isRefreshing.value = false
                    }
                }
                .launchIn(viewModelScope)
        }
    }
    
    fun refreshDashboardMetrics() {
        _isRefreshing.value = true
        loadDashboardMetrics()
    }
}
