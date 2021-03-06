package com.commonslab.commonslab.Activities;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import android.util.Log;
import android.view.View;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

import com.commonslab.commonslab.AudioPlayer.MediaControlsConstants;
import com.commonslab.commonslab.AudioPlayer.MediaServiceActions;
import com.commonslab.commonslab.AudioPlayer.OpusPlayer;
import com.commonslab.commonslab.AudioPlayer.StorageUtilAudioPlayer;
import com.commonslab.commonslab.BuildConfig;
import com.commonslab.commonslab.Fragments.AudioRegisterFragment;
import com.commonslab.commonslab.Fragments.MODFragment;
import com.commonslab.commonslab.Fragments.PODFragment;
import com.commonslab.commonslab.Fragments.UploadToCommonsFragment;
import com.commonslab.commonslab.R;
import com.commonslab.commonslab.AudioPlayer.MediaPlayerService;
import com.commonslab.commonslab.Utils.NetworkStatus;
import com.commonslab.commonslab.Utils.SharedPreferencesUtils.StorageUtil;
import com.commonslab.commonslab.Utils.FragmentUtils.TabbedPagerAdapter;
import com.commonslab.commonslab.VideoRecording.Camera2VideoFragment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;
import apiwrapper.commons.wikimedia.org.Commons;
import apiwrapper.commons.wikimedia.org.Enums.ContributionType;
import apiwrapper.commons.wikimedia.org.Enums.CookieStatus;
import apiwrapper.commons.wikimedia.org.Enums.MediaType;
import apiwrapper.commons.wikimedia.org.Interfaces.ContributionsCallback;
import apiwrapper.commons.wikimedia.org.Interfaces.LogoutCallback;
import apiwrapper.commons.wikimedia.org.Interfaces.RSS_FeedCallback;
import apiwrapper.commons.wikimedia.org.Models.Contribution;
import apiwrapper.commons.wikimedia.org.Models.FeedItem;
import apiwrapper.commons.wikimedia.org.Utils.UriToAbsolutePath;
import pub.devrel.easypermissions.EasyPermissions;

