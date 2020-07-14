/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package io.github.muntashirakon.music.mvp.presenter

import io.github.muntashirakon.music.mvp.BaseView
import io.github.muntashirakon.music.mvp.Presenter
import io.github.muntashirakon.music.mvp.PresenterImpl
import io.github.muntashirakon.music.providers.interfaces.Repository
import kotlinx.coroutines.*

import kotlin.coroutines.CoroutineContext

/**
 * Created by hemanths on 20/08/17.
 */

interface SearchView : BaseView {

    fun showData(data: MutableList<Any>)
}

interface SearchPresenter : Presenter<SearchView> {

    fun search(query: String?)

    class SearchPresenterImpl constructor(
        private val repository: Repository
    ) : PresenterImpl<SearchView>(), SearchPresenter, CoroutineScope {

        override val coroutineContext: CoroutineContext
            get() = Dispatchers.IO + job

        private var job: Job = Job()

        override fun detachView() {
            super.detachView()
            job.cancel()
        }

        override fun search(query: String?) {
            launch {
                val result = repository.search(query)
                withContext(Dispatchers.Main) { view?.showData(result) }
            }
        }
    }
}


