package com.smarteye.ui.identify

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class IdentifyViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is identification Fragment"
    }
    val text: LiveData<String> = _text
}