import static com.commonslab.commonslab.Fragments.AudioFragment.Broadcast_PLAY_NEW_AUDIO;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, EasyPermissions.PermissionCallbacks {

    public static final String SEEKBAR_UPDATE_BROADCAST = "org.wikimedia.commons.wikimedia.SEEKBAR_UPDATE_BROADCAST";
    public static final String CONTRIBUTIONS_LOADED_BROADCAST = "org.wikimedia.commons.wikimedia.CONTRIBUTIONS_LOADED_BROADCAST";
    private static final int UPLOAD_CAMERA_PERMISSION = 10;
    private static final int UPLOAD_VIDEO_PERMISSION = 11;
    private static final int UPLOAD_AUDIO_PERMISSION = 12;
    private static final int UPLOAD_GALLERY_PERMISSION = 13;
    private static final String LOAD_USER_CONTRIBUTIONS_FROM_BUNDLE = "org.wikimedia.commons.wikimedia.USER_CONTRIBUTIONS";
    public static final String SEEKBAR_UPDATE_OPUS = "org.wikimedia.commons.wikimedia.SEEKBAR_UPDATE_OPUS";
    public static final String NEXT_OPUS = "org.wikimedia.commons.wikimedia.NEXT_OPUS";
    public static final String PREVIOUS_OPUS = "org.wikimedia.commons.wikimedia.PREVIOUS_OPUS";
    public static final String PLAY_OPUS = "org.wikimedia.commons.wikimedia.PLAY_OPUS";
    public static final String PAUSE_OPUS = "org.wikimedia.commons.wikimedia.PAUSE_OPUS";
    public static final String STOP_OPUS = "org.wikimedia.commons.wikimedia.STOP_OPUS";
    private Intent seekBarUpdateIntent;
    private static final int GALLERY_REQUEST_CODE = 101;
    private static final int VIDEO_CAPTURE = 201;
    static final int IMAGE_CAPTURE = 1;

    Toolbar toolbar;
    private ViewPager viewPager;
    private TabbedPagerAdapter pagerAdapter;
    private TabLayout tabLayout;
    private TextView nav_headerTextView;
    private ImageView collapsingImageView;
    private AppBarLayout appBarLayout;
    private CollapsingToolbarLayout collapsingToolbarLayout;

    //Media player
    private AppCompatSeekBar seekBar;
    private ImageView previousTrack;
    private ImageView playPayPauseTrack;
    private ImageView nextTrack;
    private ImageView stopPlayer;
    private RelativeLayout playerContainerLayout;

    private boolean isPlyingAudio = false;

    private OpusPlayer opusPlayer;
    private int playingAudioIndex;
    private ArrayList<Contribution> playingAudioList;
    //FAB, upload section
    FloatingActionMenu uploadMenu;

    private MediaPlayerService player;
    boolean serviceBound = false;

    //Video recording
    Uri videoUri;
    private File videoFile;
    String capturedPhotoPath;
    private int statusBatColor;

    private StorageUtil storage;
    private Commons commons;
    private ArrayList<Contribution> contributions;
    private String username;
    private String limit = "max"; // number of contributions to load

    private ArrayList<FeedItem> pictureOfTheDayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("set content view");
        seekBarUpdateIntent = new Intent(SEEKBAR_UPDATE_BROADCAST);//for audio player to send seekbar progress changes
        //TODO: registerMediaControlsReceiver();
        //TODO: registerOPUSMediaControlsReceiver();

        //register seekBarReceiver
        registerReceiver(seekBarReceiver, new IntentFilter(MediaPlayerService.SEEKBAR_UPDATE));

        initViewPagerAndTabs();
        setupNavView();
        setupPlayerActions();
        setupUploadMenu();
        getStatusBatColor();

        storage = new StorageUtil(MainActivity.this);

        commons = new Commons(getApplicationContext(), CookieStatus.DISABLED);
        username = storage.loadUserCredentials();
        if (username == null) {
            storage.clearUserSession();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }
        nav_headerTextView.setText(username);
        setupCollapsingImage();

        /**
         * Check if data are stored in savedInstanceState
         * Check whether we're recreating a previously destroyed instance
         */
        if (savedInstanceState != null) {
            //userDataList exists
            //Restore data from saved state
            boolean loadContributions = savedInstanceState.getBoolean(LOAD_USER_CONTRIBUTIONS_FROM_BUNDLE);
            if (loadContributions)
                contributions = storage.retrieveUserContributions();//no need to load from network ... load form cache
        } else {
            storage.setUserContributionsStatus(false);//set a flag that Contributions need to load

            /**
             * Delete the flowing section
             * used for testing
             *
             */

            /**
             //delete this username assigning
             username = "Thalie_Envolée"; //test user for audio files
             loadtestContributions(username, limit, false);
             username = "Ilya_at_Simpleshow_Foundation";//test user for video files
             loadtestContributions(username, limit, false);
             //username = "Operationundies"; //test user for images
             username = "Martin_Falbisoner"; //test user for images
             loadtestContributions(username, limit, true);
             */

            loadContributions(username, limit);
        }

        loadPictureOfTheDay();
    }

    private void setupCollapsingImage() {
        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar_layout);

        //Fade out CollapsingToolbarLayout title ...from white to transparent
        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        Typeface type = Typeface.createFromAsset(getAssets(), "fonts/open_sans/OpenSans-Light.ttf");
        collapsingToolbarLayout.setExpandedTitleTypeface(type);//fonts
        collapsingToolbarLayout.setCollapsedTitleTypeface(type);

        collapsingToolbarLayout.setTitle(getString(R.string.pod));
        collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(R.color.white_opacity));
        collapsingToolbarLayout.setCollapsedTitleTextColor(getResources().getColor(R.color.white_opacity_king));

        //Set Picture of the day to collapsing ImageView
        collapsingImageView = (ImageView) findViewById(R.id.collapsing_ImageView);

        pictureOfTheDayList = storage.retrieveRSS_Feed(MediaType.PICTURE);
        if (pictureOfTheDayList != null &&
                pictureOfTheDayList.size() > 0 &&
                pictureOfTheDayList.get(0) != null &&
                pictureOfTheDayList.get(0).getMediaLink() != null)
            Glide.with(this)
                    .load(pictureOfTheDayList.get(0).getMediaLink())
                    .centerCrop()
                    .into(collapsingImageView);
    }


    private void setupUploadMenu() {
        uploadMenu = (FloatingActionMenu) findViewById(R.id.upload_menu);
        uploadMenu.setClosedOnTouchOutside(true);
        FloatingActionButton pictureFAB = (FloatingActionButton) findViewById(R.id.upload_image_fab);
        FloatingActionButton videoFAB = (FloatingActionButton) findViewById(R.id.upload_video_fab);
        FloatingActionButton audioFAB = (FloatingActionButton) findViewById(R.id.upload_audio_fab);
        FloatingActionButton galleryFAB = (FloatingActionButton) findViewById(R.id.upload_gallery_fab);
        pictureFAB.setOnClickListener(clickListenerFAB);
        videoFAB.setOnClickListener(clickListenerFAB);
        audioFAB.setOnClickListener(clickListenerFAB);
        galleryFAB.setOnClickListener(clickListenerFAB);
    }

    private View.OnClickListener clickListenerFAB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.upload_image_fab:
                    String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    if (EasyPermissions.hasPermissions(MainActivity.this, permissions)) {
                        // Already have permission
                        uploadMenu.close(true);
                        takePictureIntent();
                    } else {
                        // Do not have permissions, request permissions
                        EasyPermissions.requestPermissions(MainActivity.this,
                                getString(R.string.image_capture_request),
                                R.string.allow_permission,
                                R.string.deny_permission,
                                UPLOAD_CAMERA_PERMISSION,
                                permissions);
                    }
                    break;
                case R.id.upload_video_fab:
