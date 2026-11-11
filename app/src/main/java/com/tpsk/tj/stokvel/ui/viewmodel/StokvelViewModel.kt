package com.tpsk.tj.stokvel.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.stokvel.data.model.StokvelGroup
import com.tpsk.tj.stokvel.data.remote.CreateGroupRequestDto
import com.tpsk.tj.stokvel.domain.usecase.CreateStokvelUseCase
import com.tpsk.tj.stokvel.domain.usecase.GetStokvelsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StokvelViewModel @Inject constructor(
    private val getStokvelsUseCase: GetStokvelsUseCase,
    private val createStokvelUseCase: CreateStokvelUseCase
) : ViewModel() {

    private val _stokvelsState = MutableStateFlow<Resource<List<StokvelGroup>>>(Resource.Loading())
    val stokvelsState: StateFlow<Resource<List<StokvelGroup>>> = _stokvelsState

    private val _createState = MutableStateFlow<Resource<StokvelGroup>?>(null)
    val createState: StateFlow<Resource<StokvelGroup>?> = _createState

    init {
        fetchStokvels()
    }

    fun fetchStokvels() {
        viewModelScope.launch {
            getStokvelsUseCase().collect { result ->
                _stokvelsState.value = result
            }
        }
    }

    fun createStokvel(request: CreateGroupRequestDto) {
        viewModelScope.launch {
            createStokvelUseCase(request).collect { result ->
                _createState.value = result
                if (result is Resource.Success) {
                    fetchStokvels()
                }
            }
        }
    }
}
