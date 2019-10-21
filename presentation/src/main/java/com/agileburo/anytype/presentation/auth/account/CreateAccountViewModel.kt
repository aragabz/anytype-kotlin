package com.agileburo.anytype.presentation.auth.account

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.agileburo.anytype.core_utils.common.Event
import com.agileburo.anytype.presentation.auth.model.Session
import com.agileburo.anytype.presentation.navigation.AppNavigation
import com.agileburo.anytype.presentation.navigation.SupportNavigation

class CreateAccountViewModel(
    val session: Session
) : ViewModel(), SupportNavigation<Event<AppNavigation.Command>> {

    override val navigation: MutableLiveData<Event<AppNavigation.Command>> = MutableLiveData()

    fun onCreateProfileClicked(input: String) {
        session.name = input
        navigation.postValue(Event(AppNavigation.Command.SetupNewAccountScreen))
    }
}