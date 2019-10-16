package com.agileburo.anytype.feature_desktop.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.agileburo.anytype.core_utils.ui.ViewState
import com.agileburo.anytype.feature_desktop.R
import com.agileburo.anytype.feature_desktop.mvvm.DesktopView
import com.agileburo.anytype.feature_desktop.mvvm.DesktopViewModel
import kotlinx.android.synthetic.main.fragment_desktop.*

class DesktopFragment : FeatureBaseFragment<ViewState<List<DesktopView>>>(R.layout.fragment_desktop) {

    private val vm by lazy {
        ViewModelProviders.of(this).get(DesktopViewModel::class.java)
    }

    private val desktopAdapter by lazy {
        DesktopAdapter(
            data = mutableListOf(),
            onDocumentClicked = { vm.onDocumentClicked() }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.state.observe(this, this)
        vm.navigation.observe(this, navObserver)
        vm.onViewCreated()
    }

    override fun render(state: ViewState<List<DesktopView>>) {
        when (state) {
            is ViewState.Init -> {
                desktopRecycler.apply {
                    layoutManager = GridLayoutManager(context, 2)
                    adapter = desktopAdapter
                }
                fab.setOnClickListener { vm.onAddNewDocumentClicked() }
                profileImage.setOnClickListener { vm.onProfileClicked() }
            }
            is ViewState.Success -> {
                desktopAdapter.update(state.data)
            }
        }
    }

    override fun injectDependencies() {
        // TODO
    }

    override fun releaseDependencies() {
        // TODO
    }
}