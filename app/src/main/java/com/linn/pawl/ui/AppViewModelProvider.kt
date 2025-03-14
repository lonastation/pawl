package com.linn.pawl.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.linn.pawl.PawlApplication
import com.linn.pawl.ui.viewmodels.NfcCardViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            NfcCardViewModel(
                application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]),
                pawlApplication().container.cardRepository
            )
        }
    }
}

fun CreationExtras.pawlApplication(): PawlApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as PawlApplication)