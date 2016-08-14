package com.nibokapp.nibok.ui.fragment.common

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nibokapp.nibok.R
import com.nibokapp.nibok.extension.getName
import com.nibokapp.nibok.extension.inflate
import com.nibokapp.nibok.ui.adapter.common.InfiniteScrollListener
import com.nibokapp.nibok.ui.adapter.common.ListAdapter
import com.nibokapp.nibok.ui.adapter.common.ViewType

/**
 * Base fragment for fragments representing ViewTypes.
 *
 * It handles the creation of the main view and of the search view and querying.
 */
abstract class ViewTypeFragment : BaseFragment() {

    companion object {
        private val TAG = ViewTypeFragment::class.java.simpleName
    }

    private var oldQuery: String? = null
    private var oldResults: List<ViewType>? = null

    private var mainView: RecyclerView? = null
    private var searchResultsView: RecyclerView? = null
    private var currentView: RecyclerView? = null

    /**
     * Get the layout used by the fragment.
     *
     * @return the fragment's layout id
     */
    abstract fun getFragmentLayout() : Int

    /**
     * Get the main view defined in the fragment's layout.
     *
     * @return the main view defined in the fragment's layout.
     */
    abstract fun getMainView() : RecyclerView

    /**
     * Get the layout manager used by the main view.
     *
     * @return the layout manager used by the main view
     */
    abstract fun getMainViewLayoutManager() : LinearLayoutManager

    /**
     * Get the adapter used by the main view.
     *
     * @return the adapter used by the main view
     */
    abstract fun getMainViewAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>

    /**
     * The initial set of data to display in the main view.
     */
    abstract fun getMainViewData() : List<ViewType>

    /**
     * Get the search view defined in the fragment's layout.
     *
     * @return the search view defined in the fragment's layout.
     */
    abstract fun getSearchView() : RecyclerView

    /**
     * Get the layout manager used by the search view.
     *
     * @return the layout manager used by the search view
     */
    abstract fun getSearchViewLayoutManager() : LinearLayoutManager

    /**
     * Get the adapter used by the search view.
     *
     * @return the adapter used by the search view
     */
    abstract fun getSearchViewAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>

    /**
     * The method that given a query returns a list of ViewType results.
     *
     * @return the list of ViewType representing the results of the query
     */
    abstract fun searchStrategy(query: String) : List<ViewType>

    /**
     * The loading function called when scrolling down the main view.
     */
    abstract fun onMainViewScrollDownLoader()


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return container?.inflate(getFragmentLayout())
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupMainView()
        mainView = getMainView()
        val mainViewData = getMainViewData()
        @Suppress("UNCHECKED_CAST")
        val adapter = mainView?.adapter as? ListAdapter<ViewType>
        adapter?.let {
            it.clearAndAddItems(mainViewData)
        }
    }

    override fun onBecomeVisible() {
        super.onBecomeVisible()
        showMainView()
    }

    /**
     * Scroll back to the top of the current view.
     */
    override fun handleBackToTopAction() {
        currentView?.let {
            Log.d(TAG, "Going back to top")
            it.layoutManager.scrollToPosition(0)
        }
    }

    override fun handleOnQueryTextSubmit(query: String) = handleOnQueryTextChange(query)

    override fun handleOnQueryTextChange(query: String) {
        if (query.equals(oldQuery)) {
            Log.d(TAG, "Same query as before, return")
            return
        }
        oldQuery = query

        val results = searchStrategy(query)
        if (results.equals(oldResults)) {
            Log.d(TAG, "Same results as before, return")
            return
        }
        oldResults = results

        @Suppress("UNCHECKED_CAST")
        val adapter = searchResultsView?.adapter as? ListAdapter<ViewType>
        adapter?.let {
            it.clearAndAddItems(results)
        }
    }

    override fun handleOnSearchOpen() {
        Log.d(TAG, "Search opened. Hide MainView and show SearchView")
        mainView?.visibility = View.GONE
        setupSearchView()
        searchResultsView = getSearchView()
        searchResultsView?.visibility = View.VISIBLE
        // Update the current view to be the search results view
        currentView = searchResultsView
    }

    override fun handleOnSearchClose() {
        Log.d(TAG, "Search closed. Hide SearchView and show MainView")
        showMainView()
    }

    override fun getSearchHint() : String = getString(R.string.search_hint_book) //TODO remove

    /**
     * Initial setup of a recycler view.
     * Assign layout manager and adapter, add infinite scroll listener if requested.
     *
     * @param view the recycler view to setup
     * @param viewLM the linear layout manager of the view
     * @param viewAdapter the adapter for the view
     * @param hasCustomScrollListener true if a custom scroll listener has to be added
     */
    private fun setupView(view: RecyclerView,
                          viewLM: LinearLayoutManager,
                          viewAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
                          hasCustomScrollListener: Boolean) {

        val viewName = view.getName()
        Log.d(TAG, "Setting up View: $viewName")
        view.apply {
            // Performance improvement
            setHasFixedSize(true)
            // Assign layout manager
            layoutManager = viewLM
            // Assign adapter
            if (adapter == null) {
                adapter = viewAdapter
            }
            if (hasCustomScrollListener) {
                // Add infinite scroll listener
                clearOnScrollListeners()
                addOnScrollListener(InfiniteScrollListener(viewLM) {
                    // Custom loading function executed on scroll down
                    onMainViewScrollDownLoader()
                })
            }
        }
    }

    private fun setupMainView() =
            setupView(getMainView(), getMainViewLayoutManager(), getMainViewAdapter(), true)

    private fun setupSearchView() =
            setupView(getSearchView(), getSearchViewLayoutManager(), getSearchViewAdapter(), false)

    /**
     * Hide search results view (if present) and show main view (if present).
     * Update currentView to be the mainView.
     */
    private fun showMainView() {
        searchResultsView?.visibility = View.GONE
        mainView?.visibility = View.VISIBLE
        currentView = mainView
    }
}