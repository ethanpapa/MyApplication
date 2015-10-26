package com.shin.nuleedasle.myapplication.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.shin.nuleedasle.myapplication.R;
import com.shin.nuleedasle.myapplication.models.DataModelForList;


import java.io.File;
import java.util.ArrayList;

public class DataAdapter extends RecyclerView.Adapter {
	private final int VIEW_ITEM = 1;
	private final int VIEW_PROG = 0;
	private ImageLoader mImageLoader;

	private ArrayList<DataModelForList> datamodel;

	// The minimum amount of items to have below your current scroll position
	// before loading more.
	private int visibleThreshold = 5;
	private int lastVisibleItem, totalItemCount;
	private boolean loading;
	private RequestQueue mVolleyQueue;
	private OnLoadMoreDataListener onLoadMoreDataListener;
	private static OnItemClickListener onItemClickListener;


	public static class DiskBitmapCache extends DiskBasedCache implements ImageLoader.ImageCache {

		public DiskBitmapCache(File rootDirectory, int maxCacheSizeInBytes) {
			super(rootDirectory, maxCacheSizeInBytes);
		}

		public DiskBitmapCache(File cacheDir) {
			super(cacheDir);
		}

		public Bitmap getBitmap(String url) {
			final Entry requestedItem = get(url);

			if (requestedItem == null)
				return null;

			return BitmapFactory.decodeByteArray(requestedItem.data, 0, requestedItem.data.length);
		}

		public void putBitmap(String url, Bitmap bitmap) {
			final Entry entry = new Entry();
			entry.data = BitmapUtil.convertBitmapToBytes(bitmap) ;
			put(url, entry);
		}
	}

	public void setDateModel(ArrayList<DataModelForList> modellist)
	{
		datamodel = modellist;
	}

	public DataAdapter(ArrayList<DataModelForList> modellist, RecyclerView recyclerView , Context context) {
		mVolleyQueue = Volley.newRequestQueue(context);
		int max_cache_size = 1000000;

		mImageLoader = new ImageLoader(mVolleyQueue, new DiskBitmapCache(context.getCacheDir(),max_cache_size));

		datamodel = modellist;

		if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {

			final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView
					.getLayoutManager();
					recyclerView
					.addOnScrollListener(new RecyclerView.OnScrollListener() {
						@Override
						public void onScrolled(RecyclerView recyclerView,int dx, int dy) {
							super.onScrolled(recyclerView, dx, dy);

							totalItemCount = linearLayoutManager.getItemCount();
							lastVisibleItem = linearLayoutManager
									.findLastVisibleItemPosition();
							if (!loading
									&& totalItemCount <= (lastVisibleItem + visibleThreshold)) {
								// End has been reached
								// Do something
								if (onLoadMoreDataListener != null) {
									onLoadMoreDataListener.onLoadMoreData();
								}
								loading = true;
							}
						}
					});
		}
	}

	@Override
	public int getItemViewType(int position) {
		return datamodel.get(position) != null ? VIEW_ITEM : VIEW_PROG;
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
			int viewType) {
		RecyclerView.ViewHolder vh;
		if (viewType == VIEW_ITEM) {
			View v = LayoutInflater.from(parent.getContext()).inflate(
					R.layout.list_item, parent, false);

			vh = new FlickrViewHolder(v);
		} else {
			View v = LayoutInflater.from(parent.getContext()).inflate(
					R.layout.progress_item, parent, false);

			vh = new ProgressViewHolder(v);
		}
		return vh;
	}

	@Override
	public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
		if (holder instanceof FlickrViewHolder) {

			DataModelForList item= datamodel.get(position);
			
			((FlickrViewHolder) holder).title.setText(item.getTitle());

			((FlickrViewHolder) holder).imageID = item.getImageID();

			mImageLoader.get(item.getImageUrl(),
					ImageLoader.getImageListener(((FlickrViewHolder) holder).picture, R.drawable.flickr, android.R.drawable.ic_dialog_alert),
					50, 50);
		} else {
			((ProgressViewHolder) holder).progressBar.setIndeterminate(true);
		}
	}

	public void setLoaded() {
		loading = false;
	}

	@Override
	public int getItemCount() {
		return datamodel.size();
	}

	public void setOnLoadMoreDataListener(OnLoadMoreDataListener onoadmoredatalistener) {
		this.onLoadMoreDataListener = onoadmoredatalistener;
	}

	public void setItemClickListener(OnItemClickListener listener){
		this.onItemClickListener = listener;
	}

	//
	public static class FlickrViewHolder extends RecyclerView.ViewHolder {

		public ImageView picture;
		public TextView title;
		public String imageID;

		public FlickrViewHolder(View v) {
			super(v);
			picture = (ImageView) v.findViewById(R.id.image);
			title = (TextView)v.findViewById(R.id.title);

			v.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					onItemClickListener.onItemClick(imageID);
				}
			});
		}
	}

	public static class ProgressViewHolder extends RecyclerView.ViewHolder {
		public ProgressBar progressBar;

		public ProgressViewHolder(View v) {
			super(v);
			progressBar = (ProgressBar) v.findViewById(R.id.progressBar1);
		}
	}
}