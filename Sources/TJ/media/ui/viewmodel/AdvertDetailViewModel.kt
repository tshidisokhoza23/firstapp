package com.tpsk.tj.media.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.chat.model.Advert
import com.tpsk.tj.chat.domain.repository.AdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdvertDetailViewModel @Inject constructor(
    private val repository: AdRepository
) : ViewModel() {

    private val _advert = MutableStateFlow<Resource<Advert>>(Resource.Loading())
    val advert: StateFlow<Resource<Advert>> = _advert.asStateFlow()

    fun loadAdvert(advertId: String) {
        viewModelScope.launch {
            repository.getAdverts().collect { resource ->
                if (resource is Resource.Success) {
                    val ad = resource.data?.find { it.id == advertId }
                    if (ad != null) _advert.value = Resource.Success(ad)
                    else _advert.value = Resource.Error("Advert not found")
                } else if (resource is Resource.Error) {
                    _advert.value = Resource.Error(resource.message ?: "Failed to load advert")
                }
            }
        }
    }

    fun trackClick(advertId: String) {
        viewModelScope.launch { repository.trackAdvertClick(advertId).collect {} }
    }

    fun trackShare(advertId: String) {
        viewModelScope.launch { repository.trackAdvertShare(advertId).collect {} }
    }
}