//                    String[] videoPermissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
//                    if (EasyPermissions.hasPermissions(MainActivity.this, videoPermissions)) {
//                        // Already have permission
//                        uploadMenu.close(true);
//                        startRecordingVideo();
//                    } else {
//                        // Do not have permissions, request permissions
//                        EasyPermissions.requestPermissions(MainActivity.this,
//                                getString(R.string.image_video_request),
//                                R.string.allow_permission,
//                                R.string.deny_permission,
//                                UPLOAD_VIDEO_PERMISSION,
//                                videoPermissions);
//                    }

                    uploadMenu.close(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        getFragmentManager().beginTransaction()
                                .replace(R.id.drawer_layout, Camera2VideoFragment.newInstance())
                                .commit();
                    } else {
                        showToastMessage(getString(R.string.cant_record_in_suported_format));
                    }
                    break;
                case R.id.upload_audio_fab:
                    String[] audioPermissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    if (EasyPermissions.hasPermissions(MainActivity.this, audioPermissions)) {
                        // Already have permission
                        uploadMenu.close(true);
                        Fragment audioRegisterFragment = AudioRegisterFragment.newInstance();
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.drawer_layout, audioRegisterFragment, "AudioRegisterFragment");// give your fragment container id in first parameter
                        transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                        transaction.commit();
                    } else {
                        // Do not have permissions, request permissions
                        EasyPermissions.requestPermissions(MainActivity.this,
                                getString(R.string.image_audio_request),
                                R.string.allow_permission,
                                R.string.deny_permission,
                                UPLOAD_AUDIO_PERMISSION,
                                audioPermissions);
                    }
                    break;
                case R.id.upload_gallery_fab:
                    String[] galleryPermissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
                    if (EasyPermissions.hasPermissions(MainActivity.this, galleryPermissions)) {
                        // Already have permission
                        uploadMenu.close(true);
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                        startActivityForResult(Intent.createChooser(intent,
                                "Upload to Commons"), GALLERY_REQUEST_CODE);
                    } else {
                        // Do not have permissions, request permissions
                        EasyPermissions.requestPermissions(MainActivity.this,
                                getString(R.string.image_gallery_request),
                                R.string.allow_permission,
                                R.string.deny_permission,
                                UPLOAD_GALLERY_PERMISSION,
                                galleryPermissions);
                    }
                    break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == VIDEO_CAPTURE) {
                File file = new File(UriToAbsolutePath.getPath(getApplication(), data.getData()));
                Fragment uploadToCommonsFragment = UploadToCommonsFragment.newInstance(file.getAbsolutePath(), false, ContributionType.VIDEO);
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.drawer_layout, uploadToCommonsFragment, "UploadToCommonsFragment");// give your fragment container id in first parameter
                transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                transaction.commit();
            } else if (requestCode == IMAGE_CAPTURE) {
                if (capturedPhotoPath != null) {
                    Fragment uploadToCommonsFragment = UploadToCommonsFragment.newInstance(capturedPhotoPath, true, ContributionType.IMAGE);
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.drawer_layout, uploadToCommonsFragment, "UploadToCommonsFragment");// give your fragment container id in first parameter
                    transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                    transaction.commit();
                } else {
                    showToastMessage(getString(R.string.failed_capturing_image));
                }
            } else if (requestCode == GALLERY_REQUEST_CODE) {
                File file = new File(UriToAbsolutePath.getPath(getApplication(), data.getData()));
                if (file.exists()) {
                    Fragment uploadToCommonsFragment = UploadToCommonsFragment.newInstance(file.getAbsolutePath(), false, ContributionType.IMAGE);
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.drawer_layout, uploadToCommonsFragment, "UploadToCommonsFragment");// give your fragment container id in first parameter
                    transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                    transaction.commit();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeNotification();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();
        }

        //Clear all player data from cache
        StorageUtilAudioPlayer storage = new StorageUtilAudioPlayer(MainActivity.this);
        storage.clearAllPlayerData();
        new StorageUtil(MainActivity.this).setUserContributionsStatus(false);//clear the flag that Contributions were received
    }

    private void setupNavView() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null);

        View header = navigationView.getHeaderView(0);
        nav_headerTextView = (TextView) header.findViewById(R.id.nav_headerTextView);
    }

    //Initialize ViewPager & Tabs with Fragments
    private void initViewPagerAndTabs() {
        viewPager = findViewById(R.id.viewPager);
        pagerAdapter = new TabbedPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        tabLayout = findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
        customizeTabFont();
    }

    private void customizeTabFont() {
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/open_sans/OpenSans-Regular.ttf");
        ViewGroup viewGroup = (ViewGroup) tabLayout.getChildAt(0);
        int tabsCount = viewGroup.getChildCount();
        for (int j = 0; j < tabsCount; j++) {
            ViewGroup viewGroupTab = (ViewGroup) viewGroup.getChildAt(j);
            int tabCount = viewGroupTab.getChildCount();
            for (int i = 0; i < tabCount; i++) {
                View tabViewChild = viewGroupTab.getChildAt(i);
                if (tabViewChild instanceof TextView) {
                    ((TextView) tabViewChild).setTypeface(typeface, Typeface.NORMAL);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (uploadMenu.isOpened()) {
            uploadMenu.close(true);
        } else {
            super.onBackPressed();
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_pod) {
            //Picture of the day
            Fragment uploadToCommonsFragment = PODFragment.newInstance();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.drawer_layout, uploadToCommonsFragment, "PODFragment");// give your fragment container id in first parameter
            transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
            transaction.commit();

        } else if (id == R.id.nav_mod) {
            //Media of the day
            Fragment uploadToCommonsFragment = MODFragment.newInstance();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.drawer_layout, uploadToCommonsFragment, "MODFragment");// give your fragment container id in first parameter
            transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
            transaction.commit();

        } else if (id == R.id.nav_logout) {
            commons.userLogout(new LogoutCallback() {
                @Override
                public void onLogoutSuccessful() {
                    storage.clearUserSession();
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                }

                @Override
                public void onFailure() {
                    showToastMessage(getString(R.string.something_went_wrong));
                }
            });
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    /**
     * Audio player methods and setup
     */
    private void setupPlayerActions() {
        seekBar = (AppCompatSeekBar) findViewById(R.id.appCompatSeekBarPlayer);
        seekBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.record_btn_color), PorterDuff.Mode.SRC_IN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            seekBar.getThumb().setColorFilter(getResources().getColor(R.color.record_btn_color), PorterDuff.Mode.SRC_IN);
        }

        previousTrack = (ImageView) findViewById(R.id.media_action_previous);
        playPayPauseTrack = (ImageView) findViewById(R.id.media_action_PlayPause);
        nextTrack = (ImageView) findViewById(R.id.media_action_next);
        stopPlayer = (ImageView) findViewById(R.id.media_action_exit);
        playerContainerLayout = (RelativeLayout) findViewById(R.id.playerContainerLayout);


        previousTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //code  used to play next tract in service, not used after adding OpusPlayer
                //Send a broadcast to the service
                //Intent broadcastIntent = new Intent(MediaControlsConstants.MEDIA_CONTROLS_ACTION_PREVIOUS);
                //sendBroadcast(broadcastIntent);

                if (playingAudioList != null && playingAudioIndex >= 0) {
                    if (--playingAudioIndex == -1) {
                        //if last in playlist
                        playingAudioIndex = playingAudioList.size() - 1;
                    }
                    playAudio(playingAudioIndex, playingAudioList);

                }

            }
        });

        playPayPauseTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (playingAudioList.get(playingAudioIndex).getUrl().endsWith(".opus")) {
                    opusPlayer.toggleOpusPlayer();
                } else {
                    if (isPlyingAudio) {
                        Intent broadcastIntent = new Intent(MediaControlsConstants.MEDIA_CONTROLS_ACTION_PAUSE);
                        sendBroadcast(broadcastIntent);
                    } else {
                        Intent broadcastIntent = new Intent(MediaControlsConstants.MEDIA_CONTROLS_ACTION_PLAY);
                        sendBroadcast(broadcastIntent);
                    }
                }
            }
        });

        nextTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //code  used to play next tract in service, not used after adding OpusPlayer
                // Intent broadcastIntent = new Intent(MediaControlsConstants.MEDIA_CONTROLS_ACTION_NEXT);
                // sendBroadcast(broadcastIntent);

                if (playingAudioList != null && playingAudioIndex >= 0) {
                    if (++playingAudioIndex == (playingAudioList.size())) {
                        //if last in playlist
                        playingAudioIndex = 0;
                    }
                    playAudio(playingAudioIndex, playingAudioList);

                }

            }
        });

        stopPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent broadcastIntent = new Intent(MediaControlsConstants.MEDIA_CONTROLS_ACTION_STOP);
