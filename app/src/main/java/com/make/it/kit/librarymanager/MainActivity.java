package com.make.it.kit.librarymanager;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
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
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class MainActivity extends AppCompatActivity implements
        SearchView.OnQueryTextListener,
        NavigationView.OnNavigationItemSelectedListener, OnCompleteListener<QuerySnapshot>
{
    //Notification
    private static final String CHANNEL_ID = "channel_id_for_library_manager";
    private static MainActivity This;
    private static int NOTIFICATION_ID = 0;
    private static NotificationManagerCompat notificationManager;
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
    //Arbitrary constant.
    private final int RC_SIGN_IN = 300;
    //Remoteconfig
    private final FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
    private final FirebaseRemoteConfigSettings configSettings =
            new FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(60 * 60 * 12)
                    .build();
    //Algolia
    private final Client client = new Client("D77D99DE4O", "453424a3ad3532f4d5c3fb1ad2584695");
    private final Index index = client.getIndex("book_shelf");
    @SuppressLint("UseSparseArrays")
    private final HashMap<Integer, Book> searchedBooks = new HashMap<>();
    //Design
    private final DisplayMetrics displayMetrics = new DisplayMetrics();
    private final FragmentManager manager = this.getSupportFragmentManager();
    private Home searchFragment;
    private int currentMenuItem;
    private final Fragment[] pages = {null, Add.newInstance(), Stats.newInstance()};
    private Dialog loading;

    static int createNotification(CharSequence textTitle, CharSequence textContent)
    {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(This, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(textTitle)
                .setContentText(textContent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(textContent));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
        {
            builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        }
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(NOTIFICATION_ID, builder.build());
        return NOTIFICATION_ID++;
    }

    static void disposeNotification(int id)
    {
        if (notificationManager.areNotificationsEnabled())
            notificationManager.cancel(id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        This = this;
        notificationManager = NotificationManagerCompat.from(this);
        //Fabric.with(this, new Crashlytics()); TODO Enable This
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        remoteConfig.setConfigSettingsAsync(configSettings);
        remoteConfig.setDefaults(R.xml.remote_config_defaults);
        remoteConfig.fetchAndActivate().addOnCompleteListener(this, task ->
        {

            //else {
            //Crashlytics.log("Error getting data from Remoteconfig : "); TODO Enable This
            //Crashlytics.logException(task.getException());
            //}
        });
        if (auth.getCurrentUser() != null)
        {
            init();
        } else startActivityForResult(signIn, RC_SIGN_IN);
    }

    private void init()
    {
        setContentView(R.layout.activity_main);
        loading = new Dialog(this, R.style.Dialog_FrameLess);
        loading.setContentView(R.layout.progressbar);
        loading.setCancelable(false);
        loading.setCanceledOnTouchOutside(false);
        loading.show();
        NavInit();
        homeInit();
        createNotificationChannel();
    }

    private void NavInit()
    {
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

    private void homeInit()
    {
        if (pages[0] != null) return;
        CollectionReference bookRef = db.collection("Books");
        // Create a query against the collection.
        bookRef.orderBy("SavedOn", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(20).get()
                .addOnCompleteListener(this);
    }

    private void createNotificationChannel()
    {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void statsInit()
    {
        Stats stats = (Stats) pages[2];
        db.collection("/stats/Author_Props/Authors").get()
                .addOnCompleteListener(task ->
                {
                    if (task.isSuccessful() && task.getResult() != null)
                    {
                        stats.setAuthors(Utils.extract(task));
                    } else firebaseError(task.getException());
                });
        db.collection("/stats/Category_Props/Categories").get()
                .addOnCompleteListener(task ->
                {
                    if (task.isSuccessful() && task.getResult() != null)
                    {
                        stats.setCategories(Utils.extract(task));
                    } else firebaseError(task.getException());
                });
        db.document("/stats/Book_Count").get()
                .addOnCompleteListener(task ->
                {
                    if (task.isSuccessful() && task.getResult() != null)
                    {
                        stats.setBookCount(requireNonNull(task.getResult().getLong("count"))
                                .intValue());
                    } else firebaseError(task.getException());
                });

    }

    private void firebaseError(@Nullable Exception error)
    {
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

        if (requestCode == RC_SIGN_IN)
        {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK)
            {
                init();
            } else if (response == null) startActivityForResult(signIn, RC_SIGN_IN);
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
        else if (manager.findFragmentByTag(Utils.getNameOfFragment(currentMenuItem)) != null)
            manager.popBackStack(Utils.getNameOfFragment(currentMenuItem), 0);
        else super.onBackPressed();
    }

    private void alert(CharSequence message)
    {
        AlertDialog alert = new AlertDialog.Builder(this).create();
        alert.setTitle("Error");
        alert.setIcon(R.drawable.ic_error);
        alert.setButton(AlertDialog.BUTTON_POSITIVE, "Retry",
                (dialog, which) -> startActivityForResult(signIn, RC_SIGN_IN));
        alert.setMessage(message);
        alert.show();
    }

    @Override
    public boolean onQueryTextSubmit(String s)
    {
        if (searchedBooks.size() > 30) searchedBooks.clear();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s)
    {
        if (s == null || s.length() <= 0 || !Utils.isConnected(this)) return true;
        new Thread(() ->
        {
            try
            {
                final JSONArray result =
                        index.search(new Query(s), new RequestOptions()).getJSONArray("hits");
                ArrayList<Book> searchResults = new ArrayList<>();
                for (int i = 0; i < result.length(); i++)
                {
                    JSONObject object = result.getJSONObject(0);
                    if (searchedBooks.containsKey(object.getInt("objectID")))
                        searchResults.add(searchedBooks.get(object.getInt("objectID")));
                    else
                    {
                        Book book = new Book(object.getString("Name"),
                                object.getString("Author"),
                                object.getString("Category"),
                                object.getString("Photo"),
                                object.getString("PhotoRef"),
                                (float) object.getDouble("Price"));
                        searchResults.add(book);
                        searchedBooks.put(object.getInt("objectID"), book);
                    }
                }
                This.runOnUiThread(() ->
                {
                    if (searchFragment == null) searchFragment = Home.newInstance(searchResults);
                    else searchFragment.setData(searchResults);
                    manager.beginTransaction().replace(R.id.fragment_container,
                            searchFragment)
                            .addToBackStack(Utils.getNameOfFragment(currentMenuItem))
                            .commit();
                    if (searchResults.isEmpty()) Utils.showToast("No Results Found", This);
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
                .addOnCompleteListener(task -> startActivityForResult(signIn, RC_SIGN_IN))
                .addOnFailureListener(e ->
                {
                    alert(getText(R.string.sign_out_failed_message));
                    //Crashlytics.log("Error during SignOut : ");
                    //Crashlytics.logException(e); TODO Enable This
                });
    }

    @Override
    public void onComplete(@NonNull Task<QuerySnapshot> task)
    {
        if (task.isSuccessful() && task.getResult() != null)
        {
            try
            {
                final List<Book> books = task.getResult().toObjects(Book.class);
                pages[0] = Home.newInstance(books);
                manager.beginTransaction()
                        .replace(R.id.fragment_container, pages[0])
                        .commit();
            } catch (Exception error)
            {
                error.printStackTrace();
            } finally
            {
                if (loading.isShowing()) loading.dismiss();
            }
        } else firebaseError(task.getException());
    }
}