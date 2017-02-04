package com.nibokapp.nibok.ui.fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.*
import com.nibokapp.nibok.R
import com.nibokapp.nibok.extension.*
import com.nibokapp.nibok.ui.presenter.AuthPresenter
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread

/**
 * Abstract class at the base of fragments displayed in the main activity's viewpager.
 *
 * @param authPresenter the authentication presenter used to handle user authentication.
 *                      Default one is AuthPresenter.
 */
abstract class MainActivityFragment(
        val authPresenter: AuthPresenter = AuthPresenter()
) : Fragment() {

    companion object {
        private val TAG = MainActivityFragment::class.java.simpleName

        val REQUEST_AUTHENTICATE = 1
    }

    /**
     * Layout's id for the fragment.
     */
    protected abstract val layoutId: Int

    /**
     * Main view is the RecyclerView primarily displayed in the fragment.
     */
    protected abstract val mainView: RecyclerView

    /**
     * Adapter for the main recycler view.
     */
    protected abstract val mainAdapter: RecyclerView.Adapter<*>

    /**
     * Layout manager for the main recycler view.
     */
    protected abstract val mainLayoutManager: LinearLayoutManager

    /**
     * Scroll listener for the main recycler view.
     */
    protected abstract val mainScrollListener: RecyclerView.OnScrollListener?

    /**
     * Search view is the RecyclerView used to display search results in the fragment.
     */
    protected abstract val searchView: RecyclerView

    /**
     * Adapter for the search recycler view.
     */
    protected abstract val searchAdapter: RecyclerView.Adapter<*>

    /**
     * Layout manager for the search recycler view.
     */
    protected abstract val searchLayoutManger: LinearLayoutManager

    /**
     * Scroll listener for the search recycler view.
     */
    protected abstract val searchScrollListener: RecyclerView.OnScrollListener?

    /**
     * Hint displayed in the toolbar when searching.
     */
    protected abstract val searchHint: String

    /**
     * Optional FAB displayed in the view.
     */
    open val fab: FloatingActionButton? = null

    // Currently displayed view, changes between main and search views
    private lateinit var currentView: RecyclerView


    /**
     * Fill the adapter with cached data.
     */
    abstract fun addCachedData(): Unit

    /**
     * Fill the adapter with fresh data.
     */
    abstract fun updateData(): Unit

    /**
     * Handle submission of a query.
     *
     * @param query the String containing the query
     *
     * @return true if the query was handled, false if the SearchView should handle it
     */
    abstract fun onQueryTextSubmit(query: String): Boolean

    /**
     * Handle text change from a query.
     *
     * @param newText the String representing the new query's text
     *
     * @return true if the query was handled, false if the SearchView should handle it
     */
    abstract fun onQueryTextChange(newText: String): Boolean

    /**
     * Handle a successful authentication.
     *
     * @param data the data received from onActivityResult()
     */
    abstract fun onSuccessfulAuthResult(data: Intent?)



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return container?.inflate(layoutId)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView(mainView, mainLayoutManager, mainAdapter, mainScrollListener)
        addCachedData()
        setupRecyclerView(searchView, searchLayoutManger, searchAdapter, searchScrollListener)
        currentView = mainView
    }

    override fun onStart() {
        super.onStart()
        updateData()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_menu, menu)
        val searchActionItem = menu.findItem(R.id.searchAction) ?: return
        val searchActionView = searchActionItem.actionView as? SearchView ?: return
        searchActionItem.addExpandListener(
                { onSearchOpen() },
                { onSearchClose() }
        )
        searchActionView.apply {
            queryHint = searchHint
            onQueryListener(
                    { onQueryTextSubmit(it) },
                    { onQueryTextChange(it)}
            )
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val authItem = menu.findItem(R.id.authAction) ?: return
        val authTitle = if (authPresenter.loggedUserExists()) {
            Log.d(TAG, "Offering logout option")
            val logout = getString(R.string.logout_action)
            val username = authPresenter.getLoggedUserId()
            "$logout($username)"
        } else {
            Log.d(TAG, "Offering login option")
            getString(R.string.login_action)
        }
        authItem.title = authTitle
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.searchAction -> { Log.d(TAG, "Searching") }
            R.id.refreshAction -> updateData()
            R.id.backToTopAction -> { currentView.layoutManager.scrollToPosition(0) }
            R.id.authAction -> onAuthItemSelected()
            else -> { context.toast(R.string.error_unknown_menu_action); super.onOptionsItemSelected(item) }
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_AUTHENTICATE) {
            if (resultCode == RESULT_OK) {
                onSuccessfulAuthResult(data)
            }
        }
    }

    protected fun isUserLoggedIn(): Boolean = authPresenter.loggedUserExists()

    private fun setupRecyclerView(rv: RecyclerView,
                                  rvLM: RecyclerView.LayoutManager,
                                  rvAdapter: RecyclerView.Adapter<*>,
                                  onScrollListener: RecyclerView.OnScrollListener?) {
        Log.d(TAG, "Setting up view: ${rv.getName()}")
        rv.apply {
            setHasFixedSize(true)
            layoutManager = rvLM
            adapter = rvAdapter
            onScrollListener?.let {
                clearOnScrollListeners()
                Log.d(TAG, "Adding scroll listener")
                addOnScrollListener(it)
            }
        }
    }

    private fun onSearchOpen(): Boolean {
        Log.d(TAG, "Menu search opened")
        mainView.setGone()
        fab?.setGone()
        searchView.setVisible()
        currentView = searchView
        return true // Expand view
    }

    private fun onSearchClose(): Boolean {
        Log.d(TAG, "Menu search closed")
        searchView.setGone()
        mainView.setVisible()
        fab?.let { it.post { it.setVisible() } }
        currentView = mainView
        return true // Collapse view
    }

    private fun onAuthItemSelected() {
        if (authPresenter.loggedUserExists()) {
            Log.d(TAG, "User wants to logout")
            doAsync {
                val loggedOut = authPresenter.logout()
                uiThread {
                    if (loggedOut) {
                        updateData()
                    }
                }
            }
        } else {
            context.startAuthenticateActivity()
        }
    }
}