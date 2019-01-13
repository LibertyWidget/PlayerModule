# PlayerModule

Video Player 

Personal use items if you like to download

Original version address

https://github.com/lipangit/JiaoZiVideoPlayer


WidgetModule
The current project is for a faster personal construction project, and if there is any problem,
please submit the question. If you need it, please download it.

Thank you.
How to To get a Git project into your build:

Step 1. Add the JitPack repository to your build file

gradle Add it in your root build.gradle at the end of repositories:

allprojects {

    repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
Step 2. Add the dependency

    dependencies {
            ...
            implementation 'com.github.LibertyWidget:PlayerModule:x.x.x'
    }
    
Share this release:


For the sake of simplicity, we rely on the entire ExoPlayer library.
You can also rely on only the libraries you really need.If you want to play DASH types of media resources,
 you can just rely on the Core,DASH, and UI libraries.

    implementation 'com.google.android.exoplayer:exoplayer-core:2.X.X'
    implementation 'com.google.android.exoplayer:exoplayer-dash:2.X.X'
    implementation 'com.google.android.exoplayer:exoplayer-ui:2.X.X'

more 

    1.exoplayer-core：Core functions (required)
    2.exoplayer-dash：Support for DASH content
    3.exoplayer-hls：Support for HLS content
    4.exoplayer-smoothstreaming：SmoothStreaming content supported
    5.exoplayer-ui：UI components and associated resources for ExoPlayer.


get more 
    
    https://jitpack.io/#LibertyWidget/PlayerModule

AndroidManifest.xml
           
            android:configChanges="orientation|screenSize|keyboardHidden"
            android:hardwareAccelerated="true"
            android:screenOrientation="portrait"
            
MainActivity.java

      public class MainActivity extends AppCompatActivity {
            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_main);
                UniversalPlayerView baseUniversalPlayerView = findViewById(R.id.playerView);
                String url = "Broadcast address";
                //mp4 m3u8 
                PlayManager.$().init(baseUniversalPlayerView);
                PlayManager.$().onSettingPlay(url, "title");
            }

            @Override
            public void finish() {
                PlayManager.$().finish();
                super.finish();
            }

            @Override
            public void onBackPressed() {
                PlayManager.$().onBackPressed();
                super.onBackPressed();
            }

            @Override
            protected void onResume() {
                PlayManager.$().onResume();
                super.onResume();
            }

            @Override
            protected void onPause() {
                PlayManager.$().onPause();
                super.onPause();
            }
      }
activity_main.xml

      <com.util.player.UniversalPlayerView
        android:id="@+id/playerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>
