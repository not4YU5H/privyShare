package com.privyShare.ui.sharedpref

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.privyShare.data.AppPreference
import com.privyShare.data.UserRepository
import com.privyShare.util.EncryptionHelper

class SharedPrefViewModel(application: Application) : AndroidViewModel(application) {
    private var sharedPrefs = EncryptionHelper.getSharedPrefs(application)
    private var appPreference = AppPreference(sharedPrefs)
    private var userRepository = UserRepository(appPreference)
    val userName: MutableLiveData<String> = MutableLiveData()
    val userEmail: MutableLiveData<String> = MutableLiveData()
    val snackBarMsg: MutableLiveData<String> = MutableLiveData()

    fun saveUserData() {
        userRepository.saveUserData(userName.value!!, userEmail.value!!)
        getUserName()
        getUserEmail()
        snackBarMsg.value = "Credentials saved successfully!"
    }

    fun getUserName() {
        userName.value = userRepository.getUserName()
    }

    fun getUserEmail() {
        userEmail.value = userRepository.getUserEmail()
    }

}