package com.example.movies.popularmovies.UI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.movies.popularmovies.Adapters.MovieAdapter;

import com.example.movies.popularmovies.Database.MovieViewModel;
import com.example.movies.popularmovies.Database.ViewModelFactory;
import com.example.movies.popularmovies.Model.Movie;
import com.example.movies.popularmovies.R;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity{
    private List<Movie> movies;
    private MovieAdapter adapter;
    public static final String TAG = MainActivity.class.getName();
    public static final String LIFE_CYCLE = "LIFE CYCLE";
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    public static final int RC_SIGN_IN = 1;
    public static final String ANONYMOUS = "anonymous";
    public ActionBar actionBar;
    private String mUserName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

         actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.getDisplayOptions();
            actionBar.setTitle(getSortValue());
        }


        Log.d(LIFE_CYCLE,"onCreate");

        mFirebaseAuth = FirebaseAuth.getInstance();

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null){
                    //user is signed in
                    Log.d(TAG, "onAuthStateChanged: " +user.getDisplayName());
                    mUserName = user.getDisplayName();

                    //if user is signed in generate Movies and pass in the username
                    generateMovies(mUserName);
                }else  {
                    onSignedOutCleanUp();
                    //user is signed out
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                          new AuthUI.IdpConfig.GoogleBuilder().build(),
                                          new AuthUI.IdpConfig.EmailBuilder().build()))
                                    .build(), RC_SIGN_IN);
                }
            }
        };
        //when the device is rotated the adapter will be null so we have to init in onCreate
        adapter = new MovieAdapter(getApplicationContext(),movies,mUserName);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN){
             if (resultCode == RESULT_OK){
                 Toast.makeText(MainActivity.this,"Signed In!",Toast.LENGTH_SHORT).show();
             } else if (resultCode == RESULT_CANCELED){
                 Toast.makeText(MainActivity.this,"Signed In  canceled!!",Toast.LENGTH_SHORT).show();
                 finish(); }
        }
    }


   
    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);


    }

    @Override
    protected void onPause() {
        super.onPause();
        
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);

    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(LIFE_CYCLE, "onStart");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_sorting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent intent = new Intent(this, Settings.class);
                startActivity(intent);
                return true;

            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
        }
        return super.onOptionsItemSelected(item);

    }

    private String getSortValue() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d("MainActivity","the preference is "+sharedPreferences.getString(getString(R.string.sort_by_key), getString(R.string.sort_popularity)));
        return sharedPreferences.getString(getString(R.string.sort_by_key), getString(R.string.sort_popularity));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(LIFE_CYCLE, "onRestart ");

        getSortValue();
        generateMovies(mUserName);
        actionBar.setTitle(getSortValue());

    }

    private void onSignedOutCleanUp(){
        mUserName = ANONYMOUS;



    }
    //TODO: GenerateMovies check username
    //TODO: In Firebase Replace username with UID
    private void generateMovies(String mUserName){
        Log.d(LIFE_CYCLE,"GENERATE ");
        movies = new ArrayList<>();
        final ViewModelFactory modelFactory = new ViewModelFactory(mUserName);
        MovieViewModel viewModel = ViewModelProviders.of(this,modelFactory).get(MovieViewModel.class);
        if (getSortValue().equals(getString(R.string.sort_by_favorites))) {

          
          viewModel.getLiveData().observe(this, new Observer<DataSnapshot>() {

              @Override
              public void onChanged(DataSnapshot dataSnapshot) {

                  if (dataSnapshot != null) {
                   //  movies.clear();
                      for (DataSnapshot itemSnapShot : dataSnapshot.getChildren()) {
                        Movie movie = itemSnapShot.getValue(Movie.class);
                          Log.d(TAG, "itemSnapShot = " + itemSnapShot.getValue().toString());

                          movies.add(movie);
                          
                         adapter.notifyDataSetChanged();

                      }
                      Log.d(TAG, "movies list: "+movies.size());

                  }
              }
          });
          
        } else if (getSortValue().equals(getString(R.string.sort_popularity))){
            viewModel.getMovies( getString(R.string.sort_popularity) ).observe(this, new Observer<List<Movie>>() {
                @Override
                public void onChanged(@Nullable List<Movie> moviesModels) {
                    movies.clear();
                    if (moviesModels != null) {
                        movies.addAll(moviesModels);
                        adapter.notifyDataSetChanged();
                    }
                }
            }); }else if (getSortValue().equals(getString(R.string.sort_top_rated))){
            viewModel.getMovies(getString(R.string.sort_top_rated)).observe(this, new Observer<List<Movie>>() {
                @Override
                public void onChanged(@Nullable List<Movie> moviesModels) {
                    movies.clear();
                    if (moviesModels != null) {
                        movies.addAll(moviesModels);
                        Log.d(TAG,"Top rated movies inserted");
                        adapter.notifyDataSetChanged();

                    }
                    for (int i =0 ; i< moviesModels.size(); i++){
                        Log.d(TAG,"Top rated movie ====> "+moviesModels.get(i).getTitle());
                    }
                }
            });
        }

        Toast.makeText(this, "Sorting by   " + getSortValue(),Toast.LENGTH_LONG).show();

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        // Calling the Adapter object and setting it to the recycler view.
        adapter = new MovieAdapter(this, movies,mUserName);

        recyclerView.setAdapter(adapter);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(layoutManager);

    }

}