//                sendBroadcast(broadcastIntent);
                if (playingAudioList.get(playingAudioIndex).getUrl().endsWith(".opus")) {
                    //playing opus media
                    opusPlayer.stopOpusMedia();
                } else {
                    if (serviceBound) {
                        unbindService(serviceConnection);
                        //service is active
                        player.stopSelf();
                        serviceBound = false;
                    }
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (playingAudioList.get(playingAudioIndex).getUrl().endsWith(".opus")) {
                        //playing opus media
                        opusPlayer.seekToPositionOpusMedia(progress);
                    } else {
                        seekBarUpdateIntent.putExtra("SeekBarPosition", progress);
                        // seekBarUpdateIntent.putExtra("SeekBarPosition", seekBar.getProgress());
                        sendBroadcast(seekBarUpdateIntent);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    /**
     * Broadcast Receiver to update the seekBar from {@link MediaPlayerService} audio progress
     */
    private BroadcastReceiver seekBarReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //update SeekBar position
            String mediaPosition = intent.getStringExtra("mediaPosition");
            String mediaDuration = intent.getStringExtra("mediaDuration");
            seekBar.setMax(Integer.parseInt(mediaDuration));
            seekBar.setProgress(Integer.parseInt(mediaPosition));
        }
    };

    private BroadcastReceiver OPUSMediaControlsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {
                case SEEKBAR_UPDATE_OPUS:
                    //update SeekBar position
                    String progressPosition = intent.getStringExtra("progressPosition");
                    seekBar.setProgress(Integer.parseInt(progressPosition));
                    break;
                case NEXT_OPUS:
                    MediaPlayerPlaying();
                    break;
                case PREVIOUS_OPUS:
                    MediaPlayerPlaying();
                    break;
                case PLAY_OPUS:
                    MediaPlayerPlaying();
                    break;
                case PAUSE_OPUS:
                    MediaPlayerPaused();
                    break;
                case STOP_OPUS:
                    opusPlayer.stopOpusMedia();
                    MediaPlayerStopped();
                    break;
            }

        }
    };

    private void registerOPUSMediaControlsReceiver() {
        //Register MediaServiceActionReceiver BroadcastReceivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(SEEKBAR_UPDATE_OPUS);
        filter.addAction(NEXT_OPUS);
        filter.addAction(PREVIOUS_OPUS);
        filter.addAction(PLAY_OPUS);
        filter.addAction(PAUSE_OPUS);
        filter.addAction(STOP_OPUS);
        registerReceiver(OPUSMediaControlsReceiver, filter);
    }


    @Override
    protected void onPause() {
        super.onPause();
        try {
            //unregister SeekBar Receiver
            unregisterReceiver(seekBarReceiver);
            unregisterReceiver(OPUSMediaControlsReceiver);
            unregisterReceiver(MediaServiceActionReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //register seekBarReceiver
        if (isPlyingAudio)
            registerReceiver(seekBarReceiver, new IntentFilter(MediaPlayerService.SEEKBAR_UPDATE));
        registerMediaControlsReceiver();
        registerOPUSMediaControlsReceiver();
    }


    /**
     * Broadcast Receiver to get update actions from the {@link MediaPlayerService} and update the player UI
     */
    private BroadcastReceiver MediaServiceActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {
                case MediaServiceActions.MEDIA_SERVICE_ACTION_PLAY:
                    MediaPlayerPlaying();
                    break;
                case MediaServiceActions.MEDIA_SERVICE_ACTION_PAUSE:
                    MediaPlayerPaused();
                    break;
                case MediaServiceActions.MEDIA_SERVICE_ACTION_NEXT:
                    MediaPlayerPlaying();//Playing new media
                    break;
                case MediaServiceActions.MEDIA_SERVICE_ACTION_PREVIOUS:
                    MediaPlayerPlaying();//Playing new media
                    break;
                case MediaServiceActions.MEDIA_SERVICE_ACTION_STOP:
                    MediaPlayerStopped();
                    break;
            }
        }
    };

    private void registerMediaControlsReceiver() {
        //Register MediaServiceActionReceiver BroadcastReceivers
        IntentFilter filter = new IntentFilter();
        filter.addAction(MediaServiceActions.MEDIA_SERVICE_ACTION_PLAY);
        filter.addAction(MediaServiceActions.MEDIA_SERVICE_ACTION_PAUSE);
        filter.addAction(MediaServiceActions.MEDIA_SERVICE_ACTION_STOP);
        filter.addAction(MediaServiceActions.MEDIA_SERVICE_ACTION_NEXT);
        filter.addAction(MediaServiceActions.MEDIA_SERVICE_ACTION_PREVIOUS);
        registerReceiver(MediaServiceActionReceiver, filter);
    }

    private void MediaPlayerPlaying() {
        //move the FAB up to the player
        if (uploadMenu.getPaddingBottom() == getResources().getDimensionPixelSize(R.dimen.fab_padding))
            uploadMenu.setPadding(
                    uploadMenu.getPaddingLeft(),
                    uploadMenu.getPaddingTop(),
                    uploadMenu.getPaddingRight(),
                    uploadMenu.getPaddingBottom() + playerContainerLayout.getHeight()
            );

        isPlyingAudio = true;
        playerContainerLayout.setVisibility(View.VISIBLE);
        playPayPauseTrack.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_white));
    }

    private void MediaPlayerStopped() {
        //move the FAB to its original position
        uploadMenu.setPadding(
                uploadMenu.getPaddingLeft(),
                uploadMenu.getPaddingTop(),
                uploadMenu.getPaddingRight(),
                getResources().getDimensionPixelSize(R.dimen.fab_padding)
        );

        isPlyingAudio = false;
        playerContainerLayout.setVisibility(View.GONE);
        Log.wtf("Mediacontrolls", "MediaPlayerStopped");

        killMediaPlayerService();
    }

    private void MediaPlayerPaused() {
        isPlyingAudio = false;
        playPayPauseTrack.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_white_24dp));
        Log.wtf("Mediacontrolls", "MediaPlayerPaused");
    }


    public void updateServiceReferences(MediaPlayerService player, ServiceConnection serviceConnection) {
        this.player = player;
        this.serviceConnection = serviceConnection;
    }

    private void killMediaPlayerService() {
//        if (serviceConnection != null)
//            unbindService(serviceConnection);
//        if (player != null)
//            player.stopSelf();
        Intent intent = new Intent(getApplicationContext(), MediaPlayerService.class);
        stopService(intent);

    }


    //Upload methods
    public void startRecordingVideo() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM), "Camera");
            videoFile = new File(storageDir, "Commons_" + timeStamp + ".mp4");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                videoUri = FileProvider.getUriForFile(MainActivity.this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        videoFile);
            } else {
                videoUri = Uri.fromFile(videoFile);
            }

            intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            startActivityForResult(intent, VIDEO_CAPTURE);
        } else {
            Toast.makeText(this, R.string.no_camera_on_device, Toast.LENGTH_LONG).show();
        }
    }

    //Pass the video file on the upload fragment over SDK version 24
    public File getVideoFile() {
        return videoFile;
    }

    public void loadUploadScreen(String videoAbsolutePath) {
//        File file = new File( mNextVideoAbsolutePath);
        Fragment uploadToCommonsFragment = UploadToCommonsFragment.newInstance(videoAbsolutePath, false, ContributionType.VIDEO);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.drawer_layout, uploadToCommonsFragment, "UploadToCommonsFragment");// give your fragment container id in first parameter
        transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
        transaction.commit();
    }

    private void takePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            ContentValues values = new ContentValues(1);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            Uri cameraTempUri = getContentResolver()
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

