package com.make.it.kit.librarymanager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.Client;
import com.algolia.search.saas.Index;
import com.algolia.search.saas.Query;
import com.algolia.search.saas.RequestOptions;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

//import com.algolia.search.saas.AlgoliaException;
//import com.algolia.search.saas.Client;
//import com.algolia.search.saas.Index;
//import com.algolia.search.saas.Query;
//import com.algolia.search.saas.RequestOptions;

public class MainActivity extends AppCompatActivity implements
        SearchView.OnQueryTextListener,
        NavigationView.OnNavigationItemSelectedListener
{
    //Firebase
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    //Choose Providers.
    private final List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.EmailBuilder().build(),
            new AuthUI.IdpConfig.GoogleBuilder().build(),
            new AuthUI.IdpConfig.PhoneBuilder().build()
    );
    private final Intent signIn = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false, false)
            .setLogo(R.mipmap.logo)
            .build();
    //Remoteconfig
    private final FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
    private final FirebaseRemoteConfigSettings configSettings =
            new FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(60 * 60 * 12)
                    .build();
    //Algolia
    private Index index;
    private Home searchFragment = Home.newInstance(false);
    static String ALGOLIA_API_KEY;
    private boolean searching = false;
    //Design
    private final DisplayMetrics displayMetrics = new DisplayMetrics();
    private final FragmentManager manager = this.getSupportFragmentManager();
    private final Fragment[] pages = {Home.newInstance(true), Add.newInstance(), Stats.newInstance()};
    private int currentMenuItem = R.id.nav_home;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //Fabric.with(this, new Crashlytics()); TODO Enable This
        if (auth.getCurrentUser() != null)
        {
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            remoteConfig.setConfigSettingsAsync(configSettings);
            remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
            remoteConfig.fetchAndActivate().addOnCompleteListener(this, task ->
            {
                if (task.isSuccessful())
                    ALGOLIA_API_KEY = remoteConfig.getString("algolia_api_key");
                //else {
                //Crashlytics.log("Error getting data from Remoteconfig : "); TODO Enable This
                //Crashlytics.logException(task.getException());
                //}
            });
            index = new Client(getString(R.string.algolia_application_id),
                    getString(R.string.algolia_api_key)).getIndex(getString(R.string.algolia_index));
            setContentView(R.layout.activity_main);
            NavInit();
        } else startActivityForResult(signIn, getResources()
                .getInteger(getResources().getInteger(R.integer.rc_sign_in)));
    }

    private void NavInit()
    {
        manager.beginTransaction().add(R.id.fragment_container, pages[0], "home").commit();

        Toolbar toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);

        SearchView searchView = findViewById(R.id.search);
        searchView.setOnQueryTextListener(this);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_home);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.nav_drawer_open, R.string.nav_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        statsInit();
    }

    private void statsInit()
    {
        Stats stats = (Stats) pages[2];
        db.collection("/stats/Author_Props/Authors").orderBy("Books").limit(10)
                .addSnapshotListener((snapshots, error) ->
                {
                    if (error != null && snapshots != null)
                        stats.setAuthors(Utils.extract(snapshots.getDocuments()));
                    else firebaseError(error);
                });
        db.collection("/stats/Category_Props/Categories").orderBy("Books").limit(10).addSnapshotListener((snapshots, error) ->
        {
            if (error != null && snapshots != null)
                stats.setAuthors(Utils.extract(snapshots.getDocuments()));
            else firebaseError(error);
        });
        db.document("/stats/Book_Count").addSnapshotListener((snapshot, error) ->
        {
            if (error == null && snapshot != null && snapshot.exists())
                stats.setBookCount(requireNonNull(snapshot.getLong("count"))
                        .intValue());
            else firebaseError(error);
        });
        db.document("/stats/User_props").addSnapshotListener((snapshot, error) ->
        {
            if (error == null && snapshot != null && snapshot.exists())
                stats.setUserCount(requireNonNull(snapshot.getLong("count"))
                        .intValue());
            else firebaseError(error);
        });

    }

    private void firebaseError(@Nullable Exception error)
    {
        Utils.showToast(R.string.common_error, this);
        if (error != null)
        {
            error.printStackTrace();
        }
        //Crashlytics.log("Error getting data from firestore : "); TODO Enable This
        //Crashlytics.logException(error);
    }

    @SuppressLint("SwitchIntDef")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == getResources().getInteger(R.integer.rc_sign_in))
        {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK)
            {
                setContentView(R.layout.activity_main);
                NavInit();
            } else if (response == null) startActivityForResult(signIn,
                    getResources().getInteger(R.integer.rc_sign_in));
            else
            {
                final FirebaseUiException error = response.getError();
                if (error != null) switch (error.getErrorCode())
                {
                    case ErrorCodes.NO_NETWORK:
                        alert(getText(R.string.no_network_alert_message));
                        break;
                    case ErrorCodes.PROVIDER_ERROR:
                        alert(getText(R.string.provider_alert_message));
                        break;
                    case ErrorCodes.EMAIL_MISMATCH_ERROR:
                        alert(getText(R.string.email_mismatch_alert_message));
                        break;
                    default:
                        alert(getText(R.string.unknown_error_alert_message));
                        //Crashlytics.log("Error during sign-in : "); TODO Enable This
                        //Crashlytics.logException(error);
                }
            }
        }
    }

    @Override
    public void onBackPressed()
    {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START);
        else if (searching)
        {
            final FragmentTransaction transaction = manager.beginTransaction();
            String name = Utils.getNameOfFragment(currentMenuItem);

            switch (currentMenuItem)
            {
                case R.id.nav_home:
                    transaction.replace(R.id.fragment_container, pages[0], name);
                    break;
                case R.id.nav_add:
                    transaction.replace(R.id.fragment_container, pages[1], name);
                    break;
                case R.id.nav_profile:
                    transaction.replace(R.id.fragment_container, pages[2], name);
                    break;
                case R.id.logOut:
                    signOut();
            }
            searchFragment.clearData();
            transaction.addToBackStack(null);
            transaction.commit();
        } else super.onBackPressed();
    }

    private void alert(CharSequence message)
    {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setIcon(R.drawable.ic_error)
                .setPositiveButton("Retry",
                        (dialog, which) -> startActivityForResult(signIn,
                                getResources().getInteger(R.integer.rc_sign_in)))
                .setMessage(message)
                .create()
                .show();
    }

    @Override
    public boolean onQueryTextSubmit(String s)
    {
        if (s == null || s.isEmpty() && searching) onBackPressed();
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s)
    {
        if (s == null || s.length() <= 0 || !Utils.isConnected(this)) return true;
        final AppCompatActivity This = this;
        searching = true;
        manager.beginTransaction().replace(R.id.fragment_container,
                searchFragment)
                .addToBackStack(Utils.getNameOfFragment(currentMenuItem))
                .commit();
        new Thread(() ->
        {
            try
            {
                final JSONArray result =
                        index.search(new Query(s), new RequestOptions()).getJSONArray("hits");
                ArrayList<Book> searchResults = new ArrayList<>();
                for (int i = 0; i < result.length(); i++)
                {
                    Book book = new Book(result.getJSONObject(0));
                    if (!searchResults.contains(book))
                        searchResults.add(book);
                }
                This.runOnUiThread(() ->
                {
                    if (searchResults.isEmpty())
                        Utils.showToast(R.string.no_search_result, This);
                    else searchFragment.setData(searchResults);
                });
            } catch (NullPointerException | JSONException | AlgoliaException error)
            {
                error.printStackTrace();
                //Crashlytics.log("Error during search <Async Task> : "); TODO Enable This
                //Crashlytics.logException(error);
            }
        }).start();
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem)
    {
        if (currentMenuItem == menuItem.getItemId()) return true;
        currentMenuItem = menuItem.getItemId();

        final FragmentTransaction transaction = manager.beginTransaction();
        String name = Utils.getNameOfFragment(currentMenuItem);

        switch (requireNonNull(menuItem).getItemId())
        {
            case R.id.nav_home:
                transaction.replace(R.id.fragment_container, pages[0], name);
                break;
            case R.id.nav_add:
                transaction.replace(R.id.fragment_container, pages[1], name);
                break;
            case R.id.nav_profile:
                transaction.replace(R.id.fragment_container, pages[2], name);
                break;
            case R.id.logOut:
                signOut();
            default:
                return false;
        }
        transaction.addToBackStack(null);
        transaction.commit();
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void signOut()
    {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(task -> startActivityForResult(signIn,
                        getResources().getInteger(R.integer.rc_sign_in)))
                .addOnFailureListener(e ->
                {
                    alert(getText(R.string.sign_out_failed_message));
                    //Crashlytics.log("Error during SignOut : ");
                    //Crashlytics.logException(e); TODO Enable This
                });
    }

}