package com.ehealthwares.prognocare.feature.forms

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Thin holder so the dynamic-form composables can reach [FormLookupRepository]
 * through Hilt without each picker owning its own view-model state.
 */
@HiltViewModel
class FormLookupViewModel @Inject constructor(
    val repository: FormLookupRepository,
) : ViewModel()
