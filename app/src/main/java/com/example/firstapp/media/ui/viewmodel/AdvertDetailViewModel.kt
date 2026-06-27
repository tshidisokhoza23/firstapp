package com.example.firstapp.media.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.media.model.Advert
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.media.domain.repository.AdRepository

@HiltViewModel
class AdvertDetailViewModel @Inject constructor(
    private val repository: AdRepository
) : ViewModel() {

    private val _advert = MutableStateFlow<Resource<Advert>>(Resource.Loading())
    val advert: StateFlow<Resource<Advert>> = _advert.asStateFlow()

    fun loadAdvert(advertId: Int) {
        viewModelScope.launch {
            repository.getAdverts().collect { resource ->
                if (resource is Resource.Success) {
                    val ad = resource.data?.find { it.id == advertId }
                    if (ad != null) _advert.value = Resource.Success(ad)
                    else _advert.value = Resource.Error("Advert not found")
                } else if (resource is Resource.Error) {
                    _advert.value = Resource.Error(resource.message ?: "Failed to load")
                }
            }
        }
    }
}