//            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//            File imagesFolder = new File(Environment
//                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CommonsImages");
//            imagesFolder.mkdirs();
//
//            File image = new File(imagesFolder, "Commons_" + timeStamp + ".jpg");
//            Uri uriSavedImage = Uri.fromFile(image);
//            capturedPhotoPath = uriSavedImage.toString();

            capturedPhotoPath = cameraTempUri.toString();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraTempUri);
            takePictureIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
            startActivityForResult(takePictureIntent, IMAGE_CAPTURE);
        }
    }


    //Audio playing methods
    public void playAudio(int audioIndex, ArrayList<Contribution> contributionsList) {
        playingAudioIndex = audioIndex;
        playingAudioList = contributionsList;

        //check if audio files are of OPUS format
        if (contributionsList.get(audioIndex).getUrl().endsWith(".opus")) {
            //stop MediaPlayerService if it is plying
            if (serviceBound) {
                unbindService(serviceConnection);
                //service is active
                player.stopSelf();
                serviceBound = false;
            }
            String duration = contributionsList.get(audioIndex).getDuration();
            int audioLength;
            if (duration.contains("."))
                audioLength = Integer.parseInt(duration.substring(0, duration.indexOf(".")));
            else
                audioLength = Integer.parseInt(duration);
            if (opusPlayer != null)
                opusPlayer.stopOpusMedia();//stop any playing media
            opusPlayer = new OpusPlayer(
                    MainActivity.this,
                    audioLength,
                    contributionsList.get(audioIndex).getUrl()
            );
            opusPlayer.playOpusMedia();
            return;
        } else {


            //if not the player will proceed to play audio files in a service
            if (opusPlayer != null)
                opusPlayer.stopOpusMedia();//stop any playing media


            StorageUtilAudioPlayer storage = new StorageUtilAudioPlayer(getApplicationContext());
            //Check is service is active
            if (!storage.getPlayerFlag() || player == null) {
                // means the service is not playing
                //Store Serializable audioList to SharedPreferences
                storage.storeAudio(contributionsList);
                storage.storeAudioIndex(audioIndex);

                Intent playerIntent = new Intent(MainActivity.this, MediaPlayerService.class);
                startService(playerIntent);
                bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
                MediaPlayerPlaying();
            } else {
                //Store the new audioIndex to SharedPreferences
                storage.storeAudioIndex(audioIndex);

                //Service is active
                //Send a broadcast to the service -> PLAY_NEW_AUDIO
                Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
                sendBroadcast(broadcastIntent);
                MediaPlayerPlaying();
            }
        }

        //Send reference to the Activity, Service and its connection
//        ((MainActivity) getActivity()).updateServiceReferences(player, serviceConnection);
    }


    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;

        }
    };

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(MediaPlayerService.NOTIFICATION_ID);
    }


    public void updateStatusBarColor() {// Color must be in hexadecimal fromat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimary));
        }
    }

    public void revertStatusBarColor() {// Color must be in hexadecimal fromat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(statusBatColor);
        }
    }

    private void getStatusBatColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            statusBatColor = window.getStatusBarColor();
        }
    }

    public void triggerContributionLoadRequest() {
        loadContributions(username, limit);
    }

    //Load user Contributions
    private void loadContributions(String username, String limit) {
        if (NetworkStatus.networkAvailable(MainActivity.this)) {
            Log.wtf("NetworkStatus", "networkAvailable");
            commons.loadContributions(username, limit, new ContributionsCallback() {
                @Override
                public void onContributionsReceived(ArrayList<Contribution> arrayList) {
                    contributions = arrayList;
                    if (arrayList != null || arrayList.size() > 0) {// at least one item available
                        //cache Contributions
                        Log.wtf("Contributions", String.valueOf(arrayList.size()));

                        storage.storeUserContributions(contributions);
                        notifyNewContributionsReceived();
                    } else {
                        //no contributions from this user
                        storage.storeUserContributions(null);
                    }
                }


                @Override
                public void onFailure() {
//                showToastMessage("Error loading contributions");
                }

            });
        } else {
            //Device offline
            contributions = storage.retrieveUserContributions();// load from cache
            notifyNewContributionsReceived();
        }
    }

    //valdio delete this
    private void loadtestContributions(String username, String limit, final boolean notify) {
        if (NetworkStatus.networkAvailable(MainActivity.this)) {
            commons.loadContributions(username, limit, new ContributionsCallback() {
                @Override
                public void onContributionsReceived(ArrayList<Contribution> arrayList) {
                    if (arrayList != null || arrayList.size() > 0) {// at least one item available
                        SharedPreferences preferences = getSharedPreferences(getString(R.string.User_Contributions), Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();

                        contributions = storage.retrieveUserContributions();
                        for (Contribution contribution : arrayList) {
                            if (contributions == null)
                                contributions = new ArrayList<Contribution>();
                            contributions.add(contribution);
                        }
                        if (contributions == null || contributions.size() == 0) {
                            //No contributions yet
                            editor.putString("Contributions", "");
                            editor.apply();
                        } else {
                            Gson gson = new Gson();
                            String json = gson.toJson(contributions);
                            //Store contributions
                            editor.putString("Contributions", json);
                            editor.apply();
                        }
                    }

                    if (notify)
                        notifyNewContributionsReceived();
                }

                @Override
                public void onFailure() {
//                showToastMessage("Error loading contributions");
                }
            });
        }
    }

    private void notifyNewContributionsReceived() {
        storage.setUserContributionsStatus(true);//set a flag that Contributions were received

        //Send a broadcast to fragments that user contributions are loaded
        Intent broadcastIntent = new Intent(CONTRIBUTIONS_LOADED_BROADCAST);
        sendBroadcast(broadcastIntent);
    }

    private void loadPictureOfTheDay() {
        if ((pictureOfTheDayList = storage.retrieveRSS_Feed(MediaType.PICTURE)) == null)//load only if POD is not up to date
            commons.getPictureOfTheDay(new RSS_FeedCallback() {
                @Override
                public void onFeedReceived(ArrayList<FeedItem> arrayList) {
                    storage.storeRSS_Feed(arrayList, MediaType.PICTURE);
                    pictureOfTheDayList = arrayList;
                    //Update Collapsing ImageView
                    if (pictureOfTheDayList != null &&
                            pictureOfTheDayList.size() > 0 &&
                            pictureOfTheDayList.get(0) != null &&
                            pictureOfTheDayList.get(0).getMediaLink() != null)
                        Glide.with(MainActivity.this)
                                .load(pictureOfTheDayList.get(0).getMediaLink())
                                .centerCrop()
                                .crossFade()
                                .into(collapsingImageView);
                }

                @Override
                public void onError() {

                }
            });
    }


    private void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle saveInstanceState) {
        saveInstanceState.putBoolean(LOAD_USER_CONTRIBUTIONS_FROM_BUNDLE, true);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(saveInstanceState);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if (requestCode == UPLOAD_CAMERA_PERMISSION) {
            String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (EasyPermissions.hasPermissions(MainActivity.this, permissions)) {
                // Already have permission
                uploadMenu.close(true);
                takePictureIntent();
            } else {
                uploadMenu.close(true);
                showToastMessage(getString(R.string.allow_app_to_capture_images));
            }

        } else if (requestCode == UPLOAD_VIDEO_PERMISSION) {
            String[] videoPermissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (EasyPermissions.hasPermissions(MainActivity.this, videoPermissions)) {
                // Already have permission
                uploadMenu.close(true);
                startRecordingVideo();
            } else {
                uploadMenu.close(true);
                showToastMessage(getString(R.string.allow_app_to_record_videos));
            }

        } else if (requestCode == UPLOAD_AUDIO_PERMISSION) {
            String[] audioPermissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (EasyPermissions.hasPermissions(MainActivity.this, audioPermissions)) {
                // Already have permission
                uploadMenu.close(true);
                Fragment audioRegisterFragment = AudioRegisterFragment.newInstance();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.drawer_layout, audioRegisterFragment, "AudioRegisterFragment");// give your fragment container id in first parameter
                transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                transaction.commit();
            } else {
                uploadMenu.close(true);
                showToastMessage(getString(R.string.allow_app_to_record_audio));
            }

        } else if (requestCode == UPLOAD_GALLERY_PERMISSION) {
            String[] galleryPermissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
            if (EasyPermissions.hasPermissions(MainActivity.this, galleryPermissions)) {
                // Already have permission
                uploadMenu.close(true);
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent,
                        "Upload to Commons"), GALLERY_REQUEST_CODE);
            } else {
                uploadMenu.close(true);
                showToastMessage(getString(R.string.allow_app_to_access_gallery));
            }
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        uploadMenu.close(true);
        if (requestCode == UPLOAD_CAMERA_PERMISSION)
            showToastMessage(getString(R.string.allow_app_to_capture_images));
        else if (requestCode == UPLOAD_VIDEO_PERMISSION)
            showToastMessage(getString(R.string.allow_app_to_record_videos));
        else if (requestCode == UPLOAD_AUDIO_PERMISSION)
            showToastMessage(getString(R.string.allow_app_to_record_audio));
        else if (requestCode == UPLOAD_GALLERY_PERMISSION)
            showToastMessage(getString(R.string.allow_app_to_access_gallery));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}

