package com.shin.nuleedasle.myapplication;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.shin.nuleedasle.myapplication.models.DataModelForList;
import com.shin.nuleedasle.myapplication.models.FlickrImage;
import com.shin.nuleedasle.myapplication.models.FlickrImageSize;
import com.shin.nuleedasle.myapplication.models.FlickrResponse;
import com.shin.nuleedasle.myapplication.models.FlickrResponsePhotoSize;
import com.shin.nuleedasle.myapplication.models.FlickrResponsePhotos;
import com.shin.nuleedasle.myapplication.models.FlickrSizeResponse;
import com.shin.nuleedasle.myapplication.volley_utils.GsonRequest;
import com.shin.nuleedasle.myapplication.utils.BitmapUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button mSearchButton;
    private RequestQueue mVolleyQueue;
    private ListView mListView;
    private ListViewAdapter mAdapter;
    private ProgressDialog mProgress;
    private List<DataModelForList> mDataList;
    private int mCurrentPageNumber = 1;
    private String mCurrentSearchKeyword;
    private EditText mSearchField;

    private ImageLoader mImageLoader;

    private final String TAG_REQUEST = "nuleedasle shin";

    GsonRequest<FlickrResponsePhotos> gsonPhotoObjRequest;
    GsonRequest<FlickrResponsePhotoSize> gsonSizeObjRequest;

    private int currentPage = 1;
    static int FEEDS_PER_PAGE = 20;

    public  class DiskBitmapCache extends DiskBasedCache implements ImageLoader.ImageCache {

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        actionBarSetup();

        // Initialise Volley Request Queue.
        mVolleyQueue = Volley.newRequestQueue(this);

        int max_cache_size = 1000000;
        mImageLoader = new ImageLoader(mVolleyQueue, new DiskBitmapCache(getCacheDir(),max_cache_size));

        mDataList = new ArrayList<DataModelForList>();

        mListView = (ListView) findViewById(R.id.image_list);
        mSearchButton = (Button) findViewById(R.id.send_http);
        mSearchField = (EditText)findViewById(R.id.txtKeyWord);

        mListView.setOnScrollListener(new EndlessScrollListener());
        mAdapter = new ListViewAdapter(this);
        mListView.setAdapter(mAdapter);


        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgress();
                InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                mgr.hideSoftInputFromWindow(mSearchButton.getWindowToken(), 0);
                mCurrentSearchKeyword = mSearchField.getText().toString();
                mDataList.clear();
                mCurrentPageNumber = 1;
                makeSampleHttpRequest(mCurrentSearchKeyword, mCurrentPageNumber);
            }
        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

                int position = arg2;
                long id = arg3;
                getPhotoSizeHttpRequest(mDataList.get((int)id).getImageID());
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void actionBarSetup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ActionBar ab = getSupportActionBar();
            ab.setTitle("Flickr Image Search");
        }
    }

    public void onStop() {
        super.onStop();
        if(mProgress != null)
            mProgress.dismiss();
    }

    private void makeSampleHttpRequest(String keyword,int currentPage) {

        String url = "https://api.flickr.com/services/rest";
        Uri.Builder builder = Uri.parse(url).buildUpon();
        builder.appendQueryParameter("api_key", "5e045abd4baba4bbcd866e1864ca9d7b");
        //builder.appendQueryParameter("method", "flickr.interestingness.getList");
        builder.appendQueryParameter("method", "flickr.photos.search");
        builder.appendQueryParameter("format", "json");
        builder.appendQueryParameter("nojsoncallback", "1");

        builder.appendQueryParameter("per_page","10");
        builder.appendQueryParameter("page", Integer.toString(currentPage));
        builder.appendQueryParameter("text",keyword);

        gsonPhotoObjRequest = new GsonRequest<FlickrResponsePhotos>(Request.Method.GET, builder.toString(),
                FlickrResponsePhotos.class, null, new Response.Listener<FlickrResponsePhotos>() {
            @Override
            public void onResponse(FlickrResponsePhotos response) {
                try {
                    parseFlickrImageResponse(response);
                    mAdapter.notifyDataSetChanged();

                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("JSON parse error");
                }
                stopProgress();
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                if( error instanceof NetworkError) {
                } else if( error instanceof ServerError) {
                } else if( error instanceof AuthFailureError) {
                } else if( error instanceof ParseError) {
                } else if( error instanceof NoConnectionError) {
                } else if( error instanceof TimeoutError) {
                }
                stopProgress();
                showToast(error.getMessage());
            }
        });


        gsonPhotoObjRequest.setTag(TAG_REQUEST);
        mVolleyQueue.add(gsonPhotoObjRequest);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("list", (ArrayList<? extends Parcelable>) mDataList);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState){
        mDataList = savedInstanceState.getParcelableArrayList("list");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void showProgress() {
        mProgress = ProgressDialog.show(this, "", "Loading...");
    }

    private void stopProgress() {
        if(mProgress != null)
            mProgress.cancel();
    }

    private void showToast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
    }

    private void parseFlickrImageResponse(FlickrResponsePhotos response) {
        FlickrResponse photos = response.getPhotos();
        for(int index = 0 ; index < photos.getPhotos().size(); index++) {

            FlickrImage flkrImage = photos.getPhotos().get(index);

            String imageUrl = "http://farm" + flkrImage.getFarm() + ".static.flickr.com/" + flkrImage.getServer()
                    + "/" + flkrImage.getId() + "_" + flkrImage.getSecret() + "_t.jpg";
            DataModelForList model = new DataModelForList();
            model.setImageUrl(imageUrl);
            model.setTitle(flkrImage.getTitle());
            model.setImageID(flkrImage.getId());
            mDataList.add(model);

        }
    }

    public class EndlessScrollListener implements AbsListView.OnScrollListener {

        private int visibleThreshold = FEEDS_PER_PAGE; // google return 20 at a time
        private int currentPage = 0;
        private int previousTotal = 0;
        private boolean loading = true;

        public EndlessScrollListener() {
        }

        public EndlessScrollListener(int visibleThreshold) {
            this.visibleThreshold = visibleThreshold;
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false;
                    previousTotal = totalItemCount;
                    currentPage++;
                }
            }
            if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
                mCurrentPageNumber ++;
                makeSampleHttpRequest(mCurrentSearchKeyword,mCurrentPageNumber);
                loading = true;
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }
    }


    private class ListViewAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public ListViewAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return mDataList.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item, null);
                holder = new ViewHolder();
                holder.image = (ImageView) convertView.findViewById(R.id.image);
                holder.title = (TextView) convertView.findViewById(R.id.title);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.title.setText(mDataList.get(position).getTitle());
            mImageLoader.get(mDataList.get(position).getImageUrl(),
                    ImageLoader.getImageListener(holder.image, R.drawable.flickr, android.R.drawable.ic_dialog_alert),
                    //Specify width & height of the bitmap to be scaled down when the image is downloaded.
                    50, 50);
            return convertView;
        }

        class ViewHolder {
            TextView title;
            ImageView image;
        }
    }

    private void loadPhoto(FlickrResponsePhotoSize response) {
        String imageUrl="", width ="", hight="";
        FlickrSizeResponse photoSize = response.getPhotoSize();
        for (int index = 0; index < photoSize.getPhotoSize().size(); index++)
        {
            FlickrImageSize size = photoSize.getPhotoSize().get(index);

            if (size.getLabel().contains("Medium")) {
                imageUrl = size.getSource();// .getUrl();
                width = size.getWidth();
                hight = size.getHeight();
            }
        }

        AlertDialog.Builder imageDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);

        View layout = inflater.inflate(R.layout.custom_fullimage_dialog,
                (ViewGroup) findViewById(R.id.layout_root));
        ImageView image = (ImageView) layout.findViewById(R.id.fullimage);

        mImageLoader.get(imageUrl,
                ImageLoader.getImageListener(image, R.drawable.flickr, android.R.drawable.ic_dialog_alert),
                //Specify width & height of the bitmap to be scaled down when the image is downloaded.
                Integer.parseInt(width), Integer.parseInt(hight));

        imageDialog.setView(layout);
        imageDialog.setPositiveButton("Close", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        });
        imageDialog.create();
        imageDialog.show();
    }

    private void getPhotoSizeHttpRequest(String photoId) {

        String url = "https://api.flickr.com/services/rest";
        Uri.Builder builder = Uri.parse(url).buildUpon();
        builder.appendQueryParameter("api_key", "5e045abd4baba4bbcd866e1864ca9d7b");
        builder.appendQueryParameter("method", "flickr.photos.getSizes");
        builder.appendQueryParameter("photo_id", photoId);
        builder.appendQueryParameter("format", "json");
        builder.appendQueryParameter("nojsoncallback", "1");

        gsonSizeObjRequest = new GsonRequest<FlickrResponsePhotoSize>(Request.Method.GET, builder.toString(),
                FlickrResponsePhotoSize.class, null, new Response.Listener<FlickrResponsePhotoSize>(){
            @Override
            public void onResponse(FlickrResponsePhotoSize response) {
                try {
                    loadPhoto(response);
                    //mAdapter.notifyDataSetChanged();

                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("Photo Size parse error");
                }
                stopProgress();
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                if( error instanceof NetworkError) {
                } else if( error instanceof ServerError) {
                } else if( error instanceof AuthFailureError) {
                } else if( error instanceof ParseError) {
                } else if( error instanceof NoConnectionError) {
                } else if( error instanceof TimeoutError) {
                }
                stopProgress();
                showToast(error.getMessage());
            }
        });
        gsonSizeObjRequest.setTag(TAG_REQUEST);
        mVolleyQueue.add(gsonSizeObjRequest);
    }
}
