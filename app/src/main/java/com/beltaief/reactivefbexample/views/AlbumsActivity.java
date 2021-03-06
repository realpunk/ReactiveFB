package com.beltaief.reactivefbexample.views;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.beltaief.reactivefb.ReactiveFB;
import com.beltaief.reactivefb.actions.ReactiveLogin;
import com.beltaief.reactivefb.requests.ReactiveRequest;
import com.beltaief.reactivefb.util.PermissionHelper;
import com.beltaief.reactivefbexample.R;
import com.beltaief.reactivefbexample.models.Album;
import com.beltaief.reactivefbexample.models.Photo;
import com.beltaief.reactivefbexample.util.AlbumsAdapter;
import com.beltaief.reactivefbexample.util.GsonDateTypeAdapter;
import com.beltaief.reactivefbexample.util.RecyclerViewClickListener;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

public class AlbumsActivity extends AppCompatActivity implements RecyclerViewClickListener {

    private static final String TAG = AlbumsActivity.class.getSimpleName();
    private AlbumsAdapter mAdapter;
    private List<Photo> photos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_albums);

        RecyclerView recycler = (RecyclerView) findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setNestedScrollingEnabled(false);
        mAdapter = new AlbumsAdapter(photos, this);
        recycler.setAdapter(mAdapter);

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(view -> {
            // check permissions
            boolean permissionGranted = ReactiveFB.checkPermission(PermissionHelper.USER_PHOTOS);
            if (permissionGranted) {
                getAlbums();
            } else {
                List<PermissionHelper> permissions = new ArrayList<>();
                permissions.add(PermissionHelper.USER_PHOTOS);
                requestAdditionalPermission(permissions);
            }
        });
    }

    private void requestAdditionalPermission(List<PermissionHelper> permissions) {
        ReactiveLogin.requestAdditionalPermission(permissions, this)
                .subscribe(new MaybeObserver<LoginResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "onSubscribe");
                    }

                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d(TAG, "onSuccess");
                        // verify if permission was granted
                        if (loginResult.getRecentlyDeniedPermissions()
                                .contains(PermissionHelper.USER_PHOTOS.getValue())) {
                            // permission was refused, show a toast :
                            Toast.makeText(getApplicationContext(), "We cannot get your photos " +
                                    "without your permissions", Toast.LENGTH_LONG).show();
                        } else {
                            // permission was granted, get albums
                            getAlbums();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "onError " + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete");
                    }
                });
    }

    public void getAlbums() {
        mAdapter.clear();

        String albumFields = "cover_photo,description,created_time,count";
        final String photoFields = "album,images";

        ReactiveRequest
                .getMyAlbums(albumFields) // get albums
                .map(this::parseAlbums) // parse json to list of Album
                .flatMapObservable(Observable::fromIterable) // iterate throw collection
                .flatMap(album -> ReactiveRequest.getPhoto(album.getCover().getId(), photoFields).toObservable()) // get singular album photo
                .doOnError(throwable -> Observable.empty()) // return Observable.empty if error occured
                .map(this::parsePhoto)// transform json to Photo
                .subscribe( // subscribe
                        photo -> {
                            Log.d(TAG, "onNext");
                            addPhoto(photo);
                        },
                        throwable -> Log.d(TAG, "onError " + throwable.getMessage()),
                        () -> Log.d(TAG, "onComplete")
                );
    }

    private List<Album> parseAlbums(GraphResponse response) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new GsonDateTypeAdapter())
                .create();
        String data = null;
        try {
            data = response.getJSONObject().has("data") ?
                    response.getJSONObject().get("data").toString() :
                    response.getJSONObject().toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Type listType = new TypeToken<List<Album>>() {
        }.getType();
        return gson.fromJson(data, listType);
    }

    private Photo parsePhoto(GraphResponse response) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new GsonDateTypeAdapter())
                .create();
        String data = null;
        try {
            data = response.getJSONObject().has("data") ?
                    response.getJSONObject().get("data").toString() :
                    response.getJSONObject().toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return gson.fromJson(data, Photo.class);
    }

    private void addPhoto(Photo photo) {
        mAdapter.addItem(photo);
    }


    @Override
    public void recyclerViewListClicked(View v, int position, int id) {
        // TODO open one album activity
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ReactiveLogin.onActivityResult(requestCode, resultCode, data);
    }
}
