/*
 * Copyright 2015 Rudson Lima
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.lamvv.yboxnews.view.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import java.util.ArrayList;
import java.util.List;

import io.github.lamvv.yboxnews.R;
import io.github.lamvv.yboxnews.adapter.ArticlesAdapter;
import io.github.lamvv.yboxnews.controller.FragmentController;
import io.github.lamvv.yboxnews.iml.YboxAPI;
import io.github.lamvv.yboxnews.listener.RecyclerTouchListener;
import io.github.lamvv.yboxnews.model.Article;
import io.github.lamvv.yboxnews.model.ArticleList;
import io.github.lamvv.yboxnews.util.BaseFragment;
import io.github.lamvv.yboxnews.util.ServiceGenerator;
import io.github.lamvv.yboxnews.util.VerticalLineDecorator;
import io.github.lamvv.yboxnews.view.activity.MainActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.facebook.FacebookSdk.getApplicationContext;
import static io.github.lamvv.yboxnews.R.id.recyclerView;

public class MainFragment extends BaseFragment {

	private List<Article> articles;
	private RecyclerView mRecyclerView;
	private ArticlesAdapter adapter;
	Context mContext;
	private YboxAPI api;

	private SwipeRefreshLayout mSwipeRefreshLayout;

	Toolbar mToolbar;
	MainActivity mainActivity;

	private static final String TEXT_FRAGMENT = "TEXT_FRAGMENT";

	public static MainFragment newInstance(String text) {
		MainFragment mFragment = new MainFragment();
		Bundle mBundle = new Bundle();
		mBundle.putString(TEXT_FRAGMENT, text);
		mFragment.setArguments(mBundle);
		return mFragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (containerView == null) {
			containerView = inflate.inflate(R.layout.fragment_main, null);
			containerView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			articles = new ArrayList<>();
			adapter = new ArticlesAdapter(getActivity(), articles);
		}
	}


	@Override
	public void onResume() {
		super.onResume();
		Context mContext = (MainActivity) getActivity();
//		ActionBar actionBar = ((MainActivity) mContext).getSupportActionBar();
//		actionBar.setSubtitle("Ybox " + mContext.getResources().getString(R.string.home));
		((MainActivity) mContext).setTypeHomeMenu(0);
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof MainActivity) {
			this.mainActivity = (MainActivity) context;
		}
	}

//	@Override
//	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//		View rootView = inflater.inflate(R.layout.fragment_main, container, false);
//		rootView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
//		return rootView;
//	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setHasOptionsMenu(true);
		mRecyclerView = (RecyclerView)view.findViewById(recyclerView);
		mSwipeRefreshLayout = (SwipeRefreshLayout)view.findViewById(R.id.swipeRefreshLayout);
//		mToolbar = (Toolbar)view.findViewById(R.id.toolbar);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);

		/*
		 * onRefreshLayout
		 */
		mSwipeRefreshLayout.setColorSchemeColors(Color.parseColor("#ff0000"), Color.parseColor("#00ff00"),
				Color.parseColor("#0000ff"), Color.parseColor("#f234ab"));
		mSwipeRefreshLayout.setOnRefreshListener(onRefreshListener);

		/*
		 * onLoadMore
		 */
		adapter.setLoadMoreListener(new ArticlesAdapter.OnLoadMoreListener() {
			@Override
			public void onLoadMore() {
				mRecyclerView.post(new Runnable() {
					@Override
					public void run() {
//						int index = articles.size() - 1;
						int page = articles.size()/10;
						page += 1;
						loadMore(page);
					}
				});
			}
		});

		ViewGroup.LayoutParams params = mRecyclerView.getLayoutParams();
		params.height = 100;
		mRecyclerView.setLayoutParams(params);
		mRecyclerView.setHasFixedSize(true);
		mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
		mRecyclerView.addItemDecoration(new VerticalLineDecorator(2));
		mRecyclerView.setAdapter(adapter);

		/*
		 * onItemClickListener
		 */
		mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), mRecyclerView, new RecyclerTouchListener.ClickListener() {
			@Override
			public void onClick(View view, int position) {
				Article article = articles.get(position);
				Bundle args = new Bundle();
				args.putString("detail", article.getLinks().getDetail());
				Fragment two = ArticleFragment.newInstance("Article");
				two.setArguments(args);
				FragmentController.replaceWithAddToBackStackAnimation(getActivity(), two, ArticleFragment.class.getName());
			}

			@Override
			public void onLongClick(View view, int position) {

			}
		}));

		/*mRecyclerView.setOnScrollListener(new HidingScrollListener() {
			@Override
			public void onHide() {
				mToolbar.animate().translationY(-mToolbar.getHeight()).setInterpolator(new AccelerateInterpolator(2));
			}
			@Override
			public void onShow() {
				mToolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator(2));
			}
		});*/

		api = ServiceGenerator.createService(YboxAPI.class);
		load(1);
	}

	private void load(int page){
		Call<ArticleList> call = api.getArticle(page);
		call.enqueue(new Callback<ArticleList>() {
			@Override
			public void onResponse(Call<ArticleList> call, Response<ArticleList> response) {
				if(response.isSuccessful()){
					articles.addAll(response.body().articles);
					adapter.notifyDataChanged();
				}else{
//					Log.e("lamvv"," Response Error "+String.valueOf(response.code()));
				}
			}

			@Override
			public void onFailure(Call<ArticleList> call, Throwable t) {
//				Log.e("lamvv"," Response Error "+t.getMessage());
			}
		});
	}

	private void loadMore(int page){

		//add loading progress view
		articles.add(new Article("load"));
		adapter.notifyItemInserted(articles.size()-1);

		Call<ArticleList> call = api.getArticle(page);
		call.enqueue(new Callback<ArticleList>() {
			@Override
			public void onResponse(Call<ArticleList> call, Response<ArticleList> response) {
				if(response.isSuccessful()){
					//remove loading view
					articles.remove(articles.size()-1);

					List<Article> result = response.body().articles;
					if(result.size()>0){
						//add loaded data
						articles.addAll(result);
					}else{//result size 0 means there is no more data available at server
						adapter.setMoreDataAvailable(false);
						//telling adapter to stop calling load more as no more server data available
//						Toast.makeText(mContext,"No More Data Available",Toast.LENGTH_LONG).show();
					}
					adapter.notifyDataChanged();
					//should call the custom method adapter.notifyDataChanged here to get the correct loading status
				}else{
//					Log.e("lamvv"," Load More Response Error "+String.valueOf(response.code()));
				}
			}

			@Override
			public void onFailure(Call<ArticleList> call, Throwable t) {
//				Log.e("lamvv"," Load More Response Error "+t.getMessage());
			}
		});
	}

	/**
	 * handle refresh
	 */
	private SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
		@Override
		public void onRefresh() {
			mSwipeRefreshLayout.postDelayed(new Runnable() {
				@Override
				public void run() {
					mSwipeRefreshLayout.setRefreshing(false);
					load(1);
				}
			}, 1000);
		}
	};
}
