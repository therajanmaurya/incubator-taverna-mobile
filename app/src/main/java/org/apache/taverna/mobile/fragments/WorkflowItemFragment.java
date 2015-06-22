package org.apache.taverna.mobile.fragments;
/**
 * Apache Taverna Mobile
 * Copyright 2015 The Apache Software Foundation

 * This product includes software developed at
 * The Apache Software Foundation (http://www.apache.org/).

 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.taverna.mobile.R;
import org.apache.taverna.mobile.activities.DashboardMainActivity;
import org.apache.taverna.mobile.adapters.WorkflowAdapter;
import org.apache.taverna.mobile.tavernamobile.Workflow;
import org.apache.taverna.mobile.utils.WorkflowLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 */
public class WorkflowItemFragment extends Fragment implements android.app.LoaderManager.LoaderCallbacks<List<Workflow>>, SwipeRefreshLayout.OnRefreshListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private Animation in;
    private ProgressDialog mProgressDialog; //progressbar used to indicate the state of the workflow loaders

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    /**
     * The fragment's ListView/GridView.
     */
    private RecyclerView mListView;
    private SwipeRefreshLayout swipeRefreshLayout;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private WorkflowAdapter workflowAdapter;


    // TODO: Rename and change types of parameters
    public static WorkflowItemFragment newInstance(String param1, String param2) {
        WorkflowItemFragment fragment = new WorkflowItemFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WorkflowItemFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        in = AnimationUtils.loadAnimation(getActivity(),android.R.anim.slide_in_left);
       List<Workflow> mlist = new ArrayList<Workflow>();
    /*    mlist.add(new Workflow(getActivity(),"Testing title","Larry","Ok testing",0,"http://127.0.0.1"));
        mlist.add(new Workflow(getActivity(),"Testing title","Larry","Ok testing",0,"http://127.0.0.1"));
*/
        workflowAdapter = new WorkflowAdapter(getActivity(), mlist );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item, container, false);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh);
        swipeRefreshLayout.setOnRefreshListener(this);
        // Set the adapter
        mListView = (RecyclerView) view.findViewById(android.R.id.list);
        mListView.setHasFixedSize(true);
        mListView.setLayoutManager(new LinearLayoutManager(getActivity()));
         if(workflowAdapter.getItemCount() == 0){
            setEmptyText("No Workflows available");
            mListView.swapAdapter(workflowAdapter, true);
        }else {
            mListView.swapAdapter(workflowAdapter,true);
             mListView.setAnimation(in);
        }
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {

            ((DashboardMainActivity) activity).onSectionAttached(1);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {@link android.app.Activity#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume() {
        super.onResume();
        //Handle search actions from a system sent intent
        Intent searchIntent = getActivity().getIntent();
        if(searchIntent != null && Intent.ACTION_SEARCH.equals(searchIntent.getAction())){
            //retrieve and process query then display results
            String query = searchIntent.getStringExtra(SearchManager.QUERY);
            //Toast.makeText(getActivity(), "Query = " + query, Toast.LENGTH_SHORT).show();
            performSearch(workflowAdapter,query);
        }else
            getActivity().getLoaderManager().initLoader(0,null,this);
    }

    /**
     * Initialize the contents of the Activity's standard options menu.  You
     * should place your menu items in to <var>menu</var>.  For this method
     * to be called, you must have first called {@link #setHasOptionsMenu}.  See
     * {@link android.app.Activity#onCreateOptionsMenu(android.view.Menu) Activity.onCreateOptionsMenu}
     * for more information.
     *
     * @param menu     The options menu in which you place your items.
     * @param inflater
     * @see #setHasOptionsMenu
     * @see #onPrepareOptionsMenu
     * @see #onOptionsItemSelected
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        //menu.clear();
        if(menu.size() == 1) {
            //get the searchview and set the searchable configuration
            SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
            //assuming this activity is the searchable activity
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
            searchView.setSubmitButtonEnabled(true);
//            searchView.setIconifiedByDefault(false);
            MenuItem mit = menu.add("Refresh");
            mit.setIcon(android.R.drawable.stat_notify_sync);
            mit.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal
     * processing happen (calling the item's Runnable or sending a message to
     * its Handler as appropriate).  You can use this method for any items
     * for which you would like to do processing without those other
     * facilities.
     * <p/>
     * <p>Derived classes should call through to the base class for it to
     * perform the default menu handling.
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     * proceed, true to consume it here.
     * @see #onCreateOptionsMenu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getTitle().equals("Refresh")){

            getActivity().getLoaderManager().restartLoader(0, null, this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getChildAt(2);

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    private void performSearch(WorkflowAdapter adapter, String search){
        WorkflowAdapter ladapter = new WorkflowAdapter(getActivity());

        for(int i=0; i<adapter.getItemCount(); i++) {
            Workflow workflow = adapter.getItem(i);
            if(search.contains(workflow.getWorkflow_author()) || search.contains(workflow.getWorkflow_title())){
                ladapter.addWorkflow(workflow);
            }
        }
        mListView.swapAdapter(ladapter, true);
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param id   The ID whose loader is to be created.
     * @param args Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public android.content.Loader<List<Workflow>> onCreateLoader(int id, Bundle args) {
        swipeRefreshLayout.setRefreshing(true);
        return new WorkflowLoader(getActivity());
    }

    @Override
    public void onLoadFinished(android.content.Loader<List<Workflow>> loader, List<Workflow> workflows) {
        swipeRefreshLayout.setRefreshing(false);
        workflowAdapter = new WorkflowAdapter(getActivity(), workflows);
        mListView.swapAdapter(workflowAdapter, true);

    }

    @Override
    public void onLoaderReset(android.content.Loader<List<Workflow>> listLoader) {
        //listLoader.reset();
//        mListView.swapAdapter(null, true);
    }

    @Override
    public void onRefresh() {
        getActivity().getLoaderManager().restartLoader(0, null, this);
    }

